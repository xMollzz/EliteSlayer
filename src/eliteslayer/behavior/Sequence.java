package eliteslayer.behavior;

import java.util.List;

/**
 * Runs children in order; stops and returns FAILURE / RUNNING on the first
 * non-SUCCESS result. Returns SUCCESS only when every child succeeds.
 *
 * <p><b>RUNNING state propagation</b> — when a child returns RUNNING, its
 * index is cached so the next {@link #tick()} resumes directly from that
 * child instead of re-running already-succeeded siblings.  The cache is
 * invalidated whenever the running child returns a terminal status
 * (SUCCESS or FAILURE).</p>
 */
public final class Sequence implements Node {

    private final List<Node> children;

    /** Index of the child that returned RUNNING on the previous tick, or -1. */
    private int runningChild = -1;

    public Sequence(List<Node> children) {
        this.children = children;
    }

    @Override
    public Status tick() {
        int start = (runningChild >= 0) ? runningChild : 0;

        for (int i = start; i < children.size(); i++) {
            Node child = children.get(i);
            Status s;
            try {
                s = child.tick();
            } catch (Exception e) {
                org.dreambot.api.utilities.Logger.error("[EliteSlayer] Node " + child.getClass().getSimpleName() + " threw: " + e.getMessage());
                s = Status.FAILURE;
            }
            if (s == Status.RUNNING) {
                runningChild = i;
                return Status.RUNNING;
            }
            if (s == Status.FAILURE) {
                runningChild = -1;
                return Status.FAILURE;
            }
            // SUCCESS — continue to next child
        }
        runningChild = -1;
        return Status.SUCCESS;
    }
}
