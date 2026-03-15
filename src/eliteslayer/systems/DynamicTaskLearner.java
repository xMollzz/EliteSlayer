package eliteslayer.systems;

import org.dreambot.api.utilities.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight session-local learning system that tracks action-outcome
 * statistics and adjusts behaviour weights at runtime.
 *
 * <h3>What it learns</h3>
 * <ul>
 *   <li><b>Combat success rate per NPC ID</b> — if a particular NPC variant
 *       frequently results in failed interactions (unreachable, misclick) its
 *       priority is lowered.</li>
 *   <li><b>Loot-value efficiency</b> — tracks average GP per loot pick-up to
 *       let the loot threshold self-tune over time.</li>
 *   <li><b>Path success rate per route</b> — routes that repeatedly timeout
 *       receive a penalty in the Navigator's tile-cost layer.</li>
 *   <li><b>Anti-ban action effectiveness</b> — actions that are always
 *       followed by a successful game interaction are weighted up;  actions
 *       after which the bot gets stuck are weighted down.</li>
 * </ul>
 *
 * <p>All data is session-local (not persisted) and is only used to make
 * within-session micro-adjustments.  There is no external I/O.</p>
 */
public final class DynamicTaskLearner {

    // ------------------------------------------------------------------ //
    //  Per-action outcome tracking                                         //
    // ------------------------------------------------------------------ //

    /** Success/fail counter for a named category. */
    private static class Outcome {
        int successes;
        int failures;
        double successRate() {
            int total = successes + failures;
            return total == 0 ? 1.0 : (double) successes / total;
        }
    }

    /** Category → NPC-ID → outcomes. */
    private final Map<Integer, Outcome> npcOutcomes  = new HashMap<>();
    /** Route key (startTile hash → endTile hash) → outcomes. */
    private final Map<Long, Outcome>    pathOutcomes = new HashMap<>();
    /** Per-anti-ban-action ID → outcomes. */
    private final Map<Integer, Outcome> antiBanOutcomes = new HashMap<>();

    /** Minimum loot samples before the threshold adjusts. */
    private static final int MIN_SAMPLES_FOR_THRESHOLD = 10;

    /** Running average GP per loot pick-up. */
    private double avgLootGp   = 0.0;
    private int    lootSamples = 0;

    // ------------------------------------------------------------------ //
    //  NPC combat learning                                                 //
    // ------------------------------------------------------------------ //

    /** Records a successful attack on the given NPC ID. */
    public void recordNpcSuccess(int npcId) {
        npcOutcomes.computeIfAbsent(npcId, k -> new Outcome()).successes++;
    }

    /** Records a failed attack attempt on the given NPC ID. */
    public void recordNpcFailure(int npcId) {
        npcOutcomes.computeIfAbsent(npcId, k -> new Outcome()).failures++;
    }

    /**
     * Returns a weight modifier [0.0, 1.0] for the given NPC ID.
     * 1.0 = fully reliable, lower = less reliable (should be deprioritized).
     */
    public double getNpcWeight(int npcId) {
        Outcome o = npcOutcomes.get(npcId);
        return o == null ? 1.0 : Math.max(0.1, o.successRate());
    }

    // ------------------------------------------------------------------ //
    //  Loot learning                                                        //
    // ------------------------------------------------------------------ //

    /** Records the GP value of a loot pick-up. */
    public void recordLootValue(long gp) {
        lootSamples++;
        avgLootGp += (gp - avgLootGp) / lootSamples;
    }

    /** Returns the running average GP per loot. */
    public double getAverageLootGp() {
        return avgLootGp;
    }

    /**
     * Returns a suggested loot-value threshold based on observed average.
     * If the average is high, raise the threshold to skip cheap drops.
     */
    public int getSuggestedLootThreshold() {
        if (lootSamples < MIN_SAMPLES_FOR_THRESHOLD) return 1_000;   // default until enough data
        return Math.max(500, (int) (avgLootGp * 0.3));
    }

    // ------------------------------------------------------------------ //
    //  Path learning                                                        //
    // ------------------------------------------------------------------ //

    /** Records a successful navigation between two tile hashes. */
    public void recordPathSuccess(long routeKey) {
        pathOutcomes.computeIfAbsent(routeKey, k -> new Outcome()).successes++;
    }

    /** Records a navigation failure (timeout / stuck). */
    public void recordPathFailure(long routeKey) {
        pathOutcomes.computeIfAbsent(routeKey, k -> new Outcome()).failures++;
    }

    /**
     * Returns a reliability weight [0.0, 1.0] for the given route.
     * Low values indicate the route frequently fails.
     */
    public double getPathWeight(long routeKey) {
        Outcome o = pathOutcomes.get(routeKey);
        return o == null ? 1.0 : Math.max(0.1, o.successRate());
    }

    // ------------------------------------------------------------------ //
    //  Anti-ban learning                                                    //
    // ------------------------------------------------------------------ //

    /** Records that the next game action after anti-ban action {@code id}
     *  was successful. */
    public void recordAntiBanFollowSuccess(int actionId) {
        antiBanOutcomes.computeIfAbsent(actionId, k -> new Outcome()).successes++;
    }

    /** Records that the next game action after anti-ban action {@code id}
     *  failed. */
    public void recordAntiBanFollowFailure(int actionId) {
        antiBanOutcomes.computeIfAbsent(actionId, k -> new Outcome()).failures++;
    }

    /**
     * Returns a weight modifier [0.0, 1.0] for the given anti-ban action.
     * Actions that correlate with subsequent failures are weighted down.
     */
    public double getAntiBanWeight(int actionId) {
        Outcome o = antiBanOutcomes.get(actionId);
        return o == null ? 1.0 : Math.max(0.2, o.successRate());
    }

    // ------------------------------------------------------------------ //
    //  Diagnostics                                                         //
    // ------------------------------------------------------------------ //

    /** Logs a summary of learned data — useful for debugging / tuning. */
    public void logSummary() {
        Logger.log("[TaskLearner] NPC outcomes: " + npcOutcomes.size() + " entries");
        Logger.log("[TaskLearner] Path outcomes: " + pathOutcomes.size() + " entries");
        Logger.log("[TaskLearner] Avg loot GP: " + String.format("%.0f", avgLootGp)
            + " (" + lootSamples + " samples)");
    }
}
