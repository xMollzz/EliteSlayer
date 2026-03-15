package eliteslayer.systems;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Detects nearby players that may represent a threat — potential player
 * moderators, bots competing for the same NPCs, or suspicious onlookers that
 * remain stationary for long periods (which may indicate someone manually
 * observing or reporting).
 *
 * <p>A <em>threat score</em> is maintained as a rolling value.  High scores
 * trigger safety pauses or world hops in the behaviour tree.</p>
 *
 * <h3>Scoring heuristics</h3>
 * <ul>
 *   <li><b>+40</b> — Player has a moderator crown (name starts with a crown
 *       character / is on a known mod list).</li>
 *   <li><b>+20</b> — Player has been nearby and <em>stationary</em> for
 *       &gt;30 s (observer pattern).</li>
 *   <li><b>+10</b> — Player count in the area increased suddenly.</li>
 *   <li><b>+5</b>  — Same player seen across multiple samples (persistent
 *       follower).</li>
 * </ul>
 */
public final class PlayerThreatDetector {

    // ------------------------------------------------------------------ //
    //  Thresholds                                                          //
    // ------------------------------------------------------------------ //

    /** Threat score at which the safety node should pause the script. */
    public static final int PAUSE_THRESHOLD = 35;
    /** Threat score at which a world hop is recommended. */
    public static final int HOP_THRESHOLD   = 55;

    /** How long (ms) a stationary player must linger to be considered suspicious. */
    private static final long STATIONARY_ALERT_MS = 30_000L;
    /** Sampling interval — we don't re-scan faster than this. */
    private static final long SAMPLE_INTERVAL_MS  = 5_000L;
    /** Rolling window length for tracking player sightings (ms). */
    private static final long WINDOW_MS           = 3 * 60_000L;

    // ------------------------------------------------------------------ //
    //  State                                                               //
    // ------------------------------------------------------------------ //

    /** Record of a single player sighting. */
    private static class Sighting {
        final String name;
        final long   timestamp;
        final int    tileX;
        final int    tileY;
        Sighting(String n, long ts, int x, int y) {
            this.name = n; this.timestamp = ts; this.tileX = x; this.tileY = y;
        }
    }

    private final Deque<Sighting> sightings = new ArrayDeque<>();
    private long   lastSampleTime  = 0L;
    private int    lastPlayerCount = 0;
    private int    currentThreat   = 0;

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Samples the environment and recalculates the threat score.
     * Should be called every game tick (guarded internally by a cooldown).
     */
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastSampleTime < SAMPLE_INTERVAL_MS) return;
        lastSampleTime = now;

        Player local = Players.localPlayer();
        if (local == null) { currentThreat = 0; return; }

        Tile centre = local.getTile();
        List<Player> nearby = Players.all();

        // Evict old sightings
        while (!sightings.isEmpty() && now - sightings.peekFirst().timestamp > WINDOW_MS) {
            sightings.pollFirst();
        }

        int threat = 0;
        int nearbyCount = 0;
        Set<String> currentNames = new HashSet<>();

        for (Player p : nearby) {
            if (p == null || p.equals(local)) continue;
            if (p.getTile().distance(centre) > 15) continue;
            nearbyCount++;

            String name = p.getName();
            if (name == null) name = "?";
            currentNames.add(name);

            sightings.addLast(new Sighting(name, now, p.getTile().getX(), p.getTile().getY()));

            // Moderator detection (crown character prefix or known pattern)
            if (isLikelyModerator(name)) {
                threat += 40;
                Logger.warn("[ThreatDetector] Possible moderator nearby: " + name);
            }

            // Stationary observer detection
            if (isStationary(name, now)) {
                threat += 20;
            }

            // Persistent follower detection (seen in >3 distinct samples)
            if (sightingCount(name) > 3) {
                threat += 5;
            }
        }

        // Sudden crowd spike detection
        if (nearbyCount > lastPlayerCount + 2) {
            threat += 10;
        }
        lastPlayerCount = nearbyCount;

        currentThreat = threat;
    }

    /** Returns the current threat score (0 = safe, higher = more dangerous). */
    public int getThreatScore() {
        return currentThreat;
    }

    /** Returns true if the threat level warrants a script pause. */
    public boolean shouldPause() {
        return currentThreat >= PAUSE_THRESHOLD;
    }

    /** Returns true if the threat level warrants a world hop. */
    public boolean shouldHop() {
        return currentThreat >= HOP_THRESHOLD;
    }

    // ------------------------------------------------------------------ //
    //  Heuristics                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Very simple moderator heuristic — checks for known crown-like prefixes
     * or name patterns.  In a real deployment this could check a known-mod
     * list loaded from config.
     */
    private boolean isLikelyModerator(String name) {
        if (name == null) return false;
        // Jagex moderator names often begin with "Mod " prefix
        if (name.startsWith("Mod ")) return true;
        // Crown character (Unicode) — some clients expose it in the name string
        if (name.length() > 0 && name.charAt(0) < 32) return true;
        return false;
    }

    /**
     * Returns true if the named player has been at approximately the same
     * position for longer than {@link #STATIONARY_ALERT_MS}.
     */
    private boolean isStationary(String name, long now) {
        long earliest = Long.MAX_VALUE;
        int refX = -1, refY = -1;
        for (Sighting s : sightings) {
            if (!s.name.equals(name)) continue;
            if (refX == -1) { refX = s.tileX; refY = s.tileY; earliest = s.timestamp; continue; }
            // If any sighting is more than 3 tiles away, they moved — not stationary
            if (Math.abs(s.tileX - refX) > 3 || Math.abs(s.tileY - refY) > 3) return false;
            if (s.timestamp < earliest) earliest = s.timestamp;
        }
        return earliest != Long.MAX_VALUE && (now - earliest) >= STATIONARY_ALERT_MS;
    }

    /** Counts how many distinct time-samples a player was seen in. */
    private int sightingCount(String name) {
        int count = 0;
        for (Sighting s : sightings) {
            if (s.name.equals(name)) count++;
        }
        return count;
    }
}
