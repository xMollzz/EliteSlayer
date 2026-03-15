package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Poisson-process break scheduler with session-fatigue scaling, micro-breaks,
 * and time-of-day awareness.
 *
 * <h3>Improvements</h3>
 * <ul>
 *   <li><b>Micro-breaks</b> — short 5-15 second pauses interspersed between
 *       major breaks, simulating a player briefly looking away.</li>
 *   <li><b>Time-of-day variance</b> — breaks are longer and more frequent
 *       during late-night / early-morning hours when a real player would be
 *       fatigued.</li>
 *   <li><b>Session fatigue</b> — the longer the session, the more frequent
 *       and longer breaks become (original feature, retained).</li>
 * </ul>
 */
public final class BreakScheduler {

    // Base Poisson means (milliseconds)
    private static final long BASE_BREAK_MEAN_MS  = 45 * 60_000L;  // ~45 min between breaks
    private static final long BASE_BREAK_LEN_MEAN = 4 * 60_000L;   // ~4 min break length
    private static final long BASE_BREAK_LEN_MIN  = 90_000L;        // 1.5 min minimum

    // Micro-break settings
    private static final long MICRO_BREAK_INTERVAL_MS = 8 * 60_000L; // ~8 min between micro-breaks
    private static final long MICRO_BREAK_MIN_MS      = 5_000L;       // 5 sec minimum
    private static final long MICRO_BREAK_MAX_MS      = 15_000L;      // 15 sec maximum

    private final long startTime;
    private long nextBreakAt;
    private long breakEndAt;
    private boolean onBreak;

    // Micro-break state
    private long nextMicroBreakAt;
    private long microBreakEndAt;
    private boolean onMicroBreak;

    public BreakScheduler() {
        this.startTime    = System.currentTimeMillis();
        this.onBreak      = false;
        this.onMicroBreak = false;
        this.breakEndAt   = 0L;
        this.microBreakEndAt = 0L;
        scheduleNext(0L);
        scheduleMicroBreak(0L);
    }

    /**
     * Returns true when the script should pause for a break right now.
     * Handles both major breaks and micro-breaks.
     * Call once per main loop.
     */
    public boolean shouldBreak() {
        long now = System.currentTimeMillis();

        // Major break takes priority
        if (onBreak) {
            if (now >= breakEndAt) {
                onBreak = false;
                scheduleNext(now);
            }
            return onBreak;
        }

        // Micro-break
        if (onMicroBreak) {
            if (now >= microBreakEndAt) {
                onMicroBreak = false;
                scheduleMicroBreak(now);
            }
            return onMicroBreak;
        }

        // Check if it's time for a major break
        if (now >= nextBreakAt) {
            onBreak     = true;
            breakEndAt  = now + sampleBreakLength(now);
            return true;
        }

        // Check if it's time for a micro-break
        if (now >= nextMicroBreakAt) {
            onMicroBreak    = true;
            long microLen   = MICRO_BREAK_MIN_MS
                + ThreadLocalRandom.current().nextLong(MICRO_BREAK_MAX_MS - MICRO_BREAK_MIN_MS);
            // Apply time-of-day fatigue to micro-breaks too
            microLen = (long) (microLen * getTimeOfDayMultiplier());
            microBreakEndAt = now + microLen;
            return true;
        }

        return false;
    }

    /** Returns the time remaining in the current break (ms), or 0. */
    public long breakTimeRemaining() {
        if (onBreak) return Math.max(0L, breakEndAt - System.currentTimeMillis());
        if (onMicroBreak) return Math.max(0L, microBreakEndAt - System.currentTimeMillis());
        return 0L;
    }

    public boolean isOnBreak() { return onBreak || onMicroBreak; }

    /** Returns true if the current break is a micro-break. */
    public boolean isMicroBreak() { return onMicroBreak; }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Samples the next major break time from a Poisson distribution (exponential
     * inter-arrival), scaled by session fatigue and time-of-day.
     */
    private void scheduleNext(long now) {
        double fatigue        = computeFatigue(now);
        double todMultiplier  = getTimeOfDayMultiplier();
        double adjustedMean   = BASE_BREAK_MEAN_MS * (1.0 - fatigue) / todMultiplier;

        // Exponential inter-arrival: -mean * ln(U) where U ~ Uniform(0,1)
        double u       = ThreadLocalRandom.current().nextDouble();
        long interval  = (long) (-adjustedMean * Math.log(Math.max(u, 1e-9)));
        nextBreakAt    = (now == 0 ? System.currentTimeMillis() : now) + interval;
    }

    /** Schedules the next micro-break. */
    private void scheduleMicroBreak(long now) {
        double u = ThreadLocalRandom.current().nextDouble();
        long interval = (long) (-MICRO_BREAK_INTERVAL_MS * Math.log(Math.max(u, 1e-9)));
        nextMicroBreakAt = (now == 0 ? System.currentTimeMillis() : now) + interval;
    }

    private long sampleBreakLength(long now) {
        double fatigue        = computeFatigue(now);
        double todMultiplier  = getTimeOfDayMultiplier();
        double mean           = BASE_BREAK_LEN_MEAN * (1.0 + fatigue) * todMultiplier;

        // Gaussian sample, clamped to a minimum
        double sample = mean + ThreadLocalRandom.current().nextGaussian() * (mean * 0.3);
        return Math.max(BASE_BREAK_LEN_MIN, (long) sample);
    }

    /** Session fatigue factor: [0.0, 0.3] based on runtime. */
    private double computeFatigue(long now) {
        double runtimeMinutes = ((now == 0 ? System.currentTimeMillis() : now) - startTime) / 60_000.0;
        return Math.min(0.3, (runtimeMinutes / 120.0) * 0.3);
    }

    /**
     * Returns a time-of-day multiplier.  Late night (00:00-06:00) returns
     * a higher value (longer / more frequent breaks), daytime returns 1.0.
     */
    private static double getTimeOfDayMultiplier() {
        int hour = (int) ((System.currentTimeMillis() / 3_600_000L) % 24);
        // 00:00 - 05:59 → tired player, more breaks
        if (hour >= 0 && hour < 6) return 1.4;
        // 22:00 - 23:59 → getting tired
        if (hour >= 22) return 1.2;
        return 1.0;
    }
}

