package eliteslayer.behavior;

import java.util.List;

/**
 * Runs children in order; stops and returns FAILURE / RUNNING on the first
 * non-SUCCESS result. Returns SUCCESS only when every child succeeds.
 */
public final class Sequence implements Node {

    private final List<Node> children;

    public Sequence(List<Node> children) {
        this.children = children;
    }

    @Override
    public Status tick() {
        for (Node child : children) {
            Status s;
            try {
                s = child.tick();
            } catch (Exception e) {
                org.dreambot.api.utilities.Logger.error("[EliteSlayer] Node " + child.getClass().getSimpleName() + " threw: " + e.getMessage());
                s = Status.FAILURE;
            }
            if (s != Status.SUCCESS) {
                return s;
            }
        }
        return Status.SUCCESS;
    }
}
