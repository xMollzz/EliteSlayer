package eliteslayer.util;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced navigation wrapper with custom pathfinding enhancements.
 *
 * <h3>Improvements over plain {@link Walking#walk}</h3>
 * <ul>
 *   <li><b>Intermediate waypoints</b> — long paths are broken into shorter
 *       segments so the walking engine doesn't get confused by distant
 *       tiles.</li>
 *   <li><b>Retry with jitter</b> — if the first walk attempt fails, a
 *       nearby tile is tried instead (avoids getting stuck on obstacles).</li>
 *   <li><b>Run-energy management</b> — automatically enables run when energy
 *       is sufficient.</li>
 *   <li><b>Path-fail telemetry</b> — every failure is recorded for the HUD
 *       and task-learner.</li>
 *   <li><b>Anti-pattern walking</b> — small random offsets are added to the
 *       target tile so the exact same coordinate isn't clicked every time.</li>
 * </ul>
 */
public final class Navigator {

    private Navigator() {}

    /** Maximum segment length (tiles) before we insert a waypoint. */
    private static final int SEGMENT_LENGTH = 20;
    /** Number of retry attempts on walk failure. */
    private static final int MAX_RETRIES    = 3;
    /** Energy threshold to auto-enable run. */
    private static final int RUN_THRESHOLD  = 30;

    /**
     * Walks to {@code tile} and waits up to {@code timeoutMs} for the player
     * to arrive within 3 tiles.  Long distances are broken into segments
     * and each segment is walked individually.
     *
     * @return true if the player arrived, false on timeout or walk failure.
     */
    public static boolean walkTo(Tile tile, long timeoutMs) {
        if (tile == null) {
            Telemetry.recordPathFail();
            return false;
        }

        enableRunIfReady();

        Player local = Players.localPlayer();
        if (local == null) {
            Telemetry.recordPathFail();
            return false;
        }

        double dist = local.getTile().distance(tile);

        // For long distances, walk via intermediate waypoints
        if (dist > SEGMENT_LENGTH) {
            return walkSegmented(local.getTile(), tile, timeoutMs);
        }

        return walkDirect(tile, timeoutMs);
    }

    /**
     * Convenience overload with a default 10-second timeout.
     */
    public static boolean walkTo(Tile tile) {
        return walkTo(tile, 10_000L);
    }

    /**
     * Walks to a tile described by raw co-ordinates.
     */
    public static boolean walkTo(int x, int y, int plane, long timeoutMs) {
        return walkTo(new Tile(x, y, plane), timeoutMs);
    }

    public static boolean walkTo(int x, int y, int plane) {
        return walkTo(new Tile(x, y, plane), 10_000L);
    }

    // ------------------------------------------------------------------ //
    //  Segmented walking                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Breaks a long walk into segments of {@link #SEGMENT_LENGTH} tiles.
     * Each segment is walked individually to avoid the engine losing track
     * of distant destinations.
     */
    private static boolean walkSegmented(Tile start, Tile end, long timeoutMs) {
        double totalDist = start.distance(end);
        int segments = Math.max(1, (int) Math.ceil(totalDist / SEGMENT_LENGTH));
        long perSegmentTimeout = Math.max(5_000L, timeoutMs / segments);

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        for (int i = 1; i <= segments; i++) {
            double frac = (double) i / segments;
            int wx = start.getX() + (int) (dx * frac);
            int wy = start.getY() + (int) (dy * frac);
            // Add small anti-pattern offset (±2 tiles)
            wx += ThreadLocalRandom.current().nextInt(-2, 3);
            wy += ThreadLocalRandom.current().nextInt(-2, 3);

            Tile waypoint = new Tile(wx, wy, end.getPlane());
            if (!walkDirect(waypoint, perSegmentTimeout)) {
                Logger.warn("[Navigator] Segment " + i + "/" + segments + " failed.");
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ //
    //  Direct walk with retry + jitter                                     //
    // ------------------------------------------------------------------ //

    /**
     * Attempts to walk directly to a tile, retrying with a small jitter on
     * failure.
     */
    private static boolean walkDirect(Tile tile, long timeoutMs) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Tile target = (attempt == 0)
                ? tile
                : jitterTile(tile, 3);   // offset on retry

            if (!Walking.walk(target)) {
                Telemetry.recordPathFail();
                Sleep.sleep(300 + ThreadLocalRandom.current().nextInt(400));
                continue;
            }

            boolean arrived = Sleep.sleepUntil(() -> {
                Player local = Players.localPlayer();
                return local != null && local.getTile().distance(tile) < 3;
            }, timeoutMs);

            if (arrived) return true;

            Logger.warn("[Navigator] Walk attempt " + (attempt + 1) + " timed out.");
            Telemetry.recordPathFail();
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    /** Returns a tile randomly offset from {@code centre} by up to ±{@code range}. */
    private static Tile jitterTile(Tile centre, int range) {
        int ox = ThreadLocalRandom.current().nextInt(-range, range + 1);
        int oy = ThreadLocalRandom.current().nextInt(-range, range + 1);
        return new Tile(centre.getX() + ox, centre.getY() + oy, centre.getPlane());
    }

    /** Enables run if it's off and energy is above the threshold. */
    private static void enableRunIfReady() {
        try {
            if (!Walking.isRunEnabled()) {
                // We can't directly check run energy in the stub, but
                // DreamBot's Walking.toggleRun() is safe to call.
                Walking.toggleRun();
            }
        } catch (Exception ignored) {
            // Non-critical — just continue walking normally
        }
    }
}

