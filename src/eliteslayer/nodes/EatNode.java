package eliteslayer.nodes;

import eliteslayer.ScriptContext;
import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.ScriptLogger;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.wrappers.items.Item;

/**
 * Eats food when HP falls below the eat threshold.
 */
public final class EatNode implements Node {

    /** HP percentage below which we eat. Configurable via Config.eatThreshold. */
    private final int eatThresholdPercent;
    private final ScriptLogger log;

    /** Static set of food name fragments — built once, not on every tick. */
    private static final java.util.Set<String> FOOD_NAMES;
    static {
        FOOD_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
            "shark", "anglerfish", "manta ray", "lobster", "swordfish", "tuna", "bass",
            "cake", "bread", "potato", "stew", "pie", "karambwan", "food"
        ));
    }

    public EatNode(ScriptContext ctx) {
        this.eatThresholdPercent = ctx.eatThreshold;
        this.log = new ScriptLogger("EatNode");
    }

    @Override
    public Status tick() {
        int baseHp = Skills.getLevel(Skill.HITPOINTS);
        int currHp = Skills.getBoostedLevel(Skill.HITPOINTS);

        if (baseHp == 0) return Status.FAILURE;

        int hpPercent = (currHp * 100) / baseHp;
        if (hpPercent > eatThresholdPercent) {
            return Status.FAILURE;   // HP fine — nothing to do
        }

        Telemetry.setState("EAT");

        // Look for any food in inventory using the static food name fragment set
        Item food = Inventory.get(item -> {
            if (item == null || item.getName() == null) return false;
            String lower = item.getName().toLowerCase();
            for (String frag : FOOD_NAMES) {
                if (lower.contains(frag)) return true;
            }
            return false;
        });

        if (food == null) {
            log.warn("No food found in inventory.");
            return Status.FAILURE;
        }

        Telemetry.setAction("Eating " + food.getName());
        log.info("Eating " + food.getName() + " at " + hpPercent + "% HP.");
        boolean ok = SleepUtil.retryInteract(food, "Eat", 3);
        if (ok) {
            org.dreambot.api.utilities.Sleep.sleep(300, 600);
        }
        return ok ? Status.SUCCESS : Status.FAILURE;
    }
}
