package eliteslayer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private EventBus bus;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
    }

    @Test
    void subscriberReceivesPublishedEvent() {
        AtomicInteger calls = new AtomicInteger(0);
        bus.subscribe("kill", (topic, payload) -> calls.incrementAndGet());

        bus.publish("kill");
        assertEquals(1, calls.get());
    }

    @Test
    void subscriberReceivesPayload() {
        List<Object> received = new ArrayList<>();
        bus.subscribe("loot", (topic, payload) -> received.add(payload));

        bus.publish("loot", "Abyssal whip");
        assertEquals(1, received.size());
        assertEquals("Abyssal whip", received.get(0));
    }

    @Test
    void multipleSubscribersAllCalled() {
        AtomicInteger a = new AtomicInteger(0);
        AtomicInteger b = new AtomicInteger(0);
        bus.subscribe("kill", (t, p) -> a.incrementAndGet());
        bus.subscribe("kill", (t, p) -> b.incrementAndGet());

        bus.publish("kill");
        assertEquals(1, a.get());
        assertEquals(1, b.get());
    }

    @Test
    void unrelatedTopicNotTriggered() {
        AtomicInteger calls = new AtomicInteger(0);
        bus.subscribe("kill", (t, p) -> calls.incrementAndGet());

        bus.publish("loot");
        assertEquals(0, calls.get());
    }

    @Test
    void clearRemovesListeners() {
        AtomicInteger calls = new AtomicInteger(0);
        bus.subscribe("kill", (t, p) -> calls.incrementAndGet());
        bus.clear("kill");

        bus.publish("kill");
        assertEquals(0, calls.get());
    }

    @Test
    void clearAllRemovesAllListeners() {
        AtomicInteger a = new AtomicInteger(0);
        AtomicInteger b = new AtomicInteger(0);
        bus.subscribe("kill", (t, p) -> a.incrementAndGet());
        bus.subscribe("loot", (t, p) -> b.incrementAndGet());
        bus.clearAll();

        bus.publish("kill");
        bus.publish("loot");
        assertEquals(0, a.get());
        assertEquals(0, b.get());
    }

    @Test
    void listenerExceptionDoesNotBreakOthers() {
        AtomicInteger calls = new AtomicInteger(0);
        bus.subscribe("kill", (t, p) -> { throw new RuntimeException("boom"); });
        bus.subscribe("kill", (t, p) -> calls.incrementAndGet());

        // Should not throw
        bus.publish("kill");
        assertEquals(1, calls.get(), "Second listener should still be called");
    }

    @Test
    void publishWithNullPayload() {
        List<Object> received = new ArrayList<>();
        bus.subscribe("test", (t, p) -> received.add(p));

        bus.publish("test");
        assertEquals(1, received.size());
        assertNull(received.get(0));
    }
}
