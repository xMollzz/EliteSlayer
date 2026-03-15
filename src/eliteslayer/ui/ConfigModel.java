package eliteslayer.ui;

import eliteslayer.game.MonsterDatabase;
import eliteslayer.game.MonsterDef;

/**
 * Holds all user-configurable settings that are read by the rest of the
 * script.  Separating the model from the GUI view ({@link ConfigGUI}) keeps
 * the configuration testable and avoids leaking Swing state into the
 * game-loop code.
 *
 * <p>Fields are volatile so they can be safely read from the game-loop
 * thread after the EDT writes them.</p>
 */
public final class ConfigModel {

    // ------------------------------------------------------------------ //
    //  General                                                             //
    // ------------------------------------------------------------------ //

    public volatile String  selectedMonster   = "Abyssal Demons";
    public volatile String  discordWebhook    = "";

    // ------------------------------------------------------------------ //
    //  Combat                                                              //
    // ------------------------------------------------------------------ //

    public volatile boolean useCannon         = false;
    public volatile boolean usePrayer         = false;
    public volatile int     eatThreshold      = 50;
    public volatile int     specThreshold     = 50;

    // ------------------------------------------------------------------ //
    //  Supply                                                              //
    // ------------------------------------------------------------------ //

    public volatile boolean useGE             = false;
    public volatile int     foodAmount        = 16;
    public volatile int     potionAmount      = 4;

    // ------------------------------------------------------------------ //
    //  Muling                                                              //
    // ------------------------------------------------------------------ //

    public volatile boolean useMule           = false;
    public volatile String  muleName          = "";
    public volatile String  muleX             = "3213";
    public volatile String  muleY             = "3424";

    // ------------------------------------------------------------------ //
    //  State                                                               //
    // ------------------------------------------------------------------ //

    /** Set to true when the user presses "Start Script". */
    public volatile boolean started           = false;

    /**
     * Applies sensible defaults from the selected monster definition
     * (cannon and prayer settings).
     */
    public void applyMonsterDefaults() {
        MonsterDef def = MonsterDatabase.get(selectedMonster);
        if (def == null) return;
        useCannon = def.usesCannon;
        usePrayer = def.usesPrayer;
    }
}
