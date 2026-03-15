package eliteslayer.behavior;

import java.util.List;

/**
 * Tries children left-to-right; returns the first non-FAILURE result.
 * If every child returns FAILURE the Selector itself returns FAILURE.
 */
public final class Selector implements Node {

    private final List<Node> children;

    public Selector(List<Node> children) {
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
            if (s != Status.FAILURE) {
                return s;
            }
        }
        return Status.FAILURE;
    }
}
