package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.systems.FatigueEngine;
import eliteslayer.systems.PlayerProfile;
import eliteslayer.util.PriceCache;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.grounditems.GroundItems;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.GroundItem;

import java.util.List;

/**
 * Loots ground items within 6 tiles that pass the value threshold.
 * Uses PriceCache to avoid repeat API calls (optimisation 2).
 * Target selection uses a single-pass for-loop (optimisation 11).
 */
public final class LootNode implements Node {

    private static final int DEFAULT_MIN_VALUE = 1_000;   // minimum gp value to loot
    private static final int LOOT_RANGE = 6;               // tiles

    private final int minValue;
    private final FatigueEngine fatigue;

    public LootNode(PlayerProfile profile, FatigueEngine fatigue) {
        this.minValue = profile.lootValueThreshold;
        this.fatigue  = fatigue;
    }

    /** Item IDs that should always be looted regardless of value. */
    private static final int[] ALWAYS_LOOT = {
        995,   // coins
        526,   // bones
        532,   // big bones
        4151,  // abyssal whip
        1163,  // rune full helm
        1201,  // rune kiteshield
        1305,  // rune scimitar
        1373,  // rune battleaxe
        1079,  // rune platelegs
        1127,  // rune platebody
        11840, // dragon boots
        2366,  // granite maul
        12006, // berserker ring
        6585,  // amulet of fury
        1215,  // dragon dagger
    };

    /** Item names that should always be looted (partial match). */
    private static final String[] ALWAYS_LOOT_NAMES = {
        "clue scroll", "ensouled head", "rune", "dragon", "crystal key", "half of a key"
    };

    @Override
    public Status tick() {
        if (Inventory.isFull()) return Status.FAILURE;

        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        Tile centre = local.getTile();
        List<GroundItem> items = GroundItems.all();

        GroundItem best  = null;
        int        bestVal = -1;

        // Single-pass max — no streams (optimisation 11)
        for (GroundItem gi : items) {
            if (gi == null) continue;
            if (gi.getTile().distance(centre) > LOOT_RANGE) continue;

            int val = getValue(gi);
            if (val > bestVal) {
                bestVal = val;
                best    = gi;
            }
        }

        if (best == null || bestVal < 0) return Status.FAILURE;

        Telemetry.setState("LOOT");
        Telemetry.setAction("Looting " + best.getName() + " (" + bestVal + " gp)");
        Logger.log("[LootNode] Looting " + best.getName());

        boolean ok = SleepUtil.retryInteract(best, "Take", 3);
        if (ok) {
            Telemetry.addGp(bestVal);
            // Fatigue-adjusted post-loot delay
            int delay = fatigue.adjustedDelay(400, 800, 40);
            org.dreambot.api.utilities.Sleep.sleep(delay);
        }
        return ok ? Status.SUCCESS : Status.FAILURE;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private int getValue(GroundItem gi) {
        int id = gi.getID();

        // Always-loot by ID
        for (int alwaysId : ALWAYS_LOOT) {
            if (alwaysId == id) return Integer.MAX_VALUE / 2;
        }

        // Always-loot by name
        String name = gi.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            for (String frag : ALWAYS_LOOT_NAMES) {
                if (lower.contains(frag)) return Integer.MAX_VALUE / 2;
            }
        }

        // Price-cached value check
        int price = PriceCache.getPrice(id) * gi.getAmount();
        return price >= minValue ? price : -1;
    }
}
