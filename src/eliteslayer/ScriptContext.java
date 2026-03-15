package eliteslayer;

import eliteslayer.game.MonsterDef;
import eliteslayer.systems.*;
import eliteslayer.util.*;

/**
 * Shared context object that holds references to every subsystem.
 *
 * <p>Instead of passing many individual objects through constructors, nodes
 * receive a single {@code ScriptContext} that provides access to all shared
 * services.  This simplifies dependency injection and makes it trivial to
 * add new subsystems without changing every constructor signature.</p>
 */
public final class ScriptContext {

    // ------------------------------------------------------------------ //
    //  Core systems                                                        //
    // ------------------------------------------------------------------ //
    public final EntropyMonitor  entropy;
    public final CrowdTracker    crowd;
    public final AntiBanEngine   antiBan;
    public final BreakScheduler  breaks;
    public final StuckDetector   stuck;

    // ------------------------------------------------------------------ //
    //  Infrastructure                                                      //
    // ------------------------------------------------------------------ //
    public final EventBus        eventBus;
    public final Profiler        profiler;
    public final DiscordWebhook  discord;
    public final FileStateStore  state;
    public final ScriptLogger    logger;

    // ------------------------------------------------------------------ //
    //  Configuration snapshot (read-only after start)                       //
    // ------------------------------------------------------------------ //
    public final MonsterDef monster;
    public final boolean    useCannon;
    public final boolean    usePrayer;
    public final boolean    useGE;
    public final boolean    useMule;
    public final String     muleName;
    public final int[]      mulePosition;
    public final int        eatThreshold;
    public final int        specThreshold;
    public final int        foodAmount;
    public final int        potionAmount;
    public final String     protectionPrayer;

    /**
     * Constructs the context — meant to be called once in
     * {@code EliteSlayer.onStart()} after the GUI has been dismissed.
     */
    public ScriptContext(EntropyMonitor entropy, CrowdTracker crowd,
                         AntiBanEngine antiBan, BreakScheduler breaks,
                         StuckDetector stuck, EventBus eventBus,
                         Profiler profiler, DiscordWebhook discord,
                         FileStateStore state, ScriptLogger logger,
                         MonsterDef monster, boolean useCannon,
                         boolean usePrayer, boolean useGE,
                         boolean useMule, String muleName, int[] mulePosition,
                         int eatThreshold, int specThreshold,
                         int foodAmount, int potionAmount,
                         String protectionPrayer) {
        this.entropy          = entropy;
        this.crowd            = crowd;
        this.antiBan          = antiBan;
        this.breaks           = breaks;
        this.stuck            = stuck;
        this.eventBus         = eventBus;
        this.profiler         = profiler;
        this.discord          = discord;
        this.state            = state;
        this.logger           = logger;
        this.monster          = monster;
        this.useCannon        = useCannon;
        this.usePrayer        = usePrayer;
        this.useGE            = useGE;
        this.useMule          = useMule;
        this.muleName         = muleName;
        this.mulePosition     = mulePosition;
        this.eatThreshold     = eatThreshold;
        this.specThreshold    = specThreshold;
        this.foodAmount       = foodAmount;
        this.potionAmount     = potionAmount;
        this.protectionPrayer = protectionPrayer;
    }
}
