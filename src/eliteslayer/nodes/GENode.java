package eliteslayer.nodes;

import eliteslayer.ScriptContext;
import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.PriceCache;
import eliteslayer.util.ScriptLogger;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.bank.Bank;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.utilities.Sleep;

/**
 * Grand Exchange node: sells loot and restocks supplies.
 * Uses PriceCache for all price lookups (optimisation 2).
 */
public final class GENode implements Node {

    /** Item IDs to sell at the GE (valuable drops). */
    private static final int[] SELL_ITEMS = {
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

    /** Supplies to buy (name, id, quantity). */
    private static final Object[][] RESTOCK_LIST = {
        {"Shark",             385,  20},
        {"Prayer potion(4)", 2434,   4},
        {"Super attack(4)",  2436,   2},
        {"Super strength(4)",2440,   2},
        {"Super defence(4)", 2442,   2},
        {"Cannonball",        2,   500},
    };

    private final boolean enabled;
    private final ScriptLogger log;

    public GENode(ScriptContext ctx) {
        this.enabled = ctx.useGE;
        this.log     = new ScriptLogger("GENode");
    }

    @Override
    public Status tick() {
        if (!enabled) return Status.FAILURE;
        if (!needsRestock() && !hasItemsToSell()) return Status.FAILURE;

        Telemetry.setState("GRAND_EXCHANGE");

        // Open GE
        if (!GrandExchange.isOpen()) {
            GrandExchange.open();
            Sleep.sleepUntil(GrandExchange::isOpen, 8_000);
        }
        if (!GrandExchange.isOpen()) {
            Telemetry.recordFailure();
            return Status.FAILURE;
        }

        // Sell loot
        if (hasItemsToSell()) {
            Telemetry.setAction("Selling loot at GE");
            sellLoot();
        }

        // Restock supplies
        if (needsRestock()) {
            Telemetry.setAction("Restocking supplies at GE");
            restockSupplies();
        }

        GrandExchange.close();
        Telemetry.addGERestock();
        Telemetry.recordSuccess();
        return Status.SUCCESS;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private boolean hasItemsToSell() {
        if (!Bank.isOpen() && !Bank.open()) return false;
        for (int id : SELL_ITEMS) {
            if (Bank.contains(id)) { Bank.close(); return true; }
        }
        Bank.close();
        return false;
    }

    private boolean needsRestock() {
        if (!Bank.isOpen() && !Bank.open()) return false;
        for (Object[] entry : RESTOCK_LIST) {
            int id  = (int) entry[1];
            int qty = (int) entry[2];
            if (Bank.count(id) < qty / 2) { Bank.close(); return true; }
        }
        Bank.close();
        return false;
    }

    private void sellLoot() {
        for (int id : SELL_ITEMS) {
            if (!Bank.isOpen()) Bank.open();
            int qty = (int) Bank.count(id);
            if (qty <= 0) continue;
            Bank.withdraw(id, qty);
            Sleep.sleep(400, 800);
            if (!GrandExchange.isOpen()) { GrandExchange.open(); Sleep.sleepUntil(GrandExchange::isOpen, 5_000); }
            int price = (int)(PriceCache.getPrice(id) * 0.95);
            GrandExchange.sellItem(id, qty, price);
            Sleep.sleep(600, 1000);
        }
    }

    private void restockSupplies() {
        for (Object[] entry : RESTOCK_LIST) {
            String name = (String) entry[0];
            int    id   = (int)    entry[1];
            int    qty  = (int)    entry[2];

            if (!Bank.isOpen()) Bank.open();
            int have = (int) Bank.count(id);
            if (have >= qty / 2) continue;
            int needed = qty - have;

            if (!GrandExchange.isOpen()) { GrandExchange.open(); Sleep.sleepUntil(GrandExchange::isOpen, 5_000); }
            int price = (int)(PriceCache.getPrice(id) * 1.10);
            GrandExchange.buyItem(id, needed, price);
            log.info("Buying " + needed + "x " + name + " @ " + price + " gp");
            Sleep.sleep(600, 1000);
        }
    }
}
