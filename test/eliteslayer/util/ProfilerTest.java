package eliteslayer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfilerTest {

    private Profiler profiler;

    @BeforeEach
    void setUp() {
        profiler = new Profiler();
    }

    @Test
    void recordsTimingForSection() throws Exception {
        profiler.start("test");
        Thread.sleep(10);  // sleep a bit so timing is non-zero
        profiler.stop("test");

        Profiler.Stats stats = profiler.getStats("test");
        assertNotNull(stats);
        assertEquals(1, stats.getCount());
        assertTrue(stats.getTotalNs() > 0, "Total ns should be > 0");
        assertTrue(stats.getAverageNs() > 0, "Average ns should be > 0");
        assertTrue(stats.getMaxNs() > 0, "Max ns should be > 0");
        assertTrue(stats.getMinNs() > 0, "Min ns should be > 0");
    }

    @Test
    void multipleRecordingsAccumulate() {
        for (int i = 0; i < 5; i++) {
            profiler.start("multi");
            // Busy-wait a tiny amount
            long end = System.nanoTime() + 1000;
            while (System.nanoTime() < end) { /* spin */ }
            profiler.stop("multi");
        }

        Profiler.Stats stats = profiler.getStats("multi");
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
    }

    @Test
    void unknownSectionReturnsNull() {
        assertNull(profiler.getStats("nonexistent"));
    }

    @Test
    void stopWithoutStartIsIgnored() {
        // Should not throw
        profiler.stop("never_started");
        assertNull(profiler.getStats("never_started"));
    }

    @Test
    void resetClearsAllStats() {
        profiler.start("a");
        profiler.stop("a");
        profiler.reset();

        assertNull(profiler.getStats("a"));
        assertTrue(profiler.getAllStats().isEmpty());
    }

    @Test
    void getAllStatsReturnsAllSections() {
        profiler.start("x");
        profiler.stop("x");
        profiler.start("y");
        profiler.stop("y");

        Map<String, Profiler.Stats> all = profiler.getAllStats();
        assertEquals(2, all.size());
        assertTrue(all.containsKey("x"));
        assertTrue(all.containsKey("y"));
    }

    @Test
    void averageMsConversion() {
        profiler.start("ms");
        // Ensure some time passes
        long end = System.nanoTime() + 1_000_000; // ~1ms
        while (System.nanoTime() < end) { /* spin */ }
        profiler.stop("ms");

        Profiler.Stats stats = profiler.getStats("ms");
        assertNotNull(stats);
        assertTrue(stats.getAverageMs() > 0.0);
    }
}
