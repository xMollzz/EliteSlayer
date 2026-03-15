package eliteslayer.ui;

import eliteslayer.systems.CrowdTracker;
import eliteslayer.systems.EntropyMonitor;
import eliteslayer.util.Telemetry;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import java.awt.*;

/**
 * Full HUD overlay with HP bar, entropy bar, crowd display and Telemetry
 * data (optimisation 5 display requirement).
 *
 * {@link #render(Graphics)} is called from the script's {@code onPaint}.
 */
public final class ScriptHUD {

    private static final Color BG_COLOR      = new Color(0, 0, 0, 160);
    private static final Color HEADER_COLOR  = new Color(255, 165, 0);
    private static final Color LABEL_COLOR   = new Color(200, 200, 200);
    private static final Color VALUE_COLOR   = Color.WHITE;
    private static final Color HP_HIGH       = new Color(50, 205, 50);
    private static final Color HP_MED        = new Color(255, 165, 0);
    private static final Color HP_LOW        = new Color(220, 50, 50);
    private static final Color ENTROPY_COLOR = new Color(100, 180, 255);
    private static final Color BAR_BG        = new Color(50, 50, 50);

    private static final int HUD_X     = 10;
    private static final int HUD_Y     = 10;
    private static final int HUD_W     = 230;
    private static final int LINE_H    = 16;
    private static final int BAR_H     = 10;
    private static final int BAR_W     = 180;
    private static final int PADDING   = 8;

    private final EntropyMonitor entropy;
    private final CrowdTracker   crowd;

    public ScriptHUD(EntropyMonitor entropy, CrowdTracker crowd) {
        this.entropy = entropy;
        this.crowd   = crowd;
    }

    /** Called from onPaint — renders the entire HUD. */
    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int lines  = 16;
        int height = PADDING * 2 + lines * LINE_H + BAR_H * 2 + 10;

        // Background
        g2.setColor(BG_COLOR);
        g2.fillRoundRect(HUD_X, HUD_Y, HUD_W, height, 12, 12);
        g2.setColor(new Color(255, 165, 0, 80));
        g2.drawRoundRect(HUD_X, HUD_Y, HUD_W, height, 12, 12);

        int x = HUD_X + PADDING;
        int y = HUD_Y + PADDING + 12;

        // Header
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(HEADER_COLOR);
        g2.drawString("\u2694 EliteSlayer", x, y);
        y += LINE_H + 2;

        g2.setFont(new Font("Arial", Font.PLAIN, 11));

        // Runtime (use Telemetry.getStartTime so it syncs with GP/hr calc)
        long elapsed = System.currentTimeMillis() - Telemetry.getStartTime();
        drawRow(g2, x, y, "Runtime:", formatTime(elapsed)); y += LINE_H;

        // State / target
        drawRow(g2, x, y, "State:",   Telemetry.getState());  y += LINE_H;
        drawRow(g2, x, y, "Target:",  Telemetry.getTarget()); y += LINE_H;
        drawRow(g2, x, y, "Action:",  Telemetry.getAction()); y += LINE_H;

        // Stats
        drawRow(g2, x, y, "Kills:",    Telemetry.getKillCount() + " (" + Telemetry.getKillsPerHour() + "/hr)"); y += LINE_H;
        drawRow(g2, x, y, "GP Loot:",  formatGp(Telemetry.getGpLooted()) + " (" + formatGp(Telemetry.getGpPerHour()) + "/hr)"); y += LINE_H;
        drawRow(g2, x, y, "Tasks:",    String.valueOf(Telemetry.getSessionTasks())); y += LINE_H;
        drawRow(g2, x, y, "GE Buys:",  String.valueOf(Telemetry.getGERestocks()));   y += LINE_H;
        drawRow(g2, x, y, "Mule Tx:",  String.valueOf(Telemetry.getMuleTransfers())); y += LINE_H;

        // Success rate
        drawRow(g2, x, y, "Success:", Telemetry.getSuccessRate() + "% ("
            + Telemetry.getSuccessCount() + "/" +
            (Telemetry.getSuccessCount() + Telemetry.getFailCount()) + ")"); y += LINE_H;

        // Path failures
        drawRow(g2, x, y, "PathFail:", String.valueOf(Telemetry.getPathFailures())); y += LINE_H;

        // Crowd
        drawRow(g2, x, y, "Crowd:",   crowd.getCurrentCrowd() + " nearby"); y += LINE_H + 4;

        // HP bar
        int hp    = Skills.getBoostedLevel(Skill.HITPOINTS);
        int maxHp = Math.max(1, Skills.getLevel(Skill.HITPOINTS));
        float hpFrac = (float) hp / maxHp;
        g2.setColor(LABEL_COLOR);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.drawString("HP: " + hp + "/" + maxHp, x, y); y += 12;
        drawBar(g2, x, y, BAR_W, BAR_H, hpFrac, hpFrac > 0.5f ? HP_HIGH : hpFrac > 0.25f ? HP_MED : HP_LOW);
        y += BAR_H + 4;

        // Entropy bar
        float ef = (float) entropy.getNormalized();
        g2.setColor(LABEL_COLOR);
        g2.drawString("Entropy: " + String.format("%.2f", ef), x, y); y += 12;
        drawBar(g2, x, y, BAR_W, BAR_H, ef, ENTROPY_COLOR);
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                    //
    // ------------------------------------------------------------------ //

    private void drawRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(LABEL_COLOR);
        g.drawString(label, x, y);
        g.setColor(VALUE_COLOR);
        g.drawString(value, x + 80, y);
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h, float fraction, Color fill) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        g.setColor(BAR_BG);
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(fill);
        g.fillRoundRect(x, y, (int)(w * fraction), h, 4, 4);
        g.setColor(Color.DARK_GRAY);
        g.drawRoundRect(x, y, w, h, 4, 4);
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    private static String formatGp(long gp) {
        if (gp >= 1_000_000) return String.format("%.1fM", gp / 1_000_000.0);
        if (gp >= 1_000)     return String.format("%.1fK", gp / 1_000.0);
        return String.valueOf(gp);
    }
}
