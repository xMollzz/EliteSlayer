package eliteslayer.systems;

import org.dreambot.api.methods.camera.Camera;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.World;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Social environment awareness — reacts to nearby players so the bot
 * doesn't look oblivious to its surroundings.
 *
 * <p>When other players are nearby, the system may:</p>
 * <ul>
 *   <li>Briefly idle (appear aware)</li>
 *   <li>Rotate camera towards the player</li>
 *   <li>Walk a short random distance</li>
 * </ul>
 *
 * <p>When the area is very crowded the system recommends a world hop.</p>
 */
public final class SocialAwareness {

    /** Minimum time between social reactions (ms). */
    private static final long REACTION_COOLDOWN_MS = 30_000L;

    /** World-hop cooldown — don't hop more than once every 3 minutes. */
    private static final long WORLD_HOP_COOLDOWN_MS = 180_000L;

    /** Number of nearby players that triggers a world-hop recommendation. */
    private static final int CROWDED_THRESHOLD = 5;

    private long lastReaction = 0L;
    private long lastWorldHop = 0L;

    /** Number of unique player names seen near us last check. */
    private int lastSeenCount = 0;

    /**
     * Call once per main loop.  Observes nearby players and may trigger
     * a social reaction.
     *
     * @return true if the bot should skip its normal action this tick
     *         (because a social reaction consumed the tick)
     */
    public boolean react() {
        Player local = Players.localPlayer();
        if (local == null) return false;

        List<Player> nearby = Players.all();
        int count = 0;
        if (nearby != null) {
            for (Player p : nearby) {
                if (p != null && !p.equals(local) && p.getTile().distance(local.getTile()) <= 15) {
                    count++;
                }
            }
        }

        boolean newPlayerAppeared = count > lastSeenCount && count > 0;
        lastSeenCount = count;

        long now = System.currentTimeMillis();

        // React to a new player appearing nearby
        if (newPlayerAppeared && now - lastReaction > REACTION_COOLDOWN_MS) {
            lastReaction = now;
            performReaction(local);
            return true;
        }

        return false;
    }

    /**
     * Returns true if the area is too crowded and a world hop is recommended.
     */
    public boolean shouldWorldHop() {
        long now = System.currentTimeMillis();
        if (now - lastWorldHop < WORLD_HOP_COOLDOWN_MS) return false;
        return lastSeenCount >= CROWDED_THRESHOLD;
    }

    /**
     * Attempts to hop to a random valid members' world, filtering out
     * PVP, high-risk, and skill-total requirement worlds.
     * @return true if the hop was initiated
     */
    public boolean hopWorld() {
        List<World> worlds = Worlds.all();
        if (worlds == null || worlds.isEmpty()) return false;

        // Filter to regular members worlds only
        java.util.ArrayList<World> valid = new java.util.ArrayList<>();
        for (World w : worlds) {
            if (w == null) continue;
            if (!w.isMembers()) continue;
            if (w.isPVP()) continue;
            if (w.isHighRisk()) continue;
            if (w.getMinimumLevel() > 0) continue;
            valid.add(w);
        }
        if (valid.isEmpty()) return false;

        World target = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        Logger.log("[SocialAwareness] World hopping to " + target.getWorld());
        boolean ok = Worlds.hopWorld(target);
        if (ok) {
            lastWorldHop = System.currentTimeMillis();
            Sleep.sleep(ThreadLocalRandom.current().nextInt(3_000, 6_000));
        }
        return ok;
    }

    // ------------------------------------------------------------------ //
    //  Reaction behaviours                                                 //
    // ------------------------------------------------------------------ //

    private void performReaction(Player local) {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 40) {
            // Brief idle — appear to notice the player
            int delay = ThreadLocalRandom.current().nextInt(1_500, 4_000);
            Logger.log("[SocialAwareness] Noticed a player — idling " + delay + " ms");
            Sleep.sleep(delay);
        } else if (roll < 70) {
            // Rotate camera
            Logger.log("[SocialAwareness] Noticed a player — rotating camera");
            Camera.rotateTo(
                ThreadLocalRandom.current().nextInt(0, 360),
                ThreadLocalRandom.current().nextInt(30, 80));
        } else {
            // Walk a short random distance
            Tile localTile = local.getTile();
            int dx = ThreadLocalRandom.current().nextInt(-3, 4);
            int dy = ThreadLocalRandom.current().nextInt(-3, 4);
            Tile walkTo = new Tile(localTile.getX() + dx, localTile.getY() + dy, localTile.getZ());
            Logger.log("[SocialAwareness] Noticed a player — walking slightly");
            Walking.walk(walkTo);
            Sleep.sleep(ThreadLocalRandom.current().nextInt(1_000, 2_500));
        }
    }
}
