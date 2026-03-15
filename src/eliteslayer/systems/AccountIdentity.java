package eliteslayer.systems;

import eliteslayer.util.FileStateStore;
import org.dreambot.api.utilities.Logger;

/**
 * Persistent per-account identity that evolves across sessions.
 *
 * <p>While {@link BehaviorProfile} gives each account a deterministic
 * personality based on its name, {@code AccountIdentity} tracks how the
 * account <em>matures</em> over time.  A fresh account behaves tentatively;
 * a veteran account acts more casually and efficiently — just like a real
 * player would.</p>
 *
 * <p>All values are persisted via {@link FileStateStore} so they survive
 * across script restarts and DreamBot sessions.</p>
 */
public final class AccountIdentity {

    private final FileStateStore store;

    // ------------------------------------------------------------------ //
    //  Persisted metrics                                                   //
    // ------------------------------------------------------------------ //

    private int  totalSessions;
    private long totalPlayTimeMs;
    private long lastSessionStartMs;
    private long lastSessionEndMs;
    private boolean sessionActive;

    // ------------------------------------------------------------------ //
    //  Construction / lifecycle                                             //
    // ------------------------------------------------------------------ //

    public AccountIdentity(FileStateStore store) {
        this.store = store;
        this.totalSessions     = store.getInt("identity.sessions", 0);
        this.totalPlayTimeMs   = store.getLong("identity.playTimeMs", 0L);
        this.lastSessionEndMs  = store.getLong("identity.lastEnd", 0L);
    }

    /** Call once at script start after construction. */
    public void onSessionStart() {
        totalSessions++;
        lastSessionStartMs = System.currentTimeMillis();
        sessionActive = true;
        Logger.log("[AccountIdentity] Session #" + totalSessions
            + " — veteran factor: " + String.format("%.2f", getVeteranFactor()));
    }

    /** Call once at script exit. */
    public void onSessionEnd() {
        if (!sessionActive) return;
        sessionActive = false;
        long now = System.currentTimeMillis();
        long sessionLength = now - lastSessionStartMs;
        totalPlayTimeMs += sessionLength;
        lastSessionEndMs = now;

        store.set("identity.sessions",   String.valueOf(totalSessions));
        store.set("identity.playTimeMs", String.valueOf(totalPlayTimeMs));
        store.set("identity.lastEnd",    String.valueOf(lastSessionEndMs));
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Returns a value in [0, 1] indicating how "veteran" this account is.
     * Newer accounts (fewer sessions, less play time) return values closer
     * to 0; experienced accounts approach 1.  The curve saturates around
     * 50 sessions / 100 hours of total play time.
     */
    public double getVeteranFactor() {
        double sessionFactor  = Math.min(1.0, totalSessions / 50.0);
        double timeFactor     = Math.min(1.0, totalPlayTimeMs / (100.0 * 3_600_000.0));
        return (sessionFactor + timeFactor) / 2.0;
    }

    /**
     * Returns the number of milliseconds since the account's last session
     * ended, or {@link Long#MAX_VALUE} if unknown.  Can be used to adjust
     * initial behaviour (a player returning after a long break might be
     * rusty, while one hopping right back on plays more fluidly).
     */
    public long getTimeSinceLastSession() {
        if (lastSessionEndMs <= 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastSessionEndMs;
    }

    public int  getTotalSessions()   { return totalSessions; }
    public long getTotalPlayTimeMs() { return totalPlayTimeMs; }

    /**
     * Average session length in milliseconds, or 0 if no completed sessions.
     */
    public long getAverageSessionLengthMs() {
        int completed = sessionActive ? totalSessions - 1 : totalSessions;
        if (completed <= 0) return 0L;
        return totalPlayTimeMs / completed;
    }
}
