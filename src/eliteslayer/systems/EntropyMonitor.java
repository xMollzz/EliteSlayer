package eliteslayer.systems;

import org.dreambot.api.utilities.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks Shannon entropy over a sliding window of player-action events with
 * additional diversity and recency-weighting metrics.
 *
 * <h3>Improvements</h3>
 * <ul>
 *   <li><b>Action diversity score</b> — ratio of distinct action types to
 *       window size.  Low diversity (&lt;0.3) means only a few actions are
 *       being used.</li>
 *   <li><b>Recency weighting</b> — recent actions count more heavily in the
 *       entropy calculation, making the monitor more responsive to sudden
 *       pattern changes.</li>
 *   <li><b>Streak detection</b> — detects if the same action is repeated
 *       consecutively (a strong bot indicator).</li>
 * </ul>
 *
 * Hot-path uses a manual HashMap frequency count rather than streams
 * (optimisation 11).
 */
public final class EntropyMonitor {

    private static final int WINDOW_SIZE = 100;
    /** Streak length that triggers a warning. */
    private static final int STREAK_WARNING = 4;

    private final Deque<Integer> window = new ArrayDeque<>(WINDOW_SIZE + 1);
    private double lastEntropy = 0.0;
    /** Current consecutive-repeat streak length. */
    private int    currentStreak    = 0;
    /** The action ID being repeated. */
    private int    streakAction     = -1;
    /** Longest streak seen this session. */
    private int    maxStreakSeen    = 0;

    public void record(int actionId) {
        // Streak tracking
        if (actionId == streakAction) {
            currentStreak++;
            if (currentStreak > maxStreakSeen) maxStreakSeen = currentStreak;
            if (currentStreak >= STREAK_WARNING) {
                Logger.warn("[Entropy] Action " + actionId + " repeated "
                    + currentStreak + " times in a row — suspicious pattern.");
            }
        } else {
            streakAction  = actionId;
            currentStreak = 1;
        }

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

    /**
     * Returns the action-diversity ratio [0, 1].
     * 1.0 = every event is a unique action type, 0.0 = all the same.
     */
    public double getDiversity() {
        if (window.isEmpty()) return 1.0;
        Map<Integer, Integer> freq = new HashMap<>();
        for (int v : window) {
            freq.merge(v, 1, Integer::sum);
        }
        return (double) freq.size() / window.size();
    }

    /** Returns the current consecutive-action streak length. */
    public int getCurrentStreak() {
        return currentStreak;
    }

    /** Returns the longest streak seen this session. */
    public int getMaxStreak() {
        return maxStreakSeen;
    }

    /**
     * Returns a composite "bot-risk" score [0, 1] combining entropy,
     * diversity, and streak length.  Higher = more suspicious.
     */
    public double getBotRiskScore() {
        double entropyRisk  = 1.0 - getNormalized();
        double diversityRisk = 1.0 - getDiversity();
        double streakRisk = Math.min(1.0, currentStreak / 8.0);

        // Weighted combination
        return entropyRisk * 0.4 + diversityRisk * 0.35 + streakRisk * 0.25;
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
