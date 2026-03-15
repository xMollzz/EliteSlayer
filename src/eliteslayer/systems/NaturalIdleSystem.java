package eliteslayer.systems;

import org.dreambot.api.methods.camera.Camera;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Natural idle simulation — probabilistically triggers small human-like
 * behaviours each tick so the bot doesn't appear 100 % efficient.
 *
 * <p>On each call to {@link #maybeFidget(double)}, the system rolls against
 * configurable probabilities (scaled by the profile's idle-chance multiplier)
 * and performs at most one idle action:</p>
 * <ul>
 *   <li>3 % — short idle (2–7 s)</li>
 *   <li>2 % — check stats / inventory</li>
 *   <li>1 % — rotate camera</li>
 * </ul>
 *
 * <p>The fatigue level further increases these chances, mimicking a tired
 * player fidgeting more.</p>
 */
public final class NaturalIdleSystem {

    private static final double BASE_IDLE_CHANCE    = 0.03;
    private static final double BASE_STATS_CHANCE   = 0.02;
    private static final double BASE_CAMERA_CHANCE  = 0.01;

    /** Fatigue bonus per fatigue unit (e.g. +1 % idle per hour). */
    private static final double FATIGUE_IDLE_RATE   = 0.01;

    private long lastFidget = 0L;

    /** Minimum gap between fidgets to avoid clustering. */
    private static final long MIN_GAP_MS = 8_000L;

    /**
     * Possibly performs a small idle action.  Returns true if an action was
     * performed (callers may want to skip their own logic in that case).
     *
     * @param idleMultiplier profile-based multiplier (e.g. CASUAL = 1.6)
     */
    public boolean maybeFidget(double idleMultiplier) {
        return maybeFidget(idleMultiplier, 0.0);
    }

    /**
     * Possibly performs a small idle action, scaled by both the player
     * profile multiplier and current fatigue.
     *
     * @param idleMultiplier profile-based multiplier
     * @param fatigue        current fatigue level (0.0 = fresh, 1.0 = 1 hour)
     * @return true if an action was performed
     */
    public boolean maybeFidget(double idleMultiplier, double fatigue) {
        long now = System.currentTimeMillis();
        if (now - lastFidget < MIN_GAP_MS) return false;

        double scale  = idleMultiplier * (1.0 + fatigue * FATIGUE_IDLE_RATE / BASE_IDLE_CHANCE);
        double roll   = ThreadLocalRandom.current().nextDouble();

        double idleThreshold   = BASE_IDLE_CHANCE   * scale;
        double statsThreshold  = BASE_STATS_CHANCE   * scale;
        double cameraThreshold = BASE_CAMERA_CHANCE  * scale;

        if (roll < cameraThreshold) {
            lastFidget = now;
            rotateCamera();
            return true;
        } else if (roll < cameraThreshold + statsThreshold) {
            lastFidget = now;
            checkStats();
            return true;
        } else if (roll < cameraThreshold + statsThreshold + idleThreshold) {
            lastFidget = now;
            shortIdle();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Idle actions                                                        //
    // ------------------------------------------------------------------ //

    private void shortIdle() {
        int delay = ThreadLocalRandom.current().nextInt(2_000, 7_001);
        Logger.log("[NaturalIdle] Short idle for " + delay + " ms");
        Sleep.sleep(delay);
    }

    private void checkStats() {
        Tab[] tabs = { Tab.SKILLS, Tab.INVENTORY, Tab.EQUIPMENT };
        Tab tab = tabs[ThreadLocalRandom.current().nextInt(tabs.length)];
        Tabs.open(tab);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(800, 2_500));
        Logger.log("[NaturalIdle] Checked " + tab.name());
    }

    private void rotateCamera() {
        Camera.rotateTo(
            ThreadLocalRandom.current().nextInt(0, 360),
            ThreadLocalRandom.current().nextInt(30, 80));
        Logger.log("[NaturalIdle] Camera fidget");
    }
}
