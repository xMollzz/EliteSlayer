package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.CrowdTracker;
import eliteslayer.systems.EntropyMonitor;
import eliteslayer.systems.EnvironmentAnalyzer;
import eliteslayer.util.Telemetry;
import org.dreambot.api.utilities.Logger;

/**
 * Safety guard: fails (allowing the tree to continue) when everything is
 * normal, and returns RUNNING (halting the tree) when entropy is too low,
 * the area is suspiciously crowded, or the environment risk is elevated
 * (e.g. a J-Mod is nearby).
 */
public final class SafetyNode implements Node {

    private static final double ENTROPY_THRESHOLD = 0.15;   // below this → too repetitive
    private static final double HIGH_RISK_THRESHOLD = 0.7;  // above this → environment too risky

    private final EntropyMonitor     entropyMonitor;
    private final CrowdTracker       crowdTracker;
    private final EnvironmentAnalyzer environment;

    public SafetyNode(EntropyMonitor entropyMonitor, CrowdTracker crowdTracker,
                      EnvironmentAnalyzer environment) {
        this.entropyMonitor = entropyMonitor;
        this.crowdTracker   = crowdTracker;
        this.environment    = environment;
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

        if (environment.isModDetected()) {
            Logger.warn("[SafetyNode] J-Mod detected nearby — pausing.");
            Telemetry.setAction("J-Mod pause");
            return Status.RUNNING;
        }

        if (environment.getRiskLevel() >= HIGH_RISK_THRESHOLD) {
            Logger.warn("[SafetyNode] High risk environment ("
                + String.format("%.2f", environment.getRiskLevel()) + ") — pausing.");
            Telemetry.setAction("High-risk pause");
            return Status.RUNNING;
        }

        return Status.FAILURE;   // normal — let other nodes run
    }
}
