package eliteslayer.systems;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 3-minute rolling player-density heatmap sampled periodically.
 * Hot-path uses a for-loop rather than streams (optimisation 11).
 */
public final class CrowdTracker {

    private static final long WINDOW_MS      = 3 * 60_000L;
    private static final int  CROWD_THRESHOLD = 5;

    /** Timestamped player-count samples. */
    private static class Sample {
        final long timestamp;
        final int  count;
        Sample(long ts, int c) { this.timestamp = ts; this.count = c; }
    }

    private final Deque<Sample> samples = new ArrayDeque<>();
    private long lastSample = 0L;

    /**
     * Takes a new sample if at least 5 seconds have elapsed since the last one.
     */
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastSample < 5_000L) return;
        lastSample = now;

        // Evict old samples
        while (!samples.isEmpty() && now - samples.peekFirst().timestamp > WINDOW_MS) {
            samples.pollFirst();
        }

        Player local = Players.localPlayer();
        if (local == null) return;

        Tile centre = local.getTile();
        List<Player> nearby = Players.all();
        int count = 0;
        for (Player p : nearby) {
            if (p != null && !p.equals(local) && p.getTile().distance(centre) <= 10) {
                count++;
            }
        }
        samples.addLast(new Sample(now, count));
    }

    /** Returns the average player count over the rolling 3-minute window. */
    public double getAverageCrowd() {
        if (samples.isEmpty()) return 0.0;
        long total = 0;
        int  sz    = 0;
        for (Sample s : samples) {
            total += s.count;
            sz++;
        }
        return sz == 0 ? 0.0 : (double) total / sz;
    }

    /** Returns the most recent raw player count near the local player. */
    public int getCurrentCrowd() {
        if (samples.isEmpty()) return 0;
        return samples.peekLast().count;
    }

    /** Returns true if the current crowd exceeds the safety threshold. */
    public boolean isCrowded() {
        return getCurrentCrowd() >= CROWD_THRESHOLD;
    }
}
