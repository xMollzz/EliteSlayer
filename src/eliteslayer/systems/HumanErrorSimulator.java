package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Deliberately introduces realistic human inefficiencies.
 *
 * <p>Real players are not perfectly efficient: they sometimes hesitate before
 * acting, react late to low HP, pick a suboptimal target, or pause
 * mid-action.  This system injects those imperfections, scaled by the
 * account's {@link BehaviorProfile} and the current
 * {@link SessionVariance}.</p>
 */
public final class HumanErrorSimulator {

    /** Base chance of a mis-selection (e.g. wrong target). */
    private static final double BASE_MISCLICK_CHANCE      = 0.03;   // 3 %
    /** Base chance of a delayed reaction (late eat, slow target switch). */
    private static final double BASE_DELAY_CHANCE         = 0.12;   // 12 %
    /** Base chance of a brief hesitation before an important action. */
    private static final double BASE_HESITATION_CHANCE    = 0.08;   // 8 %

    private final BehaviorProfile profile;
    private final SessionVariance session;

    public HumanErrorSimulator(BehaviorProfile profile, SessionVariance session) {
        this.profile = profile;
        this.session = session;
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when the simulated player should "misclick" —
     * i.e. pick a suboptimal target, click an adjacent menu option, etc.
     * Probability increases when focus is low.
     */
    public boolean shouldMisclick() {
        double chance = BASE_MISCLICK_CHANCE / session.getFocusLevel();
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Returns {@code true} when the simulated player should react late —
     * e.g. eat food a tick later than optimal, delay switching prayer, etc.
     * Probability increases as energy drops throughout the session.
     */
    public boolean shouldDelayReaction() {
        double chance = BASE_DELAY_CHANCE / session.getCurrentEnergy();
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Returns the extra reaction delay (ms) when {@link #shouldDelayReaction}
     * is {@code true}.  Accounts with a higher reaction multiplier (slower
     * players) produce longer delays.
     */
    public int getReactionDelay() {
        int base = ThreadLocalRandom.current().nextInt(200, 800);
        return (int) (base * profile.getReactionMultiplier());
    }

    /**
     * Returns {@code true} when the player should briefly hesitate before
     * performing an important action (e.g. eating, switching gear).
     */
    public boolean shouldHesitate() {
        double chance = BASE_HESITATION_CHANCE / session.getFocusLevel();
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Returns a brief hesitation duration (ms) — the pause a real player
     * might take before deciding to eat or bank.
     */
    public int getHesitationDelay() {
        int base = ThreadLocalRandom.current().nextInt(300, 1200);
        return (int) (base * profile.getReactionMultiplier());
    }

    /**
     * Returns {@code true} when the simulated player should make a
     * suboptimal tactical choice — e.g. attack the second-best target
     * instead of the closest one, or delay looting a valuable item.
     */
    public boolean shouldMakeSuboptimalChoice() {
        double chance = BASE_MISCLICK_CHANCE * 1.5 / session.getFocusLevel();
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
