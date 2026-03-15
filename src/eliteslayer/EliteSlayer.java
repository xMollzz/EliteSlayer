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
import org.dreambot.api.utilities.Logger;

import java.awt.*;
import java.io.File;
import java.util.Arrays;

@ScriptManifest(
    name    = "EliteSlayer",
    author  = "xMollzz",
    version = 2.0,
    description = "Modular OSRS Slayer bot with behaviour-tree AI, "
        + "human reaction model, dynamic mouse, threat detection, "
        + "world hopping, and task learning.",
    category = Category.SLAYER
)
public final class EliteSlayer extends AbstractScript {

    // Core systems
    private EntropyMonitor       entropy;
    private CrowdTracker         crowd;
    private AntiBanEngine        antiBan;
    private BreakScheduler       breaks;
    private StuckDetector        stuck;
    private DiscordWebhook       discord;
    private FileStateStore       state;

    // NEW systems
    private HumanReactionEngine  reactions;
    private PlayerThreatDetector threats;
    private WorldHopSystem       worldHop;
    private DynamicMouseBehavior mouseBehavior;
    private DynamicTaskLearner   learner;

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
        while (!gui.isStarted()) {
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }

        // Sanitize player name for file paths (optimisation 17)
        org.dreambot.api.wrappers.interactive.Player localPlayer =
            org.dreambot.api.methods.interactive.Players.localPlayer();
        String playerName = localPlayer != null && localPlayer.getName() != null
            ? localPlayer.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
            : "unknown";

        state   = new FileStateStore(new File(getDirectory(), playerName + "_state.cfg"));
        discord = new DiscordWebhook(gui.getDiscordWebhook());

        // Initialise telemetry — reset counters then restore crash-resume values
        Telemetry.reset();
        Telemetry.setKillCount(state.getLong("kills", 0L));
        Telemetry.setGpLooted(state.getLong("gp", 0L));
        Telemetry.setSessionTasks(state.getLong("tasks", 0L));
        // Capture baseline XP for XP/hr calculation
        Telemetry.setStartXp(Skills.getTotalXP());

        // Core systems
        entropy       = new EntropyMonitor();
        crowd         = new CrowdTracker();

        // NEW systems
        reactions     = new HumanReactionEngine();
        mouseBehavior = new DynamicMouseBehavior();
        threats       = new PlayerThreatDetector();
        worldHop      = new WorldHopSystem();
        learner       = new DynamicTaskLearner();

        antiBan       = new AntiBanEngine(entropy, reactions, mouseBehavior);
        breaks        = new BreakScheduler();
        stuck         = new StuckDetector(this::stop);
        hud           = new ScriptHUD(entropy, crowd, threats);

        MonsterDef monster = MonsterDatabase.get(gui.getSelectedMonster());
        int[]      mulePos = parseMulePos(gui);

        tree = new Selector(Arrays.asList(
            new SafetyNode(entropy, crowd, threats),
            new BreakNode(breaks),
            new WorldHopNode(crowd, threats, worldHop),
            new EatNode(gui.getEatThreshold()),
            new PotionNode(),
            new PrayerNode(gui.isUsePrayer(), monster != null ? monster.protection : ""),
            new SpecialAttackNode(gui.getSpecThreshold()),
            new MuleNode(gui.isUseMule(), gui.getMuleName(), mulePos),
            new BankNode(monster, gui.getFoodAmount()),
            new GENode(gui.isUseGE()),
            new CannonNode(gui.isUseCannon(), monster),
            new LootNode(),
            new CombatNode(monster, reactions, learner)
        ));

        // Restore crash-resume counters
        discord.send("EliteSlayer v2.0 started — targeting " + gui.getSelectedMonster());
        Logger.log("[EliteSlayer] Started on " + gui.getSelectedMonster()
            + " with HumanReaction, ThreatDetector, WorldHop, DynamicMouse, TaskLearner");
    }

    @Override
    public int onLoop() {
        stuck.check();

        // Anti-ban: run every 4–8 seconds (randomised)
        long now = System.currentTimeMillis();
        long antiBanInterval = 4_000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(4_000L);
        if (now - antiBanTick > antiBanInterval) {
            antiBan.act();
            antiBanTick = now;
        }

        // Script profiling (optimisation 10)
        long tickStart = System.nanoTime();
        try {
            tree.tick();
        } catch (Exception e) {
            Logger.error("[EliteSlayer] Uncaught exception in tree: " + e.getMessage());
        }
        long tickNs = System.nanoTime() - tickStart;
        if (tickNs > 5_000_000L) {  // > 5 ms
            Logger.warn("[EliteSlayer] Slow tick: " + (tickNs / 1_000_000L) + " ms");
        }

        return 600;
    }

    @Override
    public void onExit() {
        if (learner != null) learner.logSummary();
        if (discord != null) {
            discord.send("EliteSlayer stopped — kills: " + Telemetry.getKillCount()
                + ", gp: " + Telemetry.getGpLooted()
                + ", hops: " + Telemetry.getWorldHops());
            discord.shutdown();
        }
        if (state != null) {
            state.set("kills",    String.valueOf(Telemetry.getKillCount()));
            state.set("gp",       String.valueOf(Telemetry.getGpLooted()));
            state.set("tasks",    String.valueOf(Telemetry.getSessionTasks()));
            state.save();
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
            int x = Integer.parseInt(cfg.getMuleX());
            int y = Integer.parseInt(cfg.getMuleY());
            return new int[]{x, y, 0};
        } catch (NumberFormatException e) {
            return new int[]{3213, 3424, 0};
        }
    }
}
