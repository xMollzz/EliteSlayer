package eliteslayer.systems;

import org.dreambot.api.methods.world.World;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Smart world-hopping system that selects target worlds based on crowd
 * density, threat level, and recently-visited history.
 *
 * <h3>Selection criteria</h3>
 * <ol>
 *   <li>Must be a members world (slayer content).</li>
 *   <li>Must not be full (&gt;1900 players).</li>
 *   <li>Prefers worlds with low player count (less competition).</li>
 *   <li>Avoids worlds we recently hopped <em>from</em> (cooldown list).</li>
 *   <li>Adds small randomness so the pattern isn't deterministic.</li>
 * </ol>
 *
 * <h3>Rate limiting</h3>
 * <p>A minimum interval between hops prevents spam-hopping which is both
 * suspicious and throttled by the game server.</p>
 */
public final class WorldHopSystem {

    // ------------------------------------------------------------------ //
    //  Tunables                                                            //
    // ------------------------------------------------------------------ //

    /** Minimum milliseconds between world hops. */
    private static final long   MIN_HOP_INTERVAL_MS = 60_000L;
    /** Worlds hopped from in the last N minutes are avoided. */
    private static final long   COOLDOWN_MS         = 10 * 60_000L;
    /** Maximum player count we consider "acceptable". */
    private static final int    MAX_WORLD_POPULATION = 1_900;
    /** Preferred upper-bound population — worlds under this are ideal. */
    private static final int    IDEAL_POPULATION     = 600;

    // ------------------------------------------------------------------ //
    //  State                                                               //
    // ------------------------------------------------------------------ //

    private static class HopRecord {
        final int  worldId;
        final long timestamp;
        HopRecord(int w, long ts) { this.worldId = w; this.timestamp = ts; }
    }

    private final Deque<HopRecord> recentHops = new ArrayDeque<>();
    private long lastHopTime  = 0L;
    private int  hopCount     = 0;

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Returns true if enough time has passed since the last hop to allow
     * another one.
     */
    public boolean canHop() {
        return System.currentTimeMillis() - lastHopTime >= MIN_HOP_INTERVAL_MS;
    }

    /**
     * Attempts to hop to a suitable world.
     *
     * @return true if the hop was initiated successfully, false if no
     *         suitable world was found or rate-limited.
     */
    public boolean hop() {
        if (!canHop()) {
            Logger.warn("[WorldHop] Rate-limited — too soon since last hop.");
            return false;
        }

        long now = System.currentTimeMillis();

        // Evict old cooldown entries
        while (!recentHops.isEmpty() && now - recentHops.peekFirst().timestamp > COOLDOWN_MS) {
            recentHops.pollFirst();
        }

        Set<Integer> avoidWorlds = new HashSet<>();
        for (HopRecord r : recentHops) avoidWorlds.add(r.worldId);

        // Also avoid the current world
        int currentWorld = Worlds.getCurrentWorld();
        avoidWorlds.add(currentWorld);

        World best = selectBestWorld(avoidWorlds);
        if (best == null) {
            Logger.warn("[WorldHop] No suitable world found.");
            return false;
        }

        Logger.log("[WorldHop] Hopping to world " + best.getWorld()
            + " (pop: " + best.getPlayerCount() + ")");

        boolean ok = Worlds.hopWorld(best.getWorld());
        if (ok) {
            lastHopTime = now;
            hopCount++;
            recentHops.addLast(new HopRecord(currentWorld, now));
            Sleep.sleep(3_000 + ThreadLocalRandom.current().nextInt(2_000));
        }
        return ok;
    }

    /** Total number of world hops this session. */
    public int getHopCount() {
        return hopCount;
    }

    // ------------------------------------------------------------------ //
    //  World selection                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Single-pass selection of the lowest-population members world that is
     * not on cooldown, not full, and adds a small random perturbation to
     * avoid always picking the same world.
     */
    private World selectBestWorld(Set<Integer> avoid) {
        World best      = null;
        int   bestScore = Integer.MAX_VALUE;

        List<World> all = Worlds.all();
        if (all == null) return null;

        for (World w : all) {
            if (w == null) continue;
            if (!w.isMembers()) continue;
            if (w.isFull()) continue;
            if (w.getPlayerCount() > MAX_WORLD_POPULATION) continue;
            if (avoid.contains(w.getWorld())) continue;

            // Score: lower is better (prefer empty worlds)
            int score = w.getPlayerCount()
                + ThreadLocalRandom.current().nextInt(0, 50); // small jitter

            // Bonus for being under the ideal threshold
            if (w.getPlayerCount() <= IDEAL_POPULATION) {
                score -= 200;
            }

            if (score < bestScore) {
                bestScore = score;
                best      = w;
            }
        }
        return best;
    }

}
