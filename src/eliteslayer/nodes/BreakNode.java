package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.BreakScheduler;
import eliteslayer.util.Telemetry;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

/**
 * Handles Poisson break scheduling.  When a break is due this node returns
 * RUNNING for the duration of the break; otherwise it returns FAILURE so the
 * rest of the tree can execute.
 */
public final class BreakNode implements Node {

    private final BreakScheduler scheduler;

    public BreakNode(BreakScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Status tick() {
        if (!scheduler.shouldBreak()) {
            return Status.FAILURE;   // no break needed — let other nodes run
        }

        long remaining = scheduler.breakTimeRemaining();
        Telemetry.setState("BREAK");
        Telemetry.setAction("Resting (" + (remaining / 60_000) + " min remaining)");
        Logger.log("[BreakNode] On break — " + (remaining / 1000) + "s remaining.");
        Sleep.sleep(2_000);   // yield for 2 s then re-check
        return Status.RUNNING;
    }
}
