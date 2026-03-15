package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Account behaviour profiling — each bot session is assigned a random
 * personality that adjusts gameplay parameters so accounts don't all
 * behave identically.
 *
 * <p>Profiles influence: reaction speed, loot priority, break frequency,
 * camera movement frequency, and idle tendency.</p>
 */
public enum PlayerProfile {

    /**
     * Casual player — slow reactions, frequent camera movement, longer idles.
     */
    CASUAL(
        /* reactionMultiplier */ 1.4,
        /* lootValueThreshold */ 500,
        /* breakFreqMultiplier */ 1.3,
        /* cameraMoveWeight */   1.5,
        /* idleChanceMultiplier */ 1.6
    ),

    /**
     * Efficient player — fast reactions, less idle time, quick looting.
     */
    EFFICIENT(
        /* reactionMultiplier */ 0.8,
        /* lootValueThreshold */ 2_000,
        /* breakFreqMultiplier */ 0.8,
        /* cameraMoveWeight */   0.7,
        /* idleChanceMultiplier */ 0.6
    ),

    /**
     * AFK-style player — very slow reactions, long idle periods, infrequent looting.
     */
    AFK(
        /* reactionMultiplier */ 1.8,
        /* lootValueThreshold */ 3_000,
        /* breakFreqMultiplier */ 1.5,
        /* cameraMoveWeight */   0.5,
        /* idleChanceMultiplier */ 2.5
    ),

    /**
     * Loot goblin — fast reactions, loots everything, rarely idles.
     */
    LOOTER(
        /* reactionMultiplier */ 1.0,
        /* lootValueThreshold */ 100,
        /* breakFreqMultiplier */ 1.0,
        /* cameraMoveWeight */   1.0,
        /* idleChanceMultiplier */ 0.8
    );

    /** Multiplier applied to base reaction/sleep delays. */
    public final double reactionMultiplier;

    /** Minimum item value (gp) the profile considers worth looting. */
    public final int lootValueThreshold;

    /** Multiplier for break frequency (>1 = more breaks). */
    public final double breakFreqMultiplier;

    /** Weight multiplier for camera-movement antiban actions. */
    public final double cameraMoveWeight;

    /** Multiplier for random idle chance (>1 = idles more). */
    public final double idleChanceMultiplier;

    PlayerProfile(double reactionMultiplier, int lootValueThreshold,
                  double breakFreqMultiplier, double cameraMoveWeight,
                  double idleChanceMultiplier) {
        this.reactionMultiplier   = reactionMultiplier;
        this.lootValueThreshold   = lootValueThreshold;
        this.breakFreqMultiplier  = breakFreqMultiplier;
        this.cameraMoveWeight     = cameraMoveWeight;
        this.idleChanceMultiplier = idleChanceMultiplier;
    }

    /**
     * Randomly selects a profile for this session, giving each account
     * a distinct play style.
     */
    public static PlayerProfile randomForSession() {
        PlayerProfile[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
