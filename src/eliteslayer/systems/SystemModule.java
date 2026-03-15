package eliteslayer.systems;

/**
 * Abstract base for subsystems that should be polled periodically rather than
 * every game tick.
 *
 * <p>Each subclass specifies an {@code interval} (in milliseconds).  The
 * {@link #tick()} method checks whether enough time has elapsed and only
 * invokes the concrete {@link #update()} when the interval has passed.
 * This eliminates hundreds of redundant system calls per minute on large
 * scripts.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * public class MyCrowdTracker extends SystemModule {
 *     public MyCrowdTracker() { super(2000); }
 *     &#64;Override protected void update() { ... }
 * }
 * </pre>
 */
public abstract class SystemModule {

    /** Timestamp (epoch ms) at which the next update is due. */
    protected long nextUpdate;

    /** Minimum milliseconds between updates. */
    protected final int interval;

    /**
     * @param intervalMs minimum milliseconds between {@link #update()} calls
     */
    protected SystemModule(int intervalMs) {
        this.interval   = intervalMs;
        this.nextUpdate = 0L;   // ensure first tick triggers an update
    }

    /**
     * Called every game tick.  Delegates to {@link #update()} only when the
     * configured interval has elapsed.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now >= nextUpdate) {
            update();
            nextUpdate = now + interval;
        }
    }

    /**
     * Perform the actual system work.  Only called when the interval has
     * elapsed.
     */
    protected abstract void update();
}
