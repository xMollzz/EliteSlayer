package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Poisson-process break scheduler with session-fatigue scaling.
 *
 * The longer the session runs, the more frequently (and for longer) breaks are
 * taken, mimicking natural human fatigue (optimisation 8).
 */
public final class BreakScheduler {

    // Base Poisson means (milliseconds)
    private static final long BASE_BREAK_MEAN_MS  = 45 * 60_000L;  // ~45 min between breaks
    private static final long BASE_BREAK_LEN_MEAN = 4 * 60_000L;   // ~4 min break length
    private static final long BASE_BREAK_LEN_MIN  = 90_000L;        // 1.5 min minimum

    private final long startTime;
    private long nextBreakAt;
    private long breakEndAt;
    private boolean onBreak;

    public BreakScheduler() {
        this.startTime   = System.currentTimeMillis();
        this.onBreak     = false;
        this.breakEndAt  = 0L;
        scheduleNext(0L);
    }

    /**
     * Returns true when the script should pause for a break right now.
     * Call once per main loop.
     */
    public boolean shouldBreak() {
        long now = System.currentTimeMillis();

        if (onBreak) {
            if (now >= breakEndAt) {
                onBreak = false;
                scheduleNext(now);
            }
            return onBreak;
        }

        if (now >= nextBreakAt) {
            onBreak     = true;
            breakEndAt  = now + sampleBreakLength(now);
            return true;
        }
        return false;
    }

    /** Returns the time remaining in the current break (ms), or 0. */
    public long breakTimeRemaining() {
        if (!onBreak) return 0L;
        return Math.max(0L, breakEndAt - System.currentTimeMillis());
    }

    public boolean isOnBreak() { return onBreak; }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Samples the next break time from a Poisson distribution (exponential
     * inter-arrival), scaled by session fatigue.
     */
    private void scheduleNext(long now) {
        double runtimeMinutes = (now - startTime) / 60_000.0;
        double fatigue         = Math.min(0.3, (runtimeMinutes / 120.0) * 0.3);
        double adjustedMean    = BASE_BREAK_MEAN_MS * (1.0 - fatigue);

        // Exponential inter-arrival: -mean * ln(U) where U ~ Uniform(0,1)
        double u       = ThreadLocalRandom.current().nextDouble();
        long interval  = (long) (-adjustedMean * Math.log(Math.max(u, 1e-9)));
        nextBreakAt    = now + interval;
    }

    private long sampleBreakLength(long now) {
        double runtimeMinutes = (now - startTime) / 60_000.0;
        double fatigue         = Math.min(0.3, (runtimeMinutes / 120.0) * 0.3);
        double mean            = BASE_BREAK_LEN_MEAN * (1.0 + fatigue);

        // Gaussian sample, clamped to a minimum
        double sample = mean + ThreadLocalRandom.current().nextGaussian() * (mean * 0.3);
        return Math.max(BASE_BREAK_LEN_MIN, (long) sample);
    }
}
