package eliteslayer.systems;

import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates human-like reaction times using a Gaussian distribution with
 * context-aware adjustments.  Accounts for fatigue (longer sessions produce
 * slower reactions), combat urgency, and idle-to-active transitions where a
 * real player needs a moment to "wake up."
 *
 * <p>All delays are applied via {@link Sleep#sleep(long)} so they integrate
 * naturally with the DreamBot event loop.</p>
 */
public final class HumanReactionEngine {

    // ------------------------------------------------------------------ //
    //  Tunables                                                            //
    // ------------------------------------------------------------------ //

    /** Base mean reaction time in milliseconds (typical human: ~250 ms). */
    private static final double BASE_MEAN_MS     = 220.0;
    /** Standard deviation around the mean. */
    private static final double BASE_STDDEV_MS   = 60.0;
    /** Absolute minimum reaction (floor) — no human reacts in &lt;80 ms. */
    private static final long   FLOOR_MS         = 80L;
    /** Absolute maximum reaction (cap) — keeps the script responsive. */
    private static final long   CEILING_MS       = 1_200L;
    /** Extra latency added after idle periods (ms), simulating re-focus. */
    private static final double IDLE_REENTRY_MS  = 350.0;
    /** Combat urgency factor — multiplier &lt;1 speeds up reactions. */
    private static final double COMBAT_FACTOR    = 0.75;
    /** Maximum fatigue multiplier (after ~3 hours). */
    private static final double MAX_FATIGUE_MULT = 1.45;
    /** Session length (ms) at which fatigue reaches its maximum. */
    private static final long   FATIGUE_CEILING  = 3 * 3_600_000L;

    // ------------------------------------------------------------------ //
    //  State                                                               //
    // ------------------------------------------------------------------ //

    private final long sessionStart;
    /** Timestamp of the last action (used to detect idle-to-active). */
    private long lastActionTime;
    /** Threshold (ms) of inactivity before we apply re-entry delay. */
    private static final long IDLE_THRESHOLD_MS = 8_000L;

    public HumanReactionEngine() {
        this.sessionStart   = System.currentTimeMillis();
        this.lastActionTime = this.sessionStart;
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Sleeps for a human-like reaction delay appropriate to a normal action
     * (looting, banking, clicking buttons).
     */
    public void reactNormal() {
        sleep(sample(1.0));
    }

    /**
     * Sleeps for a shorter, combat-urgency reaction delay (attacking a new
     * target, eating food under pressure).
     */
    public void reactCombat() {
        sleep(sample(COMBAT_FACTOR));
    }

    /**
     * Sleeps for a longer delay appropriate to transitioning from an idle
     * state to an active one (e.g. break ending, respawn waiting).
     */
    public void reactWakeUp() {
        long base = sample(1.0);
        long extra = (long) (IDLE_REENTRY_MS + ThreadLocalRandom.current().nextGaussian() * 80.0);
        sleep(base + Math.max(0, extra));
    }

    /**
     * Returns the raw sample value (ms) for a normal reaction without
     * sleeping — useful for callers that need to combine the delay with other
     * logic.
     */
    public long sampleNormal() {
        return sample(1.0);
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Draws a single reaction-time sample with the given urgency multiplier.
     * Applies session-fatigue scaling and idle-re-entry adjustment.
     */
    private long sample(double urgency) {
        double fatigue = computeFatigue();

        double mean   = BASE_MEAN_MS * fatigue * urgency;
        double stddev = BASE_STDDEV_MS * fatigue;

        double raw = mean + ThreadLocalRandom.current().nextGaussian() * stddev;

        // Idle-re-entry bump
        long now = System.currentTimeMillis();
        if (now - lastActionTime > IDLE_THRESHOLD_MS) {
            raw += IDLE_REENTRY_MS * (0.5 + ThreadLocalRandom.current().nextDouble() * 0.5);
        }

        long clamped = Math.max(FLOOR_MS, Math.min(CEILING_MS, (long) raw));
        lastActionTime = now;
        return clamped;
    }

    /**
     * Returns a fatigue multiplier in {@code [1.0, MAX_FATIGUE_MULT]} that
     * increases linearly with session runtime.
     */
    private double computeFatigue() {
        long elapsed = System.currentTimeMillis() - sessionStart;
        double ratio = Math.min(1.0, (double) elapsed / FATIGUE_CEILING);
        return 1.0 + ratio * (MAX_FATIGUE_MULT - 1.0);
    }

    private static void sleep(long ms) {
        if (ms > 0) Sleep.sleep((int) ms);
    }
}
