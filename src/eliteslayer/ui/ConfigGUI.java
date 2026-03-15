package eliteslayer.ui;

import eliteslayer.game.MonsterDatabase;
import eliteslayer.game.MonsterDef;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 4-tab configuration GUI (General, Combat, Supply, Muling).
 * Swing is single-threaded — all construction happens on the EDT.
 */
public final class ConfigGUI extends JFrame {

    // ------------------------------------------------------------------ //
    //  Shared config state (volatile for cross-thread visibility)         //
    // ------------------------------------------------------------------ //
    public volatile String  selectedMonster   = "Abyssal Demons";
    public volatile boolean useCannon         = false;
    public volatile boolean usePrayer         = false;
    public volatile boolean useGE             = false;
    public volatile boolean useMule           = false;
    public volatile String  muleName          = "";
    public volatile int     eatThreshold      = 50;
    public volatile int     specThreshold     = 50;
    public volatile int     foodAmount        = 16;
    public volatile int     potionAmount      = 4;
    public volatile String  discordWebhook    = "";
    public volatile String  muleX             = "3213";
    public volatile String  muleY             = "3424";
    public volatile boolean started           = false;

    // ------------------------------------------------------------------ //
    //  Swing controls                                                      //
    // ------------------------------------------------------------------ //
    private JComboBox<String> monsterCombo;
    private JCheckBox         cannonCheck;
    private JCheckBox         prayerCheck;
    private JCheckBox         geCheck;
    private JCheckBox         muleCheck;
    private JTextField        muleNameField;
    private JSlider           eatSlider;
    private JSlider           specSlider;
    private JSpinner          foodSpinner;
    private JSpinner          potionSpinner;
    private JTextField        webhookField;
    private JTextField        muleXField;
    private JTextField        muleYField;
    private JButton           startButton;

    public ConfigGUI() {
        super("EliteSlayer Configuration");
        build();
    }

    public void showGUI() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
        });
    }

    // ------------------------------------------------------------------ //
    //  Construction                                                        //
    // ------------------------------------------------------------------ //

    private void build() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General",  buildGeneralTab());
        tabs.addTab("Combat",   buildCombatTab());
        tabs.addTab("Supply",   buildSupplyTab());
        tabs.addTab("Muling",   buildMulingTab());
        add(tabs, BorderLayout.CENTER);

        startButton = new JButton("Start Script");
        startButton.setBackground(new Color(60, 180, 75));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 14f));
        startButton.addActionListener(e -> onStart());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(startButton);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel buildGeneralTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("General Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Target Monster:"), c);
        c.gridx = 1;
        monsterCombo = new JComboBox<>(MonsterDatabase.names());
        monsterCombo.setSelectedItem(selectedMonster);
        monsterCombo.addActionListener(e -> {
            selectedMonster = (String) monsterCombo.getSelectedItem();
            applyMonsterDefaults();
        });
        p.add(monsterCombo, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Discord Webhook URL:"), c);
        c.gridx = 1;
        webhookField = new JTextField(discordWebhook, 24);
        p.add(webhookField, c);

        return p;
    }

    private JPanel buildCombatTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Combat Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        cannonCheck = new JCheckBox("Use Cannon", useCannon);
        p.add(cannonCheck, c);

        c.gridx = 0; c.gridy = 1;
        prayerCheck = new JCheckBox("Use Protection Prayer", usePrayer);
        p.add(prayerCheck, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Eat below HP%:"), c);
        c.gridx = 1;
        eatSlider = new JSlider(10, 90, eatThreshold);
        eatSlider.setMajorTickSpacing(20);
        eatSlider.setPaintTicks(true);
        eatSlider.setPaintLabels(true);
        p.add(eatSlider, c);

        c.gridx = 0; c.gridy = 3;
        p.add(new JLabel("Special attack threshold%:"), c);
        c.gridx = 1;
        specSlider = new JSlider(25, 100, specThreshold);
        specSlider.setMajorTickSpacing(25);
        specSlider.setPaintTicks(true);
        specSlider.setPaintLabels(true);
        p.add(specSlider, c);

        return p;
    }

    private JPanel buildSupplyTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Supply Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        geCheck = new JCheckBox("Use Grand Exchange for restocking", useGE);
        p.add(geCheck, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Food per trip:"), c);
        c.gridx = 1;
        foodSpinner = new JSpinner(new SpinnerNumberModel(foodAmount, 1, 26, 1));
        p.add(foodSpinner, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Potions per trip:"), c);
        c.gridx = 1;
        potionSpinner = new JSpinner(new SpinnerNumberModel(potionAmount, 0, 10, 1));
        p.add(potionSpinner, c);

        return p;
    }

    private JPanel buildMulingTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Muling Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        muleCheck = new JCheckBox("Enable Mule Trading", useMule);
        p.add(muleCheck, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Mule username:"), c);
        c.gridx = 1;
        muleNameField = new JTextField(muleName, 16);
        p.add(muleNameField, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Mule tile X:"), c);
        c.gridx = 1;
        muleXField = new JTextField(muleX, 8);
        p.add(muleXField, c);

        c.gridx = 0; c.gridy = 3;
        p.add(new JLabel("Mule tile Y:"), c);
        c.gridx = 1;
        muleYField = new JTextField(muleY, 8);
        p.add(muleYField, c);

        return p;
    }

    // ------------------------------------------------------------------ //
    //  Event handlers                                                      //
    // ------------------------------------------------------------------ //

    private void onStart() {
        readValues();
        started = true;
        setVisible(false);
        dispose();
    }

    private void readValues() {
        selectedMonster = (String) monsterCombo.getSelectedItem();
        useCannon       = cannonCheck.isSelected();
        usePrayer       = prayerCheck.isSelected();
        useGE           = geCheck.isSelected();
        useMule         = muleCheck.isSelected();
        muleName        = muleNameField.getText().trim();
        eatThreshold    = eatSlider.getValue();
        specThreshold   = specSlider.getValue();
        foodAmount      = (int) foodSpinner.getValue();
        potionAmount    = (int) potionSpinner.getValue();
        discordWebhook  = webhookField.getText().trim();
        muleX           = muleXField.getText().trim();
        muleY           = muleYField.getText().trim();
    }

    private void applyMonsterDefaults() {
        MonsterDef def = MonsterDatabase.get(selectedMonster);
        if (def == null) return;
        if (cannonCheck != null) cannonCheck.setSelected(def.usesCannon);
        if (prayerCheck != null) prayerCheck.setSelected(def.usesPrayer);
    }
}
