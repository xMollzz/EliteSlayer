package eliteslayer.systems;

import org.dreambot.api.input.Mouse;
import org.dreambot.api.utilities.Sleep;

import java.awt.Point;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates natural-looking mouse movements using quadratic Bézier curves
 * with variable speed and occasional micro-corrections.
 *
 * <h3>Key behaviours</h3>
 * <ul>
 *   <li><b>Bézier curves</b> — the cursor follows a smooth arc rather than a
 *       straight line, mimicking the natural hand-wrist motion of a real
 *       player.</li>
 *   <li><b>Variable speed</b> — acceleration and deceleration are applied so
 *       the cursor starts slowly, speeds up in the middle, and decelerates
 *       near the target.</li>
 *   <li><b>Overshoot &amp; correction</b> — occasionally the cursor slightly
 *       overshoots the target and "corrects", which is a common human
 *       micro-adjustment pattern.</li>
 *   <li><b>Idle drift</b> — when the player is waiting, the cursor may drift
 *       slowly in a small random arc, like an idle hand on a mouse.</li>
 * </ul>
 *
 * <p>All operations ultimately delegate to {@link Mouse#move(int, int)} so
 * DreamBot's built-in mouse handler still sees each intermediate point.</p>
 */
public final class DynamicMouseBehavior {

    // ------------------------------------------------------------------ //
    //  Tunables                                                            //
    // ------------------------------------------------------------------ //

    /** Number of intermediate steps in a Bézier move. */
    private static final int BEZIER_STEPS          = 18;
    /** Base delay between intermediate steps (ms). */
    private static final int STEP_DELAY_BASE_MS    = 6;
    /** Maximum random jitter added to each step delay (ms). */
    private static final int STEP_DELAY_JITTER_MS  = 8;
    /** Probability (0-1) that an overshoot-then-correct manoeuvre occurs. */
    private static final double OVERSHOOT_CHANCE   = 0.12;
    /** Maximum overshoot distance (pixels). */
    private static final int OVERSHOOT_MAX_PX      = 12;
    /** Probability of a small idle drift when waiting. */
    private static final double DRIFT_CHANCE       = 0.20;
    /** Maximum drift distance (pixels). */
    private static final int DRIFT_MAX_PX          = 30;

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Moves the mouse to ({@code targetX}, {@code targetY}) along a Bézier
     * curve with variable speed.
     */
    public void moveTo(int targetX, int targetY) {
        Point start = Mouse.getPosition();
        if (start == null) {
            Mouse.move(targetX, targetY);
            return;
        }

        int sx = start.x;
        int sy = start.y;

        // Random control point for quadratic Bézier
        int cpX = (sx + targetX) / 2 + randomOffset(60);
        int cpY = (sy + targetY) / 2 + randomOffset(60);

        for (int i = 1; i <= BEZIER_STEPS; i++) {
            double t = (double) i / BEZIER_STEPS;
            // Apply ease-in-out: t' = 3t² - 2t³
            double tEased = t * t * (3.0 - 2.0 * t);

            int x = quadBezier(sx, cpX, targetX, tEased);
            int y = quadBezier(sy, cpY, targetY, tEased);

            Mouse.move(x, y);

            int delay = STEP_DELAY_BASE_MS
                + ThreadLocalRandom.current().nextInt(STEP_DELAY_JITTER_MS);
            Sleep.sleep(delay);
        }

        // Overshoot + correction
        if (ThreadLocalRandom.current().nextDouble() < OVERSHOOT_CHANCE) {
            int ox = targetX + randomOffset(OVERSHOOT_MAX_PX);
            int oy = targetY + randomOffset(OVERSHOOT_MAX_PX);
            Mouse.move(ox, oy);
            Sleep.sleep(40 + ThreadLocalRandom.current().nextInt(60));
            Mouse.move(targetX, targetY);
        }
    }

    /**
     * Performs a small idle drift — call this during waiting periods to add
     * subtle mouse movement.
     */
    public void idleDrift() {
        if (ThreadLocalRandom.current().nextDouble() > DRIFT_CHANCE) return;

        Point pos = Mouse.getPosition();
        if (pos == null) return;

        int dx = pos.x + randomOffset(DRIFT_MAX_PX);
        int dy = pos.y + randomOffset(DRIFT_MAX_PX);
        // Clamp to screen
        dx = Math.max(0, Math.min(760, dx));
        dy = Math.max(0, Math.min(500, dy));

        moveTo(dx, dy);
    }

    /**
     * Moves the mouse off-screen with a natural deceleration arc — used
     * during AFK pauses.
     */
    public void moveOffScreen() {
        Point pos = Mouse.getPosition();
        if (pos == null) { Mouse.move(-1, -1); return; }

        // Slide toward nearest edge
        int edgeX, edgeY;
        if (pos.x < 380) {
            edgeX = -5;
            edgeY = pos.y + randomOffset(40);
        } else {
            edgeX = 770;
            edgeY = pos.y + randomOffset(40);
        }

        moveTo(edgeX, edgeY);
        Mouse.move(-1, -1);
    }

    // ------------------------------------------------------------------ //
    //  Internal                                                            //
    // ------------------------------------------------------------------ //

    /** Evaluates a quadratic Bézier curve at parameter {@code t}. */
    private static int quadBezier(int p0, int p1, int p2, double t) {
        double u = 1.0 - t;
        return (int) (u * u * p0 + 2.0 * u * t * p1 + t * t * p2);
    }

    /** Returns a random integer in {@code [-max, +max]}. */
    private static int randomOffset(int max) {
        return ThreadLocalRandom.current().nextInt(-max, max + 1);
    }
}
