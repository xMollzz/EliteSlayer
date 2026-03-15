package eliteslayer.behavior;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SequenceTest {

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
    void returnsSuccessWhenAllSucceed() {
        Sequence seq = new Sequence(Arrays.asList(
            fixed(Status.SUCCESS),
            fixed(Status.SUCCESS),
            fixed(Status.SUCCESS)
        ));
        assertEquals(Status.SUCCESS, seq.tick());
    }

    @Test
    void stopsAtFirstFailure() {
        SpyNode a = new SpyNode(Status.SUCCESS);
        SpyNode b = new SpyNode(Status.FAILURE);
        SpyNode c = new SpyNode(Status.SUCCESS);

        Sequence seq = new Sequence(Arrays.asList(a, b, c));
        Status result = seq.tick();

        assertEquals(Status.FAILURE, result);
        assertTrue(a.ticked);
        assertTrue(b.ticked);
        assertFalse(c.ticked, "Third child should NOT be ticked after FAILURE");
    }

    @Test
    void stopsAtRunning() {
        SpyNode a = new SpyNode(Status.SUCCESS);
        SpyNode b = new SpyNode(Status.RUNNING);
        SpyNode c = new SpyNode(Status.SUCCESS);

        Sequence seq = new Sequence(Arrays.asList(a, b, c));
        Status result = seq.tick();

        assertEquals(Status.RUNNING, result);
        assertFalse(c.ticked);
    }

    @Test
    void returnsSuccessForEmptyList() {
        Sequence seq = new Sequence(Collections.emptyList());
        assertEquals(Status.SUCCESS, seq.tick());
    }

    @Test
    void handlesExceptionAsFAILURE() {
        Node thrower = () -> { throw new RuntimeException("boom"); };
        SpyNode after = new SpyNode(Status.SUCCESS);

        Sequence seq = new Sequence(Arrays.asList(thrower, after));
        Status result = seq.tick();

        assertEquals(Status.FAILURE, result);
        assertFalse(after.ticked, "Node after exception should NOT be ticked in Sequence");
    }
}
