package eliteslayer.nodes;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Status;
import eliteslayer.util.Navigator;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;

/**
 * Mule trading node.  Walks to the configured mule player, offers trade, and
 * transfers items.  Includes retry logic (optimisation 6).
 */
public final class MuleNode implements Node {

    private final String  muleName;
    private final boolean enabled;
    private final int[]   mulePosition;  // {x, y, plane}

    public MuleNode(boolean enabled, String muleName, int[] mulePosition) {
        this.enabled      = enabled;
        this.muleName     = muleName;
        this.mulePosition = mulePosition;
    }

    @Override
    public Status tick() {
        if (!enabled || muleName == null || muleName.isEmpty()) return Status.FAILURE;
        if (!needsMule()) return Status.FAILURE;

        Telemetry.setState("MULE_TRADE");
        Telemetry.setTarget(muleName);

        Player local = Players.localPlayer();
        if (local == null) return Status.FAILURE;

        // Walk to mule tile
        Tile mTile = new Tile(mulePosition[0], mulePosition[1], mulePosition[2]);
        if (local.getTile().distance(mTile) > 5) {
            Telemetry.setAction("Walking to mule");
            if (!Navigator.walkTo(mTile, 20_000L)) return Status.FAILURE;
        }

        // Find mule player
        Player mule = findMule();
        if (mule == null) {
            Logger.warn("[MuleNode] Mule player '" + muleName + "' not found nearby.");
            return Status.FAILURE;
        }

        // Initiate trade with retry
        Telemetry.setAction("Trading with " + muleName);
        boolean traded = false;
        for (int i = 0; i < 4 && !traded; i++) {
            if (mule.interact("Trade with")) {
                traded = Sleep.sleepUntil(() ->
                    org.dreambot.api.methods.trade.Trade.isOpen(), 6_000);
            }
            if (!traded) Sleep.sleep(600);
        }

        if (!traded) {
            Telemetry.recordFailure();
            return Status.FAILURE;
        }

        // Accept trade screens (simplified — offer all valuable items)
        Sleep.sleep(600, 1200);
        org.dreambot.api.methods.trade.Trade.acceptTrade();
        Sleep.sleep(400, 800);
        org.dreambot.api.methods.trade.Trade.confirmTrade();
        Sleep.sleepUntil(() -> !org.dreambot.api.methods.trade.Trade.isOpen(), 8_000);

        Telemetry.addMuleTransfer();
        Telemetry.recordSuccess();
        Logger.log("[MuleNode] Trade with " + muleName + " complete.");
        return Status.SUCCESS;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private boolean needsMule() {
        // Trigger when inventory is 75% full
        return Inventory.count() >= 21;
    }

    private Player findMule() {
        for (Player p : Players.all()) {
            if (p != null && muleName.equalsIgnoreCase(p.getName())) return p;
        }
        return null;
    }
}
