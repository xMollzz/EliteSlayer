package eliteslayer.systems;

import eliteslayer.Constants;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;

/**
 * Three-tier stuck watchdog with position-history tracking.
 *
 * <p><b>Position history</b> — a ring buffer records the player's tile
 * every time {@link #check()} is called.  The total movement delta across
 * the buffer is used to detect subtle "jitter-in-place" patterns that the
 * simple last-tile comparison would miss.</p>
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

    /** Size of the position-history ring buffer. */
    private static final int HISTORY_SIZE = 10;

    private Tile   lastTile       = null;
    private long   lastMoveTime   = System.currentTimeMillis();
    private int    tier           = 0;

    // Position history ring buffer
    private final Tile[] positionHistory  = new Tile[HISTORY_SIZE];
    private final long[] positionTimes    = new long[HISTORY_SIZE];
    private int          historyIndex     = 0;
    private int          historyCount     = 0;

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
        long now     = System.currentTimeMillis();

        // Record position in history ring buffer
        recordPosition(current, now);

        if (lastTile == null || current.distance(lastTile) > Constants.STUCK_MOVE_THRESHOLD) {
            lastTile     = current;
            lastMoveTime = now;
            tier         = 0;
            return;
        }

        long stuckMs = now - lastMoveTime;

        // Use position history to detect jitter-in-place patterns
        double movementDelta = computeMovementDelta();
        boolean trulyStuck = movementDelta <= Constants.STUCK_MOVE_THRESHOLD;

        if (tier == 0 && stuckMs > TIER1_MS && trulyStuck) {
            tier = 1;
            Telemetry.recordPathFail();
            Logger.warn("[StuckDetector] Tier 1: player stuck (delta=" + String.format("%.1f", movementDelta)
                + ", stationary " + (stuckMs / 1000) + "s), nudging.");
            nudgePlayer(current);
        } else if (tier == 1 && stuckMs > TIER2_MS && trulyStuck) {
            tier = 2;
            Telemetry.recordPathFail();
            Logger.warn("[StuckDetector] Tier 2: still stuck (delta=" + String.format("%.1f", movementDelta)
                + ", stationary " + (stuckMs / 1000) + "s), teleporting.");
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
        historyCount = 0;
        historyIndex = 0;
    }

    /**
     * Returns the total movement delta (tiles) across the position history
     * buffer — useful for external diagnostics / HUD display.
     */
    public double getMovementDelta() {
        return computeMovementDelta();
    }

    /**
     * Returns how long (ms) the player has been stationary at the current
     * location.
     */
    public long getStationaryTime() {
        return System.currentTimeMillis() - lastMoveTime;
    }

    // ------------------------------------------------------------------ //
    //  Position history                                                    //
    // ------------------------------------------------------------------ //

    private void recordPosition(Tile tile, long time) {
        positionHistory[historyIndex] = tile;
        positionTimes[historyIndex]   = time;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) historyCount++;
    }

    /**
     * Computes the total distance (tiles) the player has moved across all
     * consecutive entries in the history buffer.  A low value indicates the
     * player is effectively stationary even if individual ticks show tiny
     * movements.
     */
    private double computeMovementDelta() {
        if (historyCount < 2) return 0.0;

        double totalDelta = 0.0;
        for (int i = 1; i < historyCount; i++) {
            int prevIdx = (historyIndex - historyCount + i - 1 + HISTORY_SIZE * 2) % HISTORY_SIZE;
            int currIdx = (historyIndex - historyCount + i     + HISTORY_SIZE * 2) % HISTORY_SIZE;
            Tile prev = positionHistory[prevIdx];
            Tile curr = positionHistory[currIdx];
            if (prev != null && curr != null) {
                totalDelta += prev.distance(curr);
            }
        }
        return totalDelta;
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
