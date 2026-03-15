package eliteslayer.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight telemetry tracker.  All fields are thread-safe — the paint
 * thread reads them without synchronisation overhead.
 */
public final class Telemetry {

    private static final AtomicReference<String> currentState  = new AtomicReference<>("IDLE");
    private static final AtomicReference<String> currentTarget = new AtomicReference<>("-");
    private static final AtomicReference<String> lastAction    = new AtomicReference<>("-");
    private static final AtomicInteger successCount  = new AtomicInteger(0);
    private static final AtomicInteger failCount     = new AtomicInteger(0);
    private static final AtomicInteger pathFailures  = new AtomicInteger(0);
    private static final AtomicLong    killCount     = new AtomicLong(0);
    private static final AtomicLong    gpLooted      = new AtomicLong(0);
    private static final AtomicLong    sessionTasks  = new AtomicLong(0);
    private static final AtomicLong    geRestocks    = new AtomicLong(0);
    private static final AtomicLong    muleTransfers = new AtomicLong(0);
    /** Starting XP for this session (set once in onStart). */
    private static final AtomicLong    startXp       = new AtomicLong(0);
    /** Script start time for GP/hr and kill-rate calculation. */
    private static final AtomicLong    startTime     = new AtomicLong(System.currentTimeMillis());

    private Telemetry() {}

    public static void setState(String state)  { currentState.set(state); }
    public static void setTarget(String target) { currentTarget.set(target); }
    public static void setAction(String action) { lastAction.set(action); }

    public static void recordSuccess()  { successCount.incrementAndGet(); }
    public static void recordFailure()  { failCount.incrementAndGet(); }
    public static void recordPathFail() { pathFailures.incrementAndGet(); }
    public static void addKill()        { killCount.incrementAndGet(); }
    public static void addGp(long gp)   { gpLooted.addAndGet(gp); }
    public static void addTask()        { sessionTasks.incrementAndGet(); }
    public static void addGERestock()   { geRestocks.incrementAndGet(); }
    public static void addMuleTransfer(){ muleTransfers.incrementAndGet(); }

    public static String  getState()         { return currentState.get(); }
    public static String  getTarget()        { return currentTarget.get(); }
    public static String  getAction()        { return lastAction.get(); }
    public static int     getSuccessCount()  { return successCount.get(); }
    public static int     getFailCount()     { return failCount.get(); }
    public static int     getPathFailures()  { return pathFailures.get(); }
    public static long    getKillCount()     { return killCount.get(); }
    public static long    getGpLooted()      { return gpLooted.get(); }
    public static long    getSessionTasks()  { return sessionTasks.get(); }
    public static long    getGERestocks()    { return geRestocks.get(); }
    public static long    getMuleTransfers() { return muleTransfers.get(); }
    public static long    getStartTime()     { return startTime.get(); }

    /** Success-rate percentage (0-100). */
    public static int getSuccessRate() {
        int total = successCount.get() + failCount.get();
        if (total == 0) return 100;
        return (int) (successCount.get() * 100L / total);
    }

    /** GP earned per hour based on elapsed session time. */
    public static long getGpPerHour() {
        long elapsed = System.currentTimeMillis() - startTime.get();
        if (elapsed < 5_000L) return 0L;
        return gpLooted.get() * 3_600_000L / elapsed;
    }

    /** Kills per hour based on elapsed session time. */
    public static long getKillsPerHour() {
        long elapsed = System.currentTimeMillis() - startTime.get();
        if (elapsed < 5_000L) return 0L;
        return killCount.get() * 3_600_000L / elapsed;
    }

    // ------------------------------------------------------------------ //
    //  Setters for crash-resume loading                                    //
    // ------------------------------------------------------------------ //

    public static void setKillCount(long v)   { killCount.set(v); }
    public static void setGpLooted(long v)    { gpLooted.set(v); }
    public static void setSessionTasks(long v){ sessionTasks.set(v); }
    public static void setStartXp(long xp)    { startXp.set(xp); }

    public static void reset() {
        currentState.set("IDLE");
        currentTarget.set("-");
        lastAction.set("-");
        successCount.set(0);
        failCount.set(0);
        pathFailures.set(0);
        killCount.set(0);
        gpLooted.set(0);
        sessionTasks.set(0);
        geRestocks.set(0);
        muleTransfers.set(0);
        startXp.set(0);
        startTime.set(System.currentTimeMillis());
    }
}
