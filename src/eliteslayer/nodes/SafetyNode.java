package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.CrowdTracker;
import eliteslayer.systems.EntropyMonitor;
import eliteslayer.util.Telemetry;
import org.dreambot.api.utilities.Logger;

/**
 * Safety guard: fails (allowing the tree to continue) when everything is
 * normal, and returns RUNNING (halting the tree) when entropy is too low or
 * the area is suspiciously crowded.
 */
public final class SafetyNode implements Node {

    private static final double ENTROPY_THRESHOLD = 0.15;   // below this → too repetitive

    private final EntropyMonitor entropyMonitor;
    private final CrowdTracker   crowdTracker;

    public SafetyNode(EntropyMonitor entropyMonitor, CrowdTracker crowdTracker) {
        this.entropyMonitor = entropyMonitor;
        this.crowdTracker   = crowdTracker;
    }

    @Override
    public Status tick() {
        Telemetry.setState("SAFETY_CHECK");
        crowdTracker.update();

        if (entropyMonitor.getNormalized() < ENTROPY_THRESHOLD) {
            Logger.warn("[SafetyNode] Entropy too low (" +
                String.format("%.2f", entropyMonitor.getNormalized()) +
                ") — pausing.");
            Telemetry.setAction("Low-entropy pause");
            return Status.RUNNING;   // pause the tree
        }

        if (crowdTracker.isCrowded()) {
            Logger.warn("[SafetyNode] Area is crowded (" +
                crowdTracker.getCurrentCrowd() + " players nearby) — pausing.");
            Telemetry.setAction("Crowd pause");
            return Status.RUNNING;
        }

        return Status.FAILURE;   // normal — let other nodes run
    }
}
