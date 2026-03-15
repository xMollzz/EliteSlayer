package eliteslayer.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight profiler that tracks execution times for named sections.
 *
 * <p>Designed for single-threaded game-loop use but safe for concurrent reads
 * (e.g. from the paint thread).</p>
 *
 * <pre>{@code
 *   profiler.start("tree.tick");
 *   tree.tick();
 *   profiler.stop("tree.tick");
 *
 *   long avg = profiler.getAverageNs("tree.tick");
 * }</pre>
 */
public final class Profiler {

    /** Running statistics for a single named section. */
    public static final class Stats {
        private final AtomicLong totalNs = new AtomicLong(0);
        private final AtomicLong count   = new AtomicLong(0);
        private final AtomicLong maxNs   = new AtomicLong(0);
        private final AtomicLong minNs   = new AtomicLong(Long.MAX_VALUE);

        void record(long ns) {
            totalNs.addAndGet(ns);
            count.incrementAndGet();
            maxNs.accumulateAndGet(ns, Math::max);
            minNs.accumulateAndGet(ns, Math::min);
        }

        public long getTotalNs()   { return totalNs.get(); }
        public long getCount()     { return count.get(); }
        public long getMaxNs()     { return maxNs.get(); }
        public long getMinNs()     { long v = minNs.get(); return v == Long.MAX_VALUE ? 0 : v; }
        public long getAverageNs() { long c = count.get(); return c == 0 ? 0 : totalNs.get() / c; }
        public double getAverageMs() { return getAverageNs() / 1_000_000.0; }
        public double getMaxMs()     { return getMaxNs() / 1_000_000.0; }
    }

    private final Map<String, Stats>    statsMap   = new ConcurrentHashMap<>();
    /** Thread-local start times — avoids per-section Start object allocation. */
    private final Map<String, Long>     startTimes = new ConcurrentHashMap<>();

    /** Marks the start of a named section. */
    public void start(String section) {
        startTimes.put(section, System.nanoTime());
    }

    /** Marks the end of a named section and records the elapsed time. */
    public void stop(String section) {
        Long startNs = startTimes.remove(section);
        if (startNs == null) return;
        long elapsed = System.nanoTime() - startNs;
        statsMap.computeIfAbsent(section, k -> new Stats()).record(elapsed);
    }

    /** Returns the statistics for a named section, or {@code null} if not tracked. */
    public Stats getStats(String section) {
        return statsMap.get(section);
    }

    /** Returns all tracked sections and their statistics. */
    public Map<String, Stats> getAllStats() {
        return statsMap;
    }

    /** Resets all recorded statistics. */
    public void reset() {
        statsMap.clear();
        startTimes.clear();
    }
}
