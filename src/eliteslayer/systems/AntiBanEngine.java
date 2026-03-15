package eliteslayer.systems;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;

import java.awt.AWTException;
import java.awt.Robot;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 17-action anti-ban engine with per-action cooldowns.
 * All actions use ThreadLocalRandom (optimisation 3).
 * Hot-path target selection uses a single-pass for-loop (optimisation 11).
 */
public final class AntiBanEngine {

    // ------------------------------------------------------------------ //
    //  Action registry                                                     //
    // ------------------------------------------------------------------ //

    private static final int ACTION_COUNT = 17;

    /** Human-readable labels for HUD / logging. */
    private static final String[] LABELS = {
        "Mouse wander",
        "Camera rotate",
        "Tab switch",
        "Random sleep",
        "Check XP",
        "Hover inventory",
        "Hover minimap",
        "Scroll chat",
        "Mouse off-screen",
        "Examine nearby",
        "Check skills",
        "Right-click random",
        "Yawn pause",
        "Zoom camera",
        "Afk glance",
        "Toggle run",
        "Check equipment"
    };

    /** Relative weights — higher = triggered more often. */
    private static final int[] WEIGHTS = {
        25,  // mouse wander
        20,  // camera rotate
        15,  // tab switch
        30,  // random sleep
        10,  // check XP
        20,  // hover inventory
        15,  // hover minimap
        10,  // scroll chat
        12,  // mouse off-screen
        8,   // examine nearby
        10,  // check skills
        12,  // right-click random
        18,  // yawn pause
        10,  // zoom camera
        20,  // afk glance
        5,   // toggle run
        8    // check equipment
    };

    /** Minimum cooldown between consecutive triggers of the same action (ms). */
    private static final long[] COOLDOWNS = {
        5_000,   // mouse wander
        8_000,   // camera rotate
        12_000,  // tab switch
        3_000,   // random sleep
        20_000,  // check XP
        6_000,   // hover inventory
        7_000,   // hover minimap
        15_000,  // scroll chat
        30_000,  // mouse off-screen
        25_000,  // examine nearby
        25_000,  // check skills
        10_000,  // right-click random
        45_000,  // yawn pause
        20_000,  // zoom camera
        10_000,  // afk glance
        60_000,  // toggle run
        30_000   // check equipment
    };

    private static final int TOTAL_WEIGHT;
    static {
        int sum = 0;
        for (int w : WEIGHTS) sum += w;
        TOTAL_WEIGHT = sum;
    }

    private final long[]          lastTriggered = new long[ACTION_COUNT];
    private final EntropyMonitor  entropy;
    /** Cached AWT Robot for keyboard simulation — null if AWTException prevented creation. */
    private Robot awtRobot;

    public AntiBanEngine(EntropyMonitor entropy) {
        this.entropy = entropy;
        try { this.awtRobot = new Robot(); } catch (AWTException ignored) { this.awtRobot = null; }
    }

    /**
     * Probabilistically selects and executes one anti-ban action.
     * Skips actions that are still on cooldown or for which the weight roll
     * fails.  Should be called infrequently (e.g. every 4–8 seconds).
     */
    public void act() {
        int chosen = weightedSelect();
        if (chosen < 0) return;
        long now = System.currentTimeMillis();
        if (now - lastTriggered[chosen] < COOLDOWNS[chosen]) return;
        lastTriggered[chosen] = now;
        entropy.record(chosen);
        execute(chosen);
    }

    public String getLastLabel() {
        // Find most recently triggered
        int last  = 0;
        long best = 0L;
        for (int i = 0; i < ACTION_COUNT; i++) {
            if (lastTriggered[i] > best) { best = lastTriggered[i]; last = i; }
        }
        return LABELS[last];
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /** Weighted random selection — single-pass, no streams (optimisation 11). */
    private int weightedSelect() {
        int roll = ThreadLocalRandom.current().nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (int i = 0; i < ACTION_COUNT; i++) {
            cumulative += WEIGHTS[i];
            if (roll < cumulative) return i;
        }
        return ACTION_COUNT - 1;
    }

    private void execute(int action) {
        try {
            switch (action) {
                case 0: mouseWander();      break;
                case 1: cameraRotate();     break;
                case 2: tabSwitch();        break;
                case 3: randomSleep();      break;
                case 4: checkXP();          break;
                case 5: hoverInventory();   break;
                case 6: hoverMinimap();     break;
                case 7: scrollChat();       break;
                case 8: mouseOffScreen();   break;
                case 9: examineNearby();    break;
                case 10: checkSkills();     break;
                case 11: rightClickRandom();break;
                case 12: yawnPause();       break;
                case 13: zoomCamera();      break;
                case 14: afkGlance();       break;
                case 15: toggleRun();       break;
                case 16: checkEquipment();  break;
                default: break;
            }
        } catch (Exception e) {
            Logger.warn("[AntiBan] Action " + LABELS[action] + " failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Action implementations                                              //
    // ------------------------------------------------------------------ //

    private void mouseWander() {
        org.dreambot.api.input.Mouse.move(
            ThreadLocalRandom.current().nextInt(50, 750),
            ThreadLocalRandom.current().nextInt(50, 500));
    }

    private void cameraRotate() {
        org.dreambot.api.methods.camera.Camera.rotateTo(
            ThreadLocalRandom.current().nextInt(0, 360),
            ThreadLocalRandom.current().nextInt(30, 80));
    }

    private void tabSwitch() {
        Tab[] tabs = {
            Tab.COMBAT, Tab.SKILLS, Tab.QUESTS,
            Tab.INVENTORY, Tab.EQUIPMENT, Tab.PRAYER, Tab.MAGIC
        };
        Tabs.open(tabs[ThreadLocalRandom.current().nextInt(tabs.length)]);
    }

    private void randomSleep() {
        Sleep.sleep(ThreadLocalRandom.current().nextInt(300, 2500));
    }

    private void checkXP() {
        Tabs.open(Tab.SKILLS);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(800, 2000));
    }

    private void hoverInventory() {
        Tabs.open(Tab.INVENTORY);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(400, 1200));
    }

    private void hoverMinimap() {
        org.dreambot.api.input.Mouse.move(
            ThreadLocalRandom.current().nextInt(565, 740),
            ThreadLocalRandom.current().nextInt(5, 145));
    }

    private void scrollChat() {
        // Scroll with mouse wheel in the chat area
        org.dreambot.api.input.Mouse.move(
            ThreadLocalRandom.current().nextInt(5, 510),
            ThreadLocalRandom.current().nextInt(450, 500));
        Sleep.sleep(ThreadLocalRandom.current().nextInt(200, 600));
    }

    private void mouseOffScreen() {
        org.dreambot.api.input.Mouse.move(-1, -1);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(500, 3000));
    }

    private void examineNearby() {
        Player local = Players.localPlayer();
        if (local == null) return;
        java.util.List<Player> others = Players.all();
        if (others != null && !others.isEmpty()) {
            Player target = others.get(ThreadLocalRandom.current().nextInt(others.size()));
            if (target != null && !target.equals(local)) target.interact("Examine");
        }
    }

    private void checkSkills() {
        Tabs.open(Tab.SKILLS);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(1500, 4000));
    }

    private void rightClickRandom() {
        org.dreambot.api.input.Mouse.move(
            ThreadLocalRandom.current().nextInt(100, 620),
            ThreadLocalRandom.current().nextInt(100, 400));
    }

    private void yawnPause() {
        Sleep.sleep(ThreadLocalRandom.current().nextInt(3000, 8000));
    }

    private void zoomCamera() {
        if (awtRobot == null) return;
        // Use = (zoom in) or - (zoom out) keys — VK_EQUALS is '+' on standard keyboards
        int key = ThreadLocalRandom.current().nextBoolean()
            ? java.awt.event.KeyEvent.VK_EQUALS
            : java.awt.event.KeyEvent.VK_MINUS;
        int presses = ThreadLocalRandom.current().nextInt(1, 5);
        for (int i = 0; i < presses; i++) {
            awtRobot.keyPress(key);
            awtRobot.keyRelease(key);
            Sleep.sleep(ThreadLocalRandom.current().nextInt(80, 200));
        }
    }

    private void afkGlance() {
        org.dreambot.api.input.Mouse.move(-1, -1);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(1500, 6000));
    }

    private void toggleRun() {
        if (!org.dreambot.api.methods.walking.impl.Walking.isRunEnabled()) {
            org.dreambot.api.methods.walking.impl.Walking.toggleRun();
        }
    }

    private void checkEquipment() {
        Tabs.open(Tab.EQUIPMENT);
        Sleep.sleep(ThreadLocalRandom.current().nextInt(800, 2500));
    }
}
