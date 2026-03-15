package eliteslayer.systems;

import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;

/**
 * Three-tier stuck watchdog.
 *
 * <ol>
 *   <li>Tier 1 (10 s): player hasn't moved — try walking to a random nearby tile.</li>
 *   <li>Tier 2 (30 s): still stuck — teleport to nearest bank.</li>
 *   <li>Tier 3 (60 s): still stuck — stop the script.</li>
 * </ol>
 */
public final class StuckDetector {

    private static final long TIER1_MS = 10_000L;
    private static final long TIER2_MS = 30_000L;
    private static final long TIER3_MS = 60_000L;

    private Tile   lastTile       = null;
    private long   lastMoveTime   = System.currentTimeMillis();
    private int    tier           = 0;

    private final Runnable stopScript;

    public StuckDetector(Runnable stopScript) {
        this.stopScript = stopScript;
    }

    /**
     * Call once per main loop.  Detects position stagnation and escalates
     * through the three tiers.
     */
    public void check() {
        Player local = Players.localPlayer();
        if (local == null) return;

        Tile current = local.getTile();
        if (lastTile == null || current.distance(lastTile) > 2) {
            lastTile     = current;
            lastMoveTime = System.currentTimeMillis();
            tier         = 0;
            return;
        }

        long stuckMs = System.currentTimeMillis() - lastMoveTime;

        if (tier == 0 && stuckMs > TIER1_MS) {
            tier = 1;
            Telemetry.recordPathFail();
            Logger.warn("[StuckDetector] Tier 1: player stuck, nudging.");
            nudgePlayer(current);
        } else if (tier == 1 && stuckMs > TIER2_MS) {
            tier = 2;
            Telemetry.recordPathFail();
            Logger.warn("[StuckDetector] Tier 2: still stuck, teleporting.");
            emergencyTeleport();
        } else if (tier >= 2 && stuckMs > TIER3_MS) {
            Logger.error("[StuckDetector] Tier 3: stuck for 60s, stopping script.");
            if (stopScript != null) stopScript.run();
        }
    }

    public void reset() {
        Player local = Players.localPlayer();
        if (local != null) lastTile = local.getTile();
        lastMoveTime = System.currentTimeMillis();
        tier         = 0;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private void nudgePlayer(Tile centre) {
        try {
            int dx = java.util.concurrent.ThreadLocalRandom.current().nextInt(5) - 2;
            int dy = java.util.concurrent.ThreadLocalRandom.current().nextInt(5) - 2;
            Tile nudge = new Tile(centre.getX() + dx, centre.getY() + dy, centre.getZ());
            org.dreambot.api.methods.walking.impl.Walking.walk(nudge);
        } catch (Exception e) {
            Logger.error("[StuckDetector] Nudge failed: " + e.getMessage());
        }
    }

    private void emergencyTeleport() {
        try {
            // Use a teleport tab if available
            org.dreambot.api.wrappers.items.Item tab =
                org.dreambot.api.methods.container.impl.Inventory.get("Teleport to house");
            if (tab != null) { tab.interact("Break"); return; }
            tab = org.dreambot.api.methods.container.impl.Inventory.get("Varrock teleport");
            if (tab != null) { tab.interact("Break"); }
        } catch (Exception e) {
            Logger.warn("[StuckDetector] Teleport attempt failed: " + e.getMessage());
        }
    }
}
