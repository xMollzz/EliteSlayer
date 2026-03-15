package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.game.MonsterDef;
import eliteslayer.systems.DynamicTaskLearner;
import eliteslayer.systems.HumanReactionEngine;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core combat loop with advanced NPC target scoring and dynamic learning
 * integration.
 *
 * <h3>Scoring formula</h3>
 * <pre>
 * Score = 1000
 *       - distance × 15
 *       - (inCombat ? 300 : 0)
 *       - healthPercent × 2
 *       + (interactable ? 50 : 0)
 *       + (idleAnimation ? 30 : 0)    ← NEW: idle NPCs are easier targets
 *       - (nearOtherPlayers × 25)     ← NEW: avoid crowded NPC areas
 *       × npcReliability              ← NEW: learned success-rate modifier
 *       + randomJitter                ← NEW: prevents perfectly deterministic picks
 * </pre>
 *
 * <h3>Other improvements</h3>
 * <ul>
 *   <li><b>Re-aggro penalty</b> — recently-attacked NPCs that we failed to
 *       kill are penalized to avoid repeatedly mis-targeting the same NPC.</li>
 *   <li><b>Human reaction delay</b> — a short delay is inserted before
 *       attacking to simulate reaction time.</li>
 *   <li><b>Learning integration</b> — records successful/failed attacks per
 *       NPC ID to the {@link DynamicTaskLearner}.</li>
 * </ul>
 */
public final class CombatNode implements Node {

    private final MonsterDef monster;
    private final HumanReactionEngine reactions;
    private final DynamicTaskLearner  learner;

    /** NPC we most recently issued an Attack command to. */
    private NPC  currentTarget  = null;
    /** True if local player was in combat on the previous tick. */
    private boolean wasInCombat = false;
    /** NPC ID of the last failed attack (for re-aggro penalty). */
    private int  lastFailedNpcId = -1;
    /** Timestamp of the last failed attack. */
    private long lastFailedTime  = 0L;
    /** Re-aggro penalty window (ms). */
    private static final long REAGGRO_PENALTY_MS = 30_000L;
    /** OSRS idle animation ID (no animation playing). */
    private static final int IDLE_ANIMATION_ID = -1;

    public CombatNode(MonsterDef monster,
                      HumanReactionEngine reactions,
                      DynamicTaskLearner learner) {
        this.monster   = monster;
        this.reactions = reactions;
        this.learner   = learner;
    }

    @Override
    public Status tick() {
        if (monster == null) return Status.FAILURE;

        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        boolean inCombat = local.isInCombat();

        // Kill detection: we were fighting, now we're not, and the NPC vanished
        if (wasInCombat && !inCombat && currentTarget != null && !currentTarget.exists()) {
            Telemetry.addKill();
            if (learner != null) learner.recordNpcSuccess(currentTarget.getID());
            Logger.log("[CombatNode] Kill registered. Total: " + Telemetry.getKillCount());
            currentTarget = null;
        }
        wasInCombat = inCombat;

        // Already in combat — just wait
        if (inCombat) {
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

        // Human reaction delay before attacking
        if (reactions != null) reactions.reactCombat();

        boolean ok = SleepUtil.retryInteract(target, "Attack", 3);
        if (ok) {
            currentTarget = target;
            Sleep.sleepUntil(local::isInCombat, 4_000);
        } else {
            // Record failure for learning and re-aggro penalty
            lastFailedNpcId = target.getID();
            lastFailedTime  = System.currentTimeMillis();
            if (learner != null) learner.recordNpcFailure(target.getID());
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

        // Count nearby players for crowd-penalty calculation
        List<Player> nearbyPlayers = Players.all();
        Tile localTile = local.getTile();

        // Single-pass maximum — no streams (optimisation 11)
        NPC    best      = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (NPC npc : candidates) {
            if (npc == null) continue;

            double dist  = npc.getTile().distance(localTile);

            // Base score
            double score = 1000.0
                - dist * 15.0
                - (npc.isInCombat()     ? 300.0 : 0.0)
                - (npc.getHealthPercent() * 2.0)
                + (npc.isInteractable() ? 50.0  : 0.0);

            // NEW: Idle animation bonus — idle NPCs are easier to engage
            if (npc.getAnimation() == IDLE_ANIMATION_ID) {
                score += 30.0;
            }

            // NEW: Crowd penalty — NPCs near other players are less desirable
            int playersNearNpc = countPlayersNear(npc.getTile(), nearbyPlayers, local, 4);
            score -= playersNearNpc * 25.0;

            // NEW: Re-aggro penalty — avoid NPCs we recently failed on
            if (npc.getID() == lastFailedNpcId
                    && System.currentTimeMillis() - lastFailedTime < REAGGRO_PENALTY_MS) {
                score -= 150.0;
            }

            // NEW: Learned reliability modifier
            if (learner != null) {
                score *= learner.getNpcWeight(npc.getID());
            }

            // NEW: Small random jitter to prevent deterministic picks
            score += ThreadLocalRandom.current().nextDouble(-15.0, 15.0);

            if (score > bestScore) {
                bestScore = score;
                best      = npc;
            }
        }
        return best;
    }

    /**
     * Counts the number of other players within {@code radius} tiles of a
     * position — used for the crowd penalty.
     */
    private static int countPlayersNear(Tile centre, List<Player> players,
                                        Player exclude, int radius) {
        int count = 0;
        if (players == null) return 0;
        for (Player p : players) {
            if (p == null || p.equals(exclude)) continue;
            if (p.getTile().distance(centre) <= radius) count++;
        }
        return count;
    }
}
