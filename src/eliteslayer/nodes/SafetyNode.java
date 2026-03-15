package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.CrowdTracker;
import eliteslayer.systems.EntropyMonitor;
import eliteslayer.systems.PlayerThreatDetector;
import eliteslayer.util.Telemetry;
import org.dreambot.api.utilities.Logger;

/**
 * Safety guard: fails (allowing the tree to continue) when everything is
 * normal, and returns RUNNING (halting the tree) when entropy is too low,
 * the area is suspiciously crowded, or a player threat is detected.
 *
 * <h3>Improvements</h3>
 * <ul>
 *   <li><b>Threat detection</b> — pauses if the {@link PlayerThreatDetector}
 *       reports a moderator or suspicious observer nearby.</li>
 *   <li><b>Bot-risk score</b> — pauses when the composite entropy/diversity
 *       score indicates the bot's action pattern is too predictable.</li>
 * </ul>
 */
public final class SafetyNode implements Node {

    private static final double ENTROPY_THRESHOLD = 0.15;   // below this → too repetitive
    private static final double BOT_RISK_THRESHOLD = 0.75;  // above this → actions too patterned

    private final EntropyMonitor       entropyMonitor;
    private final CrowdTracker         crowdTracker;
    private final PlayerThreatDetector threatDetector;

    public SafetyNode(EntropyMonitor entropyMonitor,
                      CrowdTracker crowdTracker,
                      PlayerThreatDetector threatDetector) {
        this.entropyMonitor = entropyMonitor;
        this.crowdTracker   = crowdTracker;
        this.threatDetector = threatDetector;
    }

    @Override
    public Status tick() {
        Telemetry.setState("SAFETY_CHECK");
        crowdTracker.update();
        threatDetector.update();

        // Entropy check
        if (entropyMonitor.getNormalized() < ENTROPY_THRESHOLD) {
            Logger.warn("[SafetyNode] Entropy too low (" +
                String.format("%.2f", entropyMonitor.getNormalized()) +
                ") — pausing.");
            Telemetry.setAction("Low-entropy pause");
            return Status.RUNNING;
        }

        // Bot-risk score check
        double botRisk = entropyMonitor.getBotRiskScore();
        if (botRisk >= BOT_RISK_THRESHOLD) {
            Logger.warn("[SafetyNode] Bot-risk score too high (" +
                String.format("%.2f", botRisk) + ") — pausing.");
            Telemetry.setAction("High-risk pause");
            return Status.RUNNING;
        }

        // Threat detection
        if (threatDetector.shouldPause()) {
            Logger.warn("[SafetyNode] Threat detected (score "
                + threatDetector.getThreatScore() + ") — pausing.");
            Telemetry.setAction("Threat pause");
            Telemetry.addThreat();
            return Status.RUNNING;
        }

        // Crowd check
        if (crowdTracker.isCrowded()) {
            Logger.warn("[SafetyNode] Area is crowded (" +
                crowdTracker.getCurrentCrowd() + " players nearby) — pausing.");
            Telemetry.setAction("Crowd pause");
            return Status.RUNNING;
        }

        return Status.FAILURE;   // normal — let other nodes run
    }
}
