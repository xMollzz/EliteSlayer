package eliteslayer.nodes;

import eliteslayer.ScriptContext;
import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.ScriptLogger;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.combat.Combat;

/**
 * Uses the special attack when the energy bar is at or above the configured
 * threshold.
 */
public final class SpecialAttackNode implements Node {

    private final int threshold;   // percentage, e.g. 50
    private final ScriptLogger log;

    public SpecialAttackNode(ScriptContext ctx) {
        this.threshold = ctx.specThreshold;
        this.log       = new ScriptLogger("SpecNode");
    }

    @Override
    public Status tick() {
        int energy = Combat.getSpecialAttackPercentage();
        if (energy < threshold) return Status.FAILURE;

        Telemetry.setState("SPEC");
        Telemetry.setAction("Special attack (" + energy + "%)");
        log.info("Activating special attack at " + energy + "%.");
        Combat.toggleSpecialAttack(true);
        org.dreambot.api.utilities.Sleep.sleep(400, 800);
        return Status.SUCCESS;
    }
}
