package eliteslayer.behavior;

import java.util.List;

/**
 * Runs children in order; stops and returns FAILURE / RUNNING on the first
 * non-SUCCESS result. Returns SUCCESS only when every child succeeds.
 *
 * <p><b>Running-node memory</b> — when a child returns {@link Status#RUNNING},
 * its index is cached so the next tick resumes from that child instead of
 * re-evaluating all preceding siblings.  The cache is cleared when the
 * sequence completes or a child fails.</p>
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
        int start = runningChild >= 0 ? runningChild : 0;

        for (int i = start; i < children.size(); i++) {
            Status s;
            try {
                s = children.get(i).tick();
            } catch (Exception e) {
                org.dreambot.api.utilities.Logger.error("[EliteSlayer] Node " + children.get(i).getClass().getSimpleName() + " threw: " + e.getMessage());
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
        }
        runningChild = -1;
        return Status.SUCCESS;
    }
}
