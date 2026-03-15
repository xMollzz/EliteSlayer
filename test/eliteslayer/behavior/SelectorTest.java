package eliteslayer.behavior;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SelectorTest {

    /** Helper node that always returns a fixed status. */
    private static Node fixed(Status status) {
        return () -> status;
    }

    /** Helper node that records whether it was called. */
    private static final class SpyNode implements Node {
        boolean ticked = false;
        final Status result;
        SpyNode(Status result) { this.result = result; }
        @Override public Status tick() { ticked = true; return result; }
    }

    @Test
    void returnsFirstSuccessSkippingRest() {
        SpyNode a = new SpyNode(Status.FAILURE);
        SpyNode b = new SpyNode(Status.SUCCESS);
        SpyNode c = new SpyNode(Status.FAILURE);

        Selector selector = new Selector(Arrays.asList(a, b, c));
        Status result = selector.tick();

        assertEquals(Status.SUCCESS, result);
        assertTrue(a.ticked, "First child should be ticked");
        assertTrue(b.ticked, "Second child should be ticked");
        assertFalse(c.ticked, "Third child should NOT be ticked after SUCCESS");
    }

    @Test
    void returnsRunningAndStops() {
        SpyNode a = new SpyNode(Status.FAILURE);
        SpyNode b = new SpyNode(Status.RUNNING);
        SpyNode c = new SpyNode(Status.SUCCESS);

        Selector selector = new Selector(Arrays.asList(a, b, c));
        Status result = selector.tick();

        assertEquals(Status.RUNNING, result);
        assertFalse(c.ticked, "Third child should NOT be ticked after RUNNING");
    }

    @Test
    void returnsFailureWhenAllFail() {
        Selector selector = new Selector(Arrays.asList(
            fixed(Status.FAILURE),
            fixed(Status.FAILURE),
            fixed(Status.FAILURE)
        ));
        assertEquals(Status.FAILURE, selector.tick());
    }

    @Test
    void returnsFailureForEmptyList() {
        Selector selector = new Selector(Collections.emptyList());
        assertEquals(Status.FAILURE, selector.tick());
    }

    @Test
    void handlesExceptionAsFAILURE() {
        Node thrower = () -> { throw new RuntimeException("boom"); };
        SpyNode after = new SpyNode(Status.SUCCESS);

        Selector selector = new Selector(Arrays.asList(thrower, after));
        Status result = selector.tick();

        assertEquals(Status.SUCCESS, result);
        assertTrue(after.ticked, "Node after exception should still be ticked");
    }
}
