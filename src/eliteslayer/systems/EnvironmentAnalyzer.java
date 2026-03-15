package eliteslayer.systems;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;

import java.time.LocalTime;
import java.util.List;

/**
 * Extended environment awareness beyond simple crowd tracking.
 *
 * <p>This system monitors time-of-day, detects potential player moderators,
 * and computes a composite risk level that other systems can use to adjust
 * behaviour (e.g. play more cautiously during peak hours or when a J-Mod is
 * nearby).</p>
 */
public final class EnvironmentAnalyzer {

    /** Risk level contributed by peak hours. */
    private static final double PEAK_RISK   = 0.3;
    /** Risk level contributed by a potential moderator nearby. */
    private static final double MOD_RISK    = 0.5;
    /** Risk level contributed by a crowded environment. */
    private static final double CROWD_RISK  = 0.2;

    /** Prefix used by Jagex Moderator display names. */
    private static final String JMOD_PREFIX = "mod ";

    /** Minimum interval between environment scans (ms). */
    private static final long SCAN_INTERVAL_MS = 10_000L;

    private long    lastScanMs        = 0L;
    private boolean peakHours         = false;
    private boolean modDetected       = false;
    private double  riskLevel         = 0.0;

    private final CrowdTracker crowdTracker;

    public EnvironmentAnalyzer(CrowdTracker crowdTracker) {
        this.crowdTracker = crowdTracker;
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Re-evaluates the environment.  Cheap enough to call every tick, but
     * internally throttled to once every {@value SCAN_INTERVAL_MS} ms.
     */
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastScanMs < SCAN_INTERVAL_MS) return;
        lastScanMs = now;

        peakHours   = computePeakHours();
        modDetected = scanForMods();
        riskLevel   = computeRisk();
    }

    /** True if the current real-world time falls in peak gaming hours. */
    public boolean isPeakHours()  { return peakHours; }

    /** True if a potential player moderator was detected nearby. */
    public boolean isModDetected() { return modDetected; }

    /**
     * Composite risk score in [0, 1].  Higher values mean the bot should
     * play more carefully (longer reaction times, fewer risky actions).
     */
    public double getRiskLevel() { return riskLevel; }

    /** Returns the current local hour (0–23). */
    public int getHourOfDay() {
        return LocalTime.now().getHour();
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /** Peak hours: 17:00 – 23:00 local time. */
    private boolean computePeakHours() {
        int hour = getHourOfDay();
        return hour >= 17 && hour <= 23;
    }

    /**
     * Scans nearby players for names that suggest a Jagex moderator.
     * J-Mod names in OSRS start with "Mod " (crown is rendered by the
     * client, the API returns the display name).
     */
    private boolean scanForMods() {
        List<Player> nearby = Players.all();
        if (nearby == null) return false;
        for (Player p : nearby) {
            if (p == null || p.getName() == null) continue;
            if (p.getName().toLowerCase().startsWith(JMOD_PREFIX)) {
                Logger.warn("[EnvironmentAnalyzer] Possible J-Mod nearby: " + p.getName());
                return true;
            }
        }
        return false;
    }

    private double computeRisk() {
        double risk = 0.0;
        if (peakHours)                  risk += PEAK_RISK;
        if (modDetected)                risk += MOD_RISK;
        if (crowdTracker.isCrowded())   risk += CROWD_RISK;
        return Math.min(1.0, risk);
    }
}
