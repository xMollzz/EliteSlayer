package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.items.Item;

/**
 * Drinks combat stat potions when the boosted level drops below base.
 * Supports Attack, Strength, Defence, Ranged and Magic potions.
 */
public final class PotionNode implements Node {

    @Override
    public Status tick() {
        Telemetry.setState("POTION_CHECK");

        Item potion = findPotion();
        if (potion == null) return Status.FAILURE;

        Telemetry.setState("DRINK_POTION");
        Telemetry.setAction("Drinking " + potion.getName());
        Logger.log("[PotionNode] Drinking " + potion.getName());
        boolean ok = SleepUtil.retryInteract(potion, "Drink", 3);
        if (ok) org.dreambot.api.utilities.Sleep.sleep(400, 700);
        return ok ? Status.SUCCESS : Status.FAILURE;
    }

    private Item findPotion() {
        // Check if any combat boost has faded back to base
        if (needsAttackBoost())   return getPotion("attack potion", "super attack", "combat potion", "super combat");
        if (needsStrengthBoost()) return getPotion("strength potion", "super strength", "combat potion", "super combat");
        if (needsDefenceBoost())  return getPotion("defence potion", "super defence");
        if (needsRangedBoost())   return getPotion("ranging potion", "super ranging");
        if (needsMagicBoost())    return getPotion("magic potion", "imbued heart");
        return null;
    }

    private boolean needsAttackBoost() {
        return Skills.getBoostedLevel(Skill.ATTACK) <= Skills.getLevel(Skill.ATTACK);
    }

    private boolean needsStrengthBoost() {
        return Skills.getBoostedLevel(Skill.STRENGTH) <= Skills.getLevel(Skill.STRENGTH);
    }

    private boolean needsDefenceBoost() {
        return Skills.getBoostedLevel(Skill.DEFENCE) <= Skills.getLevel(Skill.DEFENCE);
    }

    private boolean needsRangedBoost() {
        return Skills.getBoostedLevel(Skill.RANGED) <= Skills.getLevel(Skill.RANGED);
    }

    private boolean needsMagicBoost() {
        return Skills.getBoostedLevel(Skill.MAGIC) <= Skills.getLevel(Skill.MAGIC);
    }

    private Item getPotion(String... nameFragments) {
        for (String fragment : nameFragments) {
            final String lowerFrag = fragment.toLowerCase();
            Item item = Inventory.get(i ->
                i != null && i.getName() != null &&
                i.getName().toLowerCase().contains(lowerFrag));
            if (item != null) return item;
        }
        return null;
    }
}
