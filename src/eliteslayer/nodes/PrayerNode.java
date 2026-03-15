package eliteslayer.nodes;

import eliteslayer.ScriptContext;
import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.ScriptLogger;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.prayer.Prayer;
import org.dreambot.api.methods.prayer.Prayers;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;

/**
 * Activates the configured protection prayer when the player has prayer points
 * and the prayer is not already active.  Deactivates it when points are
 * depleted.
 */
public final class PrayerNode implements Node {

    private final String prayerName;   // e.g. "PROTECT_FROM_MELEE"
    private final boolean usePrayer;
    private final ScriptLogger log;

    public PrayerNode(ScriptContext ctx) {
        this.usePrayer  = ctx.usePrayer;
        this.prayerName = ctx.protectionPrayer != null ? ctx.protectionPrayer : "";
        this.log        = new ScriptLogger("PrayerNode");
    }

    @Override
    public Status tick() {
        if (!usePrayer || prayerName.isEmpty()) return Status.FAILURE;

        Telemetry.setState("PRAYER");
        int pp = Skills.getBoostedLevel(Skill.PRAYER);

        if (pp <= 0) {
            // No prayer points — deactivate to avoid wasting
            deactivateAll();
            return Status.FAILURE;
        }

        Prayer target = resolvePrayer(prayerName);
        if (target == null) return Status.FAILURE;

        if (Prayers.isActive(target)) return Status.FAILURE;   // already on

        Telemetry.setAction("Activating " + target.name());
        log.info("Activating " + target.name());
        Prayers.toggle(target);
        org.dreambot.api.utilities.Sleep.sleep(300, 600);
        return Status.SUCCESS;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private void deactivateAll() {
        for (Prayer p : Prayer.values()) {
            if (Prayers.isActive(p)) {
                Prayers.toggle(p);
            }
        }
    }

    private Prayer resolvePrayer(String name) {
        try {
            return Prayer.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try a case-insensitive partial match
            for (Prayer p : Prayer.values()) {
                if (p.name().toLowerCase().contains(name.toLowerCase())) return p;
            }
            log.warn("Unknown prayer: " + name);
            return null;
        }
    }
}
