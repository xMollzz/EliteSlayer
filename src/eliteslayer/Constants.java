package eliteslayer;

/**
 * Centralised constants — eliminates magic numbers scattered across the
 * codebase and provides a single place to tune key values.
 */
public final class Constants {

    private Constants() {}

    // ------------------------------------------------------------------ //
    //  Combat scoring weights                                              //
    // ------------------------------------------------------------------ //

    /** Distance penalty per tile when scoring NPC targets. */
    public static final double SCORE_DISTANCE_WEIGHT     = 15.0;
    /** Penalty applied to NPCs that are already in combat. */
    public static final double SCORE_IN_COMBAT_PENALTY   = 300.0;
    /** Health-percent penalty weight when scoring NPC targets. */
    public static final double SCORE_HEALTH_WEIGHT       = 2.0;
    /** Bonus for NPCs that are interactable. */
    public static final double SCORE_INTERACTABLE_BONUS  = 50.0;
    /** Bonus for NPCs that are idle (no animation). */
    public static final double SCORE_IDLE_BONUS          = 30.0;
    /** Penalty per nearby player when scoring NPC targets. */
    public static final double SCORE_CROWD_PENALTY       = 25.0;
    /** Penalty for an NPC that was recently failed to attack. */
    public static final double SCORE_REAGGRO_PENALTY     = 150.0;
    /** Random jitter range (± this value) added to target scores. */
    public static final double SCORE_JITTER_RANGE        = 15.0;
    /** Base score from which deductions are subtracted. */
    public static final double SCORE_BASE                = 1000.0;
    /** Radius (tiles) for counting nearby players around an NPC. */
    public static final int    CROWD_RADIUS              = 4;

    // ------------------------------------------------------------------ //
    //  Combat distances & timeouts                                         //
    // ------------------------------------------------------------------ //

    /** Maximum distance (tiles) to the fight area before walking. */
    public static final int  COMBAT_RANGE           = 15;
    /** Timeout (ms) for walking to the fight area. */
    public static final long WALK_TO_FIGHT_TIMEOUT   = 15_000L;
    /** Time (ms) to wait for the player to enter combat after attacking. */
    public static final int  COMBAT_START_TIMEOUT    = 4_000;
    /** Time window (ms) during which a failed NPC is penalized. */
    public static final long REAGGRO_PENALTY_MS      = 30_000L;
    /** Sleep duration (ms) when no valid target is found. */
    public static final int  NO_TARGET_SLEEP_MS      = 600;
    /** OSRS animation ID indicating an NPC is idle. */
    public static final int  IDLE_ANIMATION_ID       = -1;

    // ------------------------------------------------------------------ //
    //  Navigation                                                          //
    // ------------------------------------------------------------------ //

    /** Arrival threshold — player is "at" a tile within this distance. */
    public static final int  ARRIVAL_DISTANCE  = 3;
    /** Default walk timeout (ms). */
    public static final long WALK_TIMEOUT_MS   = 10_000L;

    // ------------------------------------------------------------------ //
    //  Stuck detection                                                     //
    // ------------------------------------------------------------------ //

    /** Movement threshold — player must move more than this to count. */
    public static final int STUCK_MOVE_THRESHOLD = 2;
}
