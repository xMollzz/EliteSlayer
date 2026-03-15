package eliteslayer.systems;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Activity fingerprint randomization for combat behaviour.
 *
 * <p>Instead of always following the same action order (attack → loot → eat → repeat),
 * each session picks a different strategy that alters priorities and ordering.
 * This prevents detection systems from clustering accounts with identical
 * behaviour patterns.</p>
 */
public enum CombatStrategy {

    /**
     * Kill as fast as possible — prioritise attacking over looting.
     */
    AGGRESSIVE(
        /* lootBetweenKills */ false,
        /* eatDuringCombat  */ true,
        /* preferClosest    */ false,
        /* postKillDelay    */ 100,
        /* preLootDelay     */ 200
    ),

    /**
     * Safe play — eat early, loot frequently, prefer closer targets.
     */
    SAFE_SPOT(
        /* lootBetweenKills */ true,
        /* eatDuringCombat  */ true,
        /* preferClosest    */ true,
        /* postKillDelay    */ 400,
        /* preLootDelay     */ 150
    ),

    /**
     * Loot-focused — always check ground items, less aggressive combat pacing.
     */
    LOOT_FOCUS(
        /* lootBetweenKills */ true,
        /* eatDuringCombat  */ false,
        /* preferClosest    */ true,
        /* postKillDelay    */ 600,
        /* preLootDelay     */ 100
    );

    /** Whether the strategy tries to loot between every kill. */
    public final boolean lootBetweenKills;

    /** Whether the strategy allows eating while already in combat. */
    public final boolean eatDuringCombat;

    /** Whether the strategy prefers the closest target over the best score. */
    public final boolean preferClosest;

    /** Base delay (ms) after a kill before the next action. */
    public final int postKillDelay;

    /** Base delay (ms) before attempting to pick up loot. */
    public final int preLootDelay;

    CombatStrategy(boolean lootBetweenKills, boolean eatDuringCombat,
                   boolean preferClosest, int postKillDelay, int preLootDelay) {
        this.lootBetweenKills = lootBetweenKills;
        this.eatDuringCombat  = eatDuringCombat;
        this.preferClosest    = preferClosest;
        this.postKillDelay    = postKillDelay;
        this.preLootDelay     = preLootDelay;
    }

    /**
     * Randomly selects a combat strategy for this session.
     */
    public static CombatStrategy randomForSession() {
        CombatStrategy[] values = values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
