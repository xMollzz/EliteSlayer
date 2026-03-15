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
    version = 1.0,
    description = "Modular OSRS Slayer bot with behaviour-tree AI.",
    category = Category.SLAYER
)
public final class EliteSlayer extends AbstractScript {

    // Systems
    private EntropyMonitor    entropy;
    private CrowdTracker      crowd;
    private AntiBanEngine     antiBan;
    private BreakScheduler    breaks;
    private StuckDetector     stuck;
    private DiscordWebhook    discord;
    private FileStateStore    state;
    private FatigueEngine     fatigue;
    private NaturalIdleSystem naturalIdle;
    private SocialAwareness   social;

    // Per-session behaviour profiles (assigned once at start)
    private PlayerProfile   profile;
    private CombatStrategy  combatStrategy;

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

        state   = new FileStateStore(new File(getDirectory(), playerName + "_state.cfg"));
        discord = new DiscordWebhook(gui.discordWebhook);

        // Initialise telemetry — reset counters then restore crash-resume values
        Telemetry.reset();
        Telemetry.setKillCount(state.getLong("kills", 0L));
        Telemetry.setGpLooted(state.getLong("gp", 0L));
        Telemetry.setSessionTasks(state.getLong("tasks", 0L));
        // Capture baseline XP for XP/hr calculation
        Telemetry.setStartXp(Skills.getTotalXP());

        entropy = new EntropyMonitor();
        crowd   = new CrowdTracker();
        antiBan = new AntiBanEngine(entropy);
        breaks  = new BreakScheduler();
        stuck   = new StuckDetector(this::stop);
        fatigue     = new FatigueEngine();
        naturalIdle = new NaturalIdleSystem();
        social      = new SocialAwareness();
        profile        = PlayerProfile.randomForSession();
        combatStrategy = CombatStrategy.randomForSession();
        hud     = new ScriptHUD(entropy, crowd, fatigue, profile, combatStrategy);

        MonsterDef monster = MonsterDatabase.get(gui.selectedMonster);
        int[]      mulePos = parseMulePos(gui);

        tree = new Selector(Arrays.asList(
            new SafetyNode(entropy, crowd),
            new BreakNode(breaks),
            new EatNode(gui.eatThreshold),
            new PotionNode(),
            new PrayerNode(gui.usePrayer, monster != null ? monster.protection : ""),
            new SpecialAttackNode(gui.specThreshold),
            new MuleNode(gui.useMule, gui.muleName, mulePos),
            new BankNode(monster, gui.foodAmount),
            new GENode(gui.useGE),
            new CannonNode(gui.useCannon, monster),
            new LootNode(profile, fatigue),
            new CombatNode(monster, combatStrategy, fatigue, profile)
        ));

        // Restore crash-resume counters
        discord.send("EliteSlayer started — targeting " + gui.selectedMonster
            + " | Profile: " + profile.name() + " | Strategy: " + combatStrategy.name());
        Logger.log("[EliteSlayer] Started on " + gui.selectedMonster
            + " | Profile: " + profile.name() + " | Strategy: " + combatStrategy.name());
    }

    @Override
    public int onLoop() {
        stuck.check();

        // Social awareness — react to nearby players
        if (social.react()) {
            return fatigue.adjustedDelay(400, 800, 80);
        }
        if (social.shouldWorldHop()) {
            social.hopWorld();
            return fatigue.adjustedDelay(600, 1200, 80);
        }

        // Natural idle simulation — probability-based fidgets
        if (naturalIdle.maybeFidget(profile.idleChanceMultiplier, fatigue.getCapped(5.0))) {
            return fatigue.adjustedDelay(400, 800, 80);
        }

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

        // Fatigue-adjusted loop delay
        return fatigue.adjustedDelay(500, 700, 60);
    }

    @Override
    public void onExit() {
        if (discord != null) {
            discord.send("EliteSlayer stopped — kills: " + Telemetry.getKillCount()
                + ", gp: " + Telemetry.getGpLooted());
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
            int x = Integer.parseInt(cfg.muleX);
            int y = Integer.parseInt(cfg.muleY);
            return new int[]{x, y, 0};
        } catch (NumberFormatException e) {
            return new int[]{3213, 3424, 0};
        }
    }
}
