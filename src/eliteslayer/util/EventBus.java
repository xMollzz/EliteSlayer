package eliteslayer.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight publish-subscribe event bus.
 *
 * <p>Systems can publish events instead of polling. Listeners receive events
 * synchronously on the publisher's thread (fine for a single-threaded game
 * loop) which avoids the overhead of a dedicated dispatch thread.</p>
 *
 * <pre>{@code
 *   eventBus.subscribe("kill", e -> Telemetry.addKill());
 *   eventBus.publish("kill", Map.of("npc", "Abyssal Demon"));
 * }</pre>
 */
public final class EventBus {

    @FunctionalInterface
    public interface Listener {
        void onEvent(String topic, Object payload);
    }

    private final Map<String, List<Listener>> listeners = new ConcurrentHashMap<>();

    /**
     * Registers a listener for the given topic.
     *
     * @param topic    event topic name (e.g. "kill", "loot", "break_start")
     * @param listener callback to invoke when the event is published
     */
    public void subscribe(String topic, Listener listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Publishes an event to all listeners subscribed to the given topic.
     *
     * @param topic   event topic name
     * @param payload arbitrary data associated with the event (may be {@code null})
     */
    public void publish(String topic, Object payload) {
        List<Listener> subs = listeners.get(topic);
        if (subs == null) return;
        for (Listener l : subs) {
            try {
                l.onEvent(topic, payload);
            } catch (Exception e) {
                // Log but don't propagate — protect the publisher from listener errors
                System.err.println("[EventBus] Listener error on topic '" + topic + "': " + e.getMessage());
            }
        }
    }

    /**
     * Convenience overload — publishes with a {@code null} payload.
     */
    public void publish(String topic) {
        publish(topic, null);
    }

    /**
     * Removes all listeners for a specific topic.
     */
    public void clear(String topic) {
        listeners.remove(topic);
    }

    /**
     * Removes all listeners for all topics.
     */
    public void clearAll() {
        listeners.clear();
    }
}
