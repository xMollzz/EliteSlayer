package eliteslayer;

import eliteslayer.behavior.Node;
import eliteslayer.behavior.Selector;
import eliteslayer.game.MonsterDatabase;
import eliteslayer.game.MonsterDef;
import eliteslayer.nodes.*;
import eliteslayer.systems.*;
import eliteslayer.ui.ConfigGUI;
import eliteslayer.ui.ScriptHUD;
import eliteslayer.util.*;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;

import java.awt.*;
import java.io.File;
import java.util.Arrays;

@ScriptManifest(
    name    = "EliteSlayer",
    author  = "xMollzz",
    version = 1.0,
    description = "Modular OSRS Slayer bot with behaviour-tree AI.",
    category = Category.SLAYER
)
public final class EliteSlayer extends AbstractScript {

    // Shared context
    private ScriptContext ctx;

    // UI
    private ConfigGUI gui;
    private ScriptHUD hud;

    // Behavior tree root
    private Node tree;

    // Profiling
    private long antiBanTick = 0L;

    // ------------------------------------------------------------------ //
    //  Lifecycle                                                           //
    // ------------------------------------------------------------------ //

    @Override
    public void onStart() {
        gui = new ConfigGUI();
        gui.showGUI();
        // Block until GUI is closed / started
        while (!gui.started) {
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }

        // Sanitize player name for file paths (optimisation 17)
        org.dreambot.api.wrappers.interactive.Player localPlayer =
            org.dreambot.api.methods.interactive.Players.localPlayer();
        String playerName = localPlayer != null && localPlayer.getName() != null
            ? localPlayer.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
            : "unknown";

        FileStateStore state = new FileStateStore(new File(getDirectory(), playerName + "_state.cfg"));
        DiscordWebhook discord = new DiscordWebhook(gui.discordWebhook);

        // Initialise telemetry — reset counters then restore crash-resume values
        Telemetry.reset();
        Telemetry.setKillCount(state.getLong("kills", 0L));
        Telemetry.setGpLooted(state.getLong("gp", 0L));
        Telemetry.setSessionTasks(state.getLong("tasks", 0L));
        // Capture baseline XP for XP/hr calculation
        Telemetry.setStartXp(Skills.getTotalXP());

        // Restore GUI config from saved state (config serialization)
        gui.loadFrom(state);

        EntropyMonitor entropy = new EntropyMonitor();
        CrowdTracker   crowd   = new CrowdTracker();
        AntiBanEngine  antiBan = new AntiBanEngine(entropy);
        BreakScheduler breaks  = new BreakScheduler();
        StuckDetector  stuck   = new StuckDetector(this::stop);
        EventBus       eventBus = new EventBus();
        Profiler       profiler = new Profiler();
        ScriptLogger   logger  = new ScriptLogger("EliteSlayer");

        MonsterDef monster = MonsterDatabase.get(gui.selectedMonster);
        int[]      mulePos = parseMulePos(gui);

        ctx = new ScriptContext(
            entropy, crowd, antiBan, breaks, stuck,
            eventBus, profiler, discord, state, logger,
            monster, gui.useCannon, gui.usePrayer, gui.useGE,
            gui.useMule, gui.muleName, mulePos,
            gui.eatThreshold, gui.specThreshold,
            gui.foodAmount, gui.potionAmount,
            monster != null ? monster.protection : ""
        );

        hud = new ScriptHUD(entropy, crowd);

        tree = new Selector(Arrays.asList(
            new SafetyNode(ctx),
            new BreakNode(ctx),
            new EatNode(ctx),
            new PotionNode(ctx),
            new PrayerNode(ctx),
            new SpecialAttackNode(ctx),
            new MuleNode(ctx),
            new BankNode(ctx),
            new GENode(ctx),
            new CannonNode(ctx),
            new LootNode(ctx),
            new CombatNode(ctx)
        ));

        // Restore crash-resume counters
        discord.send("EliteSlayer started — targeting " + gui.selectedMonster);
        logger.info("Started on " + gui.selectedMonster);
    }

    @Override
    public int onLoop() {
        ctx.stuck.check();

        // Anti-ban: run every 4–8 seconds (randomised)
        long now = System.currentTimeMillis();
        long antiBanInterval = 4_000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(4_000L);
        if (now - antiBanTick > antiBanInterval) {
            ctx.antiBan.act();
            antiBanTick = now;
        }

        // Script profiling — track per-tick duration
        long tickStart = System.nanoTime();
        ctx.profiler.start("tree.tick");
        try {
            tree.tick();
        } catch (Exception e) {
            ctx.logger.error("Uncaught exception in tree: " + e.getMessage());
        }
        ctx.profiler.stop("tree.tick");
        long tickNs = System.nanoTime() - tickStart;

        if (tickNs > 5_000_000L) {  // > 5 ms
            ctx.logger.warn("Slow tick: " + (tickNs / 1_000_000L) + " ms");
        }

        return 600;
    }

    @Override
    public void onExit() {
        if (ctx != null && ctx.discord != null) {
            ctx.discord.send("EliteSlayer stopped — kills: " + Telemetry.getKillCount()
                + ", gp: " + Telemetry.getGpLooted());
            ctx.discord.shutdown();
        }
        if (ctx != null && ctx.state != null) {
            ctx.state.set("kills",    String.valueOf(Telemetry.getKillCount()));
            ctx.state.set("gp",       String.valueOf(Telemetry.getGpLooted()));
            ctx.state.set("tasks",    String.valueOf(Telemetry.getSessionTasks()));
            // Save GUI config for next session (config serialization)
            if (gui != null) gui.saveTo(ctx.state);
            ctx.state.save();
        }
    }

    @Override
    public void onPaint(Graphics g) {
        if (hud != null) hud.render(g);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private int[] parseMulePos(ConfigGUI cfg) {
        try {
            int x = Integer.parseInt(cfg.muleX);
            int y = Integer.parseInt(cfg.muleY);
            return new int[]{x, y, 0};
        } catch (NumberFormatException e) {
            return new int[]{3213, 3424, 0};
        }
    }
}
