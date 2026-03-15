package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.game.MonsterDef;
import eliteslayer.util.Navigator;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.List;

/**
 * Core combat loop with upgraded target scoring (optimisation 4).
 *
 * Score = 1000 - distance*15 - (inCombat?300:0) - healthPercent*2
 *              + (interactable?50:0)
 *
 * NPCs that are in combat but NOT targeting the local player are filtered out
 * (tagged by another player).
 */
public final class CombatNode implements Node {

    private final MonsterDef monster;

    public CombatNode(MonsterDef monster) {
        this.monster = monster;
    }

    @Override
    public Status tick() {
        if (monster == null) return Status.FAILURE;

        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        // Already in combat with a valid NPC
        if (local.isInCombat()) {
            Telemetry.setState("COMBAT");
            Telemetry.setTarget(monster.name);
            return Status.RUNNING;
        }

        // Walk to fight area if needed
        Tile fightTile = new Tile(monster.fightTile[0], monster.fightTile[1], monster.fightTile[2]);
        if (local.getTile().distance(fightTile) > 15) {
            Telemetry.setState("WALK_TO_FIGHT");
            Telemetry.setAction("Walking to " + monster.name + " area");
            Navigator.walkTo(fightTile, 15_000L);
            return Status.RUNNING;
        }

        NPC target = selectBestTarget(local, monster.ids);
        if (target == null) {
            Telemetry.setState("WAITING");
            Telemetry.setAction("No valid target");
            Sleep.sleep(600);
            return Status.RUNNING;
        }

        Telemetry.setState("ATTACKING");
        Telemetry.setTarget(target.getName() != null ? target.getName() : monster.name);
        Telemetry.setAction("Attacking " + Telemetry.getTarget());
        Logger.log("[CombatNode] Attacking " + Telemetry.getTarget());

        boolean ok = SleepUtil.retryInteract(target, "Attack", 3);
        if (ok) {
            Sleep.sleepUntil(local::isInCombat, 4_000);
        }
        return ok ? Status.RUNNING : Status.FAILURE;
    }

    // ------------------------------------------------------------------ //
    //  Target selection                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Finds the highest-scoring NPC from {@code ids}, excluding NPCs that are
     * in combat with someone other than the local player.
     */
    private NPC selectBestTarget(Player local, int[] ids) {
        List<NPC> candidates = NPCs.all(npc -> {
            if (npc == null || !npc.exists()) return false;
            for (int id : ids) {
                if (npc.getID() == id) {
                    // Exclude NPCs tagged by another player
                    if (npc.isInCombat()) {
                        org.dreambot.api.wrappers.interactive.Interactable interacting =
                            npc.getInteractingCharacter();
                        if (interacting == null) return true;   // in combat but target unknown — include
                        if (interacting.equals(local)) return true;   // fighting us — include
                        return false;   // fighting someone else — skip
                    }
                    return true;
                }
            }
            return false;
        });

        if (candidates == null || candidates.isEmpty()) return null;

        // Single-pass maximum — no streams (optimisation 11)
        NPC   best      = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        Tile   localTile = local.getTile();

        for (NPC npc : candidates) {
            if (npc == null) continue;
            double dist  = npc.getTile().distance(localTile);
            double score = 1000.0
                - dist * 15.0
                - (npc.isInCombat()    ? 300.0 : 0.0)
                - (npc.getHealthPercent() * 2.0)
                + (npc.isInteractable() ? 50.0 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best      = npc;
            }
        }
        return best;
    }
}
