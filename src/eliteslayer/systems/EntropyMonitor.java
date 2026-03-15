package eliteslayer.systems;

import org.dreambot.api.utilities.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks Shannon entropy over a sliding window of player-action events.
 * High entropy → bot-like randomness; low entropy → repetitive patterns.
 *
 * Hot-path uses a manual HashMap frequency count rather than streams
 * (optimisation 11).
 */
public final class EntropyMonitor {

    private static final int WINDOW_SIZE = 100;

    private final Deque<Integer> window = new ArrayDeque<>(WINDOW_SIZE + 1);
    private double lastEntropy = 0.0;

    public void record(int actionId) {
        window.addLast(actionId);
        if (window.size() > WINDOW_SIZE) window.pollFirst();
        lastEntropy = computeEntropy();
    }

    /** Returns the most-recently computed Shannon entropy value (0 – ~6.6). */
    public double getEntropy() {
        return lastEntropy;
    }

    /**
     * Returns the entropy as a normalised fraction [0, 1] relative to the
     * theoretical maximum for the current window size.
     */
    public double getNormalized() {
        int sz = window.size();
        if (sz <= 1) return 1.0;
        double maxEntropy = Math.log(sz) / Math.log(2);
        if (maxEntropy == 0) return 1.0;
        return Math.min(1.0, lastEntropy / maxEntropy);
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    private double computeEntropy() {
        int total = window.size();
        if (total == 0) return 0.0;

        // Manual frequency count — no streams
        Map<Integer, Integer> freq = new HashMap<>();
        for (int v : window) {
            Integer cnt = freq.get(v);
            freq.put(v, cnt == null ? 1 : cnt + 1);
        }

        double entropy = 0.0;
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            double p = (double) entry.getValue() / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
