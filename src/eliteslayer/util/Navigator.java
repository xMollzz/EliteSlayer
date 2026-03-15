package eliteslayer.util;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;

/**
 * Centralised walking wrapper.  Every walk attempt records path failures in
 * Telemetry so the HUD can display them.
 */
public final class Navigator {

    private Navigator() {}

    /**
     * Walks to {@code tile} and waits up to {@code timeoutMs} for the player
     * to arrive within 3 tiles.
     *
     * @return true if the player arrived, false on timeout or walk failure.
     */
    public static boolean walkTo(Tile tile, long timeoutMs) {
        if (tile == null) {
            Telemetry.recordPathFail();
            return false;
        }
        if (!Walking.walk(tile)) {
            Telemetry.recordPathFail();
            return false;
        }
        boolean arrived = Sleep.sleepUntil(() -> {
            Player local = Players.localPlayer();
            return local != null && local.getTile().distance(tile) < 3;
        }, timeoutMs);
        if (!arrived) {
            Telemetry.recordPathFail();
            Logger.warn("[Navigator] Timed out walking to " + tile);
        }
        return arrived;
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
}
