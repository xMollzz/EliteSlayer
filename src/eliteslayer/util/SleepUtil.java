package eliteslayer.util;

import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

/**
 * Retry-interact helpers and conditional sleep utilities.
 * All retry methods record telemetry on success/failure.
 */
public final class SleepUtil {

    private static final int RETRY_SLEEP_MS = 300;

    private SleepUtil() {}

    /** Retry interacting with an NPC up to {@code attempts} times. */
    public static boolean retryInteract(NPC npc, String action, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (npc != null && npc.interact(action)) {
                Telemetry.recordSuccess();
                return true;
            }
            Sleep.sleep(RETRY_SLEEP_MS);
        }
        Telemetry.recordFailure();
        return false;
    }

    /** Retry interacting with a GameObject up to {@code attempts} times. */
    public static boolean retryInteract(GameObject obj, String action, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (obj != null && obj.interact(action)) {
                Telemetry.recordSuccess();
                return true;
            }
            Sleep.sleep(RETRY_SLEEP_MS);
        }
        Telemetry.recordFailure();
        return false;
    }

    /** Retry interacting with a GroundItem up to {@code attempts} times. */
    public static boolean retryInteract(GroundItem item, String action, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (item != null && item.interact(action)) {
                Telemetry.recordSuccess();
                return true;
            }
            Sleep.sleep(RETRY_SLEEP_MS);
        }
        Telemetry.recordFailure();
        return false;
    }

    /** Retry interacting with an inventory Item up to {@code attempts} times. */
    public static boolean retryInteract(Item item, String action, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (item != null && item.interact(action)) {
                Telemetry.recordSuccess();
                return true;
            }
            Sleep.sleep(RETRY_SLEEP_MS);
        }
        Telemetry.recordFailure();
        return false;
    }

    /**
     * Sleeps until {@code condition} returns true, checking every
     * {@code intervalMs} milliseconds up to {@code timeoutMs}.
     */
    public static boolean sleepUntil(java.util.function.BooleanSupplier condition,
                                      long timeoutMs, long intervalMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return true;
            Sleep.sleep((int) intervalMs);
        }
        return false;
    }

    /**
     * Convenience overload: checks every 600 ms.
     */
    public static boolean sleepUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        return sleepUntil(condition, timeoutMs, 600);
    }
}
