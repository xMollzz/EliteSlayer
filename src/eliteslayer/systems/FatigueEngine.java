package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fatigue simulation engine that gradually degrades bot performance during
 * long sessions, mimicking how real players slow down over time.
 *
 * Fatigue is a value from 0.0 (fresh session) that grows linearly with
 * session duration.  At 1.0 the player has been active for roughly one hour.
 *
 * Other systems query this engine to modify:
 *   - reaction time (slower)
 *   - mouse speed  (slower)
 *   - misclick chance (higher)
 *   - idle time    (more frequent / longer)
 */
public final class FatigueEngine {

    private final long sessionStart;

    public FatigueEngine() {
        this.sessionStart = System.currentTimeMillis();
    }

    /**
     * Returns the raw fatigue level.
     * 0.0 at session start, 1.0 after ~1 hour, 3.0 after ~3 hours, etc.
     */
    public double getFatigue() {
        long runtime = System.currentTimeMillis() - sessionStart;
        return runtime / 3_600_000.0;
    }

    /**
     * Returns a capped fatigue value between 0.0 and {@code cap}.
     * Useful for preventing extreme degradation after very long sessions.
     */
    public double getCapped(double cap) {
        return Math.min(getFatigue(), cap);
    }

    /**
     * Returns a fatigue-adjusted delay.  The base delay is extended by
     * {@code fatigue * scaleFactor} milliseconds.
     *
     * @param baseMin     minimum base delay (ms)
     * @param baseMax     maximum base delay (ms)
     * @param scaleFactor how many additional ms per unit of fatigue
     * @return the adjusted delay (ms)
     */
    public int adjustedDelay(int baseMin, int baseMax, int scaleFactor) {
        int base = ThreadLocalRandom.current().nextInt(baseMin, baseMax + 1);
        return base + (int) (getCapped(5.0) * scaleFactor);
    }

    /**
     * Returns true with a probability that increases with fatigue, simulating
     * misclicks or attention lapses.
     *
     * @param baseChance  chance at fatigue 0 (e.g. 0.01 = 1 %)
     * @param fatigueRate additional chance per unit of fatigue (e.g. 0.02 = 2 %/hr)
     */
    public boolean shouldMisclick(double baseChance, double fatigueRate) {
        double chance = baseChance + getCapped(5.0) * fatigueRate;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Returns the session runtime in milliseconds.
     */
    public long getSessionRuntime() {
        return System.currentTimeMillis() - sessionStart;
    }
}
