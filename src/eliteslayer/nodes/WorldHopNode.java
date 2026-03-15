package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.CrowdTracker;
import eliteslayer.systems.PlayerThreatDetector;
import eliteslayer.systems.WorldHopSystem;
import eliteslayer.util.Telemetry;
import org.dreambot.api.utilities.Logger;

/**
 * Behaviour-tree leaf node that triggers a world hop when the area is too
 * crowded or the threat detector signals danger.
 *
 * <p>Placement in the tree: after SafetyNode but before combat nodes so
 * that the bot hops worlds rather than sitting idle in a dangerous area.</p>
 *
 * <h3>Hop conditions (any one triggers a hop)</h3>
 * <ul>
 *   <li>Threat score ≥ {@link PlayerThreatDetector#HOP_THRESHOLD}</li>
 *   <li>Crowd level sustained above threshold for &gt;60 s</li>
 * </ul>
 */
public final class WorldHopNode implements Node {

    /** Minimum sustained crowd duration (ms) before hopping. */
    private static final long CROWD_SUSTAIN_MS = 60_000L;

    private final CrowdTracker         crowd;
    private final PlayerThreatDetector threats;
    private final WorldHopSystem       hopSystem;

    /** Timestamp when the crowd first exceeded the threshold, 0 = not crowded. */
    private long crowdSinceMs = 0L;

    public WorldHopNode(CrowdTracker crowd,
                        PlayerThreatDetector threats,
                        WorldHopSystem hopSystem) {
        this.crowd     = crowd;
        this.threats   = threats;
        this.hopSystem = hopSystem;
    }

    @Override
    public Status tick() {
        // Condition 1: threat-based hop
        if (threats.shouldHop() && hopSystem.canHop()) {
            Logger.warn("[WorldHopNode] High threat (" + threats.getThreatScore()
                + ") — hopping worlds.");
            Telemetry.setState("WORLD_HOP");
            Telemetry.setAction("Threat-based hop");
            if (hopSystem.hop()) {
                Telemetry.addWorldHop();
                crowdSinceMs = 0L;
                return Status.SUCCESS;
            }
            return Status.RUNNING;
        }

        // Condition 2: sustained crowd-based hop
        if (crowd.isCrowded()) {
            long now = System.currentTimeMillis();
            if (crowdSinceMs == 0L) {
                crowdSinceMs = now;
            } else if (now - crowdSinceMs >= CROWD_SUSTAIN_MS && hopSystem.canHop()) {
                Logger.warn("[WorldHopNode] Sustained crowd (" + crowd.getCurrentCrowd()
                    + " players for " + ((now - crowdSinceMs) / 1000) + "s) — hopping.");
                Telemetry.setState("WORLD_HOP");
                Telemetry.setAction("Crowd-based hop");
                if (hopSystem.hop()) {
                    Telemetry.addWorldHop();
                    crowdSinceMs = 0L;
                    return Status.SUCCESS;
                }
                return Status.RUNNING;
            }
        } else {
            crowdSinceMs = 0L;
        }

        return Status.FAILURE;   // no hop needed — let other nodes run
    }
}
