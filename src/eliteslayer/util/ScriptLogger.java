package eliteslayer.util;

import org.dreambot.api.utilities.Logger;

/**
 * Structured logging layer that wraps DreamBot's {@link Logger}.
 *
 * <p>Provides log-level filtering and a consistent {@code [tag]} prefix so
 * that messages from different subsystems are easy to trace.</p>
 *
 * <pre>{@code
 *   ScriptLogger log = new ScriptLogger("CombatNode", ScriptLogger.Level.DEBUG);
 *   log.debug("Target score = " + score);
 *   log.info("Attacking " + npc.getName());
 *   log.warn("No valid targets found");
 *   log.error("Exception during attack", e);
 * }</pre>
 */
public final class ScriptLogger {

    /** Log severity levels. Messages below the configured minimum are suppressed. */
    public enum Level {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3);

        final int value;
        Level(int v) { this.value = v; }
    }

    private static volatile Level globalLevel = Level.INFO;

    private final String tag;
    private final Level  minLevel;

    /**
     * Creates a logger for the given subsystem.
     *
     * @param tag      short subsystem label (e.g. class name)
     * @param minLevel minimum severity to emit
     */
    public ScriptLogger(String tag, Level minLevel) {
        this.tag      = tag;
        this.minLevel = minLevel;
    }

    /** Creates a logger with the default minimum level ({@link Level#INFO}). */
    public ScriptLogger(String tag) {
        this(tag, Level.INFO);
    }

    /** Sets the global minimum level — messages below this are suppressed everywhere. */
    public static void setGlobalLevel(Level level) {
        globalLevel = level;
    }

    public static Level getGlobalLevel() {
        return globalLevel;
    }

    // ------------------------------------------------------------------ //
    //  Public log methods                                                  //
    // ------------------------------------------------------------------ //

    public void debug(String msg) {
        if (shouldLog(Level.DEBUG)) Logger.log("[" + tag + "] " + msg);
    }

    public void info(String msg) {
        if (shouldLog(Level.INFO)) Logger.log("[" + tag + "] " + msg);
    }

    public void warn(String msg) {
        if (shouldLog(Level.WARN)) Logger.warn("[" + tag + "] " + msg);
    }

    public void error(String msg) {
        if (shouldLog(Level.ERROR)) Logger.error("[" + tag + "] " + msg);
    }

    public void error(String msg, Throwable t) {
        if (shouldLog(Level.ERROR)) {
            Logger.error("[" + tag + "] " + msg + ": " + t.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    private boolean shouldLog(Level level) {
        return level.value >= minLevel.value && level.value >= globalLevel.value;
    }
}
