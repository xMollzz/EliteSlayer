package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.game.MonsterDef;
import eliteslayer.util.Navigator;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

/**
 * Cannon lifecycle manager: places the cannon, reloads it, and picks it up
 * when the task ends.
 */
public final class CannonNode implements Node {

    private static final int  CANNON_BASE_ID = 6;    // Dwarf cannon base (OSRS item ID: 6 - confirmed)
    private static final int  CANNONBALL_ID  = 2;    // Cannonball (OSRS item ID: 2 - confirmed)
    private static final String CANNON_NAME  = "Dwarf multicannon";   // placed cannon game-object name

    private final MonsterDef monster;
    private final boolean    enabled;
    private boolean          placed         = false;
    private long             lastReloadTime = 0L;
    /** Only reload once every 20 s to avoid spam-clicking the cannon. */
    private static final long RELOAD_COOLDOWN_MS = 20_000L;

    public CannonNode(boolean enabled, MonsterDef monster) {
        this.enabled = enabled;
        this.monster = monster;
    }

    @Override
    public Status tick() {
        if (!enabled || monster == null || !monster.usesCannon) return Status.FAILURE;

        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        Tile fightTile = new Tile(monster.fightTile[0], monster.fightTile[1], monster.fightTile[2]);

        if (!placed) {
            return placeCannon(local, fightTile);
        }

        return reloadIfNeeded();
    }

    /** Call this when the script ends to pick up the cannon. */
    public void pickUp() {
        if (!placed) return;
        GameObject cannon = GameObjects.closest(o -> o != null &&
            CANNON_NAME.equals(o.getName()));
        if (cannon != null) {
            SleepUtil.retryInteract(cannon, "Pick up", 3);
            placed = false;
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private Status placeCannon(Player local, Tile fightTile) {
        // Must have the cannon base in inventory
        if (!hasCannon()) {
            Logger.warn("[CannonNode] No cannon pieces in inventory.");
            return Status.FAILURE;
        }

        if (local.getTile().distance(fightTile) > 3) {
            Telemetry.setState("WALK_TO_CANNON");
            Navigator.walkTo(fightTile, 15_000L);
            return Status.RUNNING;
        }

        Telemetry.setState("PLACE_CANNON");
        Telemetry.setAction("Placing cannon");
        Item base = Inventory.get(CANNON_BASE_ID);
        if (base == null) return Status.FAILURE;

        boolean ok = SleepUtil.retryInteract(base, "Set-up", 3);
        if (ok) {
            Sleep.sleepUntil(
                () -> GameObjects.closest(o -> o != null && CANNON_NAME.equals(o.getName())) != null,
                6_000);
            placed = GameObjects.closest(o -> o != null && CANNON_NAME.equals(o.getName())) != null;
        }
        return placed ? Status.SUCCESS : Status.FAILURE;
    }

    private Status reloadIfNeeded() {
        GameObject cannon = GameObjects.closest(o -> o != null && CANNON_NAME.equals(o.getName()));
        if (cannon == null) { placed = false; return Status.FAILURE; }

        // Enforce reload cooldown to avoid spam-clicking
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < RELOAD_COOLDOWN_MS) return Status.FAILURE;

        // Reload when we have cannonballs in inventory
        if (Inventory.contains(CANNONBALL_ID)) {
            Telemetry.setState("RELOAD_CANNON");
            Telemetry.setAction("Reloading cannon");
            boolean ok = SleepUtil.retryInteract(cannon, "Fire", 3);
            if (ok) {
                lastReloadTime = now;
                Sleep.sleep(400, 800);
            }
            return ok ? Status.SUCCESS : Status.FAILURE;
        }
        return Status.FAILURE;
    }

    private boolean hasCannon() {
        return Inventory.contains(CANNON_BASE_ID);
    }
}
