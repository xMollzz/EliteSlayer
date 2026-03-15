package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.game.MonsterDef;
import eliteslayer.util.Navigator;
import eliteslayer.util.SleepUtil;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.bank.Bank;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.Player;

/**
 * Banking node — deposits loot, withdraws supplies, and returns to the fight
 * area.  Includes retry logic (optimisation 6).
 */
public final class BankNode implements Node {

    /** Item names to keep in inventory (supplies). */
    private static final String[] KEEP_ITEMS = {
        "Shark", "Anglerfish", "Manta ray", "shark", "anglerfish", "manta ray",
        "Prayer potion", "Super restore", "Saradomin brew",
        "Super attack", "Super strength", "Super defence", "Super combat",
        "Ranging potion", "Magic potion",
        "Antidote", "Antipoison",
        "Rune pouch", "Slayer bell",
        "Cannon ball", "Cannonball",
        "Coins"
    };

    private final MonsterDef monster;
    private final int        foodAmount;

    public BankNode(MonsterDef monster, int foodAmount) {
        this.monster    = monster;
        this.foodAmount = foodAmount;
    }

    @Override
    public Status tick() {
        if (monster == null) return Status.FAILURE;
        if (!needsBank()) return Status.FAILURE;

        Telemetry.setState("BANKING");

        // Walk to bank tile
        Tile bankTile = new Tile(monster.bankTile[0], monster.bankTile[1], monster.bankTile[2]);
        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        if (local.getTile().distance(bankTile) > 5) {
            Telemetry.setAction("Walking to bank");
            if (!Navigator.walkTo(bankTile, 20_000L)) return Status.FAILURE;
        }

        // Open bank with retry
        if (!Bank.isOpen()) {
            Telemetry.setAction("Opening bank");
            boolean opened = false;
            for (int i = 0; i < 4 && !opened; i++) {
                opened = Bank.open();
                if (!opened) Sleep.sleep(600);
            }
            if (!opened) { Telemetry.recordFailure(); return Status.FAILURE; }
            Sleep.sleepUntil(Bank::isOpen, 5_000);
        }

        if (!Bank.isOpen()) return Status.FAILURE;

        // Deposit all loot (non-supply items)
        Telemetry.setAction("Depositing loot");
        Bank.depositAllExcept(item -> {
            if (item == null || item.getName() == null) return false;
            for (String keep : KEEP_ITEMS) {
                if (item.getName().contains(keep)) return true;
            }
            return false;
        });
        Sleep.sleep(400, 800);

        // Withdraw food if low
        withdrawFood();

        Bank.close();
        Sleep.sleepUntil(() -> !Bank.isOpen(), 3_000);
        Telemetry.recordSuccess();

        // Walk back to fight area
        Tile fightTile = new Tile(monster.fightTile[0], monster.fightTile[1], monster.fightTile[2]);
        Telemetry.setAction("Walking back to fight area");
        Navigator.walkTo(fightTile, 20_000L);

        return Status.SUCCESS;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private boolean needsBank() {
        if (Inventory.isFull()) return true;
        // Sum food using name-based counts (DreamBot 3 has Inventory.count(String))
        int foodCount = Inventory.count("Shark")
                + Inventory.count("Anglerfish")
                + Inventory.count("Manta ray")
                + Inventory.count("Lobster")
                + Inventory.count("Swordfish")
                + Inventory.count("Karambwan");
        return foodCount < 2;
    }

    private void withdrawFood() {
        // Try common food types
        String[] foodNames = {"Shark", "Anglerfish", "Manta ray", "Lobster", "Swordfish"};
        for (String food : foodNames) {
            if (Bank.contains(food)) {
                int needed = foodAmount - Inventory.count(food);
                if (needed > 0) {
                    Bank.withdraw(food, needed);
                    Sleep.sleep(300, 600);
                    return;
                }
            }
        }
    }
}
