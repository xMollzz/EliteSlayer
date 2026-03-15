package eliteslayer.systems;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-account behavioral diversity — deterministic personality seeded from the
 * player name.  Every account develops a unique "fingerprint" of reaction
 * times, camera habits, AFK tendencies, and anti-ban weight skews, so that no
 * two accounts behave identically even when running the same script.
 *
 * <p>Traits are stable across sessions for the same account (seeded by name)
 * but differ between accounts, mirroring how real humans have consistent
 * individual habits.</p>
 */
public final class BehaviorProfile {

    // ------------------------------------------------------------------ //
    //  Per-account traits (deterministic from name)                        //
    // ------------------------------------------------------------------ //

    /** Multiplier applied to all reaction/delay timings (0.7–1.4). */
    private final double reactionMultiplier;

    /** How likely the account is to AFK or pause (0.3–1.0). */
    private final double afkTendency;

    /** How frequently the account moves the camera (0.4–1.0). */
    private final double cameraActivity;

    /** How quickly the account clicks through menus (0.6–1.3). */
    private final double clickSpeed;

    /** Per-action weight modifiers for the anti-ban engine (±40 %). */
    private final double[] weightModifiers;

    /** Preferred camera pitch bias — some players look down, others look up. */
    private final int preferredPitchBias;

    /** Maximum number of anti-ban actions (must match AntiBanEngine). */
    private static final int ACTION_COUNT = 17;

    // ------------------------------------------------------------------ //
    //  Construction                                                        //
    // ------------------------------------------------------------------ //

    /**
     * Builds a deterministic profile from the player's name.  The same name
     * always produces the same traits.
     */
    public BehaviorProfile(String playerName) {
        long seed = playerName == null ? 0L : playerName.hashCode() * 31L + 7;
        Random rng = new Random(seed);

        this.reactionMultiplier = 0.7 + rng.nextDouble() * 0.7;   // 0.7 – 1.4
        this.afkTendency        = 0.3 + rng.nextDouble() * 0.7;   // 0.3 – 1.0
        this.cameraActivity     = 0.4 + rng.nextDouble() * 0.6;   // 0.4 – 1.0
        this.clickSpeed         = 0.6 + rng.nextDouble() * 0.7;   // 0.6 – 1.3
        this.preferredPitchBias = 30 + rng.nextInt(50);            // 30 – 79

        this.weightModifiers = new double[ACTION_COUNT];
        for (int i = 0; i < ACTION_COUNT; i++) {
            // Each action gets a unique ±40 % modifier per account
            weightModifiers[i] = 0.6 + rng.nextDouble() * 0.8;    // 0.6 – 1.4
        }
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Adjusts a base delay (ms) by the account's reaction multiplier,
     * then adds a small random jitter so the result is never perfectly
     * predictable.
     */
    public int adjustDelay(int baseDelayMs) {
        double adjusted = baseDelayMs * reactionMultiplier;
        int jitter = ThreadLocalRandom.current().nextInt(
                (int) Math.max(1, adjusted * 0.1));
        return (int) adjusted + jitter;
    }

    /**
     * Returns modified anti-ban weights for this account.  Each weight is
     * scaled by the per-action modifier so that different accounts favour
     * different anti-ban behaviours.
     */
    public int[] getModifiedWeights(int[] baseWeights) {
        int[] modified = new int[baseWeights.length];
        for (int i = 0; i < baseWeights.length; i++) {
            double mod = i < weightModifiers.length ? weightModifiers[i] : 1.0;
            modified[i] = Math.max(1, (int) (baseWeights[i] * mod));
        }
        return modified;
    }

    public double getReactionMultiplier() { return reactionMultiplier; }
    public double getAfkTendency()        { return afkTendency; }
    public double getCameraActivity()     { return cameraActivity; }
    public double getClickSpeed()         { return clickSpeed; }
    public int    getPreferredPitchBias() { return preferredPitchBias; }
}
