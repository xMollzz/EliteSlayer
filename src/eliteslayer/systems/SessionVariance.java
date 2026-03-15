package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-session variability engine.
 *
 * <p>Every time the script starts, a fresh set of "session modifiers" is
 * rolled so that no two sessions feel the same.  The three core axes are
 * energy, focus, and patience — together they alter reaction timing, error
 * rates, and break cadence throughout the session.</p>
 *
 * <p>Energy also <em>drifts</em> over time: it naturally drops as the
 * session ages, simulating a player getting tired.  The decay rate is itself
 * randomised so some sessions fade quickly while others stay energetic.</p>
 */
public final class SessionVariance {

    // ------------------------------------------------------------------ //
    //  Session-rolled constants                                            //
    // ------------------------------------------------------------------ //

    /** Initial energy level for this session (0.5–1.5). */
    private final double initialEnergy;

    /** Focus level — affects error rate and attentiveness (0.5–1.5). */
    private final double focusLevel;

    /** Patience level — affects break frequency and tolerance (0.5–1.5). */
    private final double patienceLevel;

    /** Rate at which energy decays over time (per hour, 0.02–0.15). */
    private final double energyDecayRate;

    /** Time this session started. */
    private final long sessionStartMs;

    // ------------------------------------------------------------------ //
    //  Construction                                                        //
    // ------------------------------------------------------------------ //

    public SessionVariance() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        this.initialEnergy   = 0.5 + rng.nextDouble() * 1.0;    // 0.5 – 1.5
        this.focusLevel      = 0.5 + rng.nextDouble() * 1.0;    // 0.5 – 1.5
        this.patienceLevel   = 0.5 + rng.nextDouble() * 1.0;    // 0.5 – 1.5
        this.energyDecayRate = 0.02 + rng.nextDouble() * 0.13;  // 0.02 – 0.15 per hour
        this.sessionStartMs  = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Returns the current energy level, which decays linearly from
     * {@link #initialEnergy} over time.  Never drops below 0.3.
     */
    public double getCurrentEnergy() {
        double hoursElapsed = (System.currentTimeMillis() - sessionStartMs) / 3_600_000.0;
        return Math.max(0.3, initialEnergy - hoursElapsed * energyDecayRate);
    }

    /** Returns the session's focus level (constant within a session). */
    public double getFocusLevel() {
        return focusLevel;
    }

    /** Returns the session's patience level (constant within a session). */
    public double getPatienceLevel() {
        return patienceLevel;
    }

    /**
     * Adjusts a base interval (ms) by the current session energy and
     * patience.  High energy → shorter intervals; low energy → longer.
     */
    public int adjustInterval(int baseMs) {
        double modifier = 1.0 / getCurrentEnergy();
        return Math.max(100, (int) (baseMs * modifier));
    }

    /**
     * Returns a multiplier that the break scheduler can use to alter
     * break frequency and length this session.  Higher patience → longer
     * gaps between breaks; lower patience → more frequent breaks.
     */
    public double getBreakModifier() {
        return patienceLevel;
    }
}
