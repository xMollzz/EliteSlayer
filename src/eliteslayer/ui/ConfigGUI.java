package eliteslayer.ui;

import eliteslayer.game.MonsterDatabase;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 4-tab configuration GUI (General, Combat, Supply, Muling).
 * Swing is single-threaded — all construction happens on the EDT.
 *
 * <p>Configuration state lives in {@link ConfigModel} so that the game loop
 * never needs a reference to Swing classes.</p>
 */
public final class ConfigGUI extends JFrame {

    // ------------------------------------------------------------------ //
    //  Model (owns the config state)                                       //
    // ------------------------------------------------------------------ //
    private final ConfigModel model;

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
        this(new ConfigModel());
    }

    public ConfigGUI(ConfigModel model) {
        super("EliteSlayer Configuration");
        this.model = model;
        build();
    }

    /** Returns the underlying configuration model. */
    public ConfigModel getModel() { return model; }

    // Convenience accessors (delegate to model) so existing code compiles
    public String  getSelectedMonster() { return model.selectedMonster; }
    public boolean isUseCannon()        { return model.useCannon; }
    public boolean isUsePrayer()        { return model.usePrayer; }
    public boolean isUseGE()            { return model.useGE; }
    public boolean isUseMule()          { return model.useMule; }
    public String  getMuleName()        { return model.muleName; }
    public int     getEatThreshold()    { return model.eatThreshold; }
    public int     getSpecThreshold()   { return model.specThreshold; }
    public int     getFoodAmount()      { return model.foodAmount; }
    public int     getPotionAmount()    { return model.potionAmount; }
    public String  getDiscordWebhook()  { return model.discordWebhook; }
    public String  getMuleX()           { return model.muleX; }
    public String  getMuleY()           { return model.muleY; }
    public boolean isStarted()          { return model.started; }

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
        monsterCombo.setSelectedItem(model.selectedMonster);
        monsterCombo.addActionListener(e -> {
            model.selectedMonster = (String) monsterCombo.getSelectedItem();
            applyMonsterDefaults();
        });
        p.add(monsterCombo, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Discord Webhook URL:"), c);
        c.gridx = 1;
        webhookField = new JTextField(model.discordWebhook, 24);
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
        cannonCheck = new JCheckBox("Use Cannon", model.useCannon);
        p.add(cannonCheck, c);

        c.gridx = 0; c.gridy = 1;
        prayerCheck = new JCheckBox("Use Protection Prayer", model.usePrayer);
        p.add(prayerCheck, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Eat below HP%:"), c);
        c.gridx = 1;
        eatSlider = new JSlider(10, 90, model.eatThreshold);
        eatSlider.setMajorTickSpacing(20);
        eatSlider.setPaintTicks(true);
        eatSlider.setPaintLabels(true);
        p.add(eatSlider, c);

        c.gridx = 0; c.gridy = 3;
        p.add(new JLabel("Special attack threshold%:"), c);
        c.gridx = 1;
        specSlider = new JSlider(25, 100, model.specThreshold);
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
        geCheck = new JCheckBox("Use Grand Exchange for restocking", model.useGE);
        p.add(geCheck, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Food per trip:"), c);
        c.gridx = 1;
        foodSpinner = new JSpinner(new SpinnerNumberModel(model.foodAmount, 1, 26, 1));
        p.add(foodSpinner, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Potions per trip:"), c);
        c.gridx = 1;
        potionSpinner = new JSpinner(new SpinnerNumberModel(model.potionAmount, 0, 10, 1));
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
        muleCheck = new JCheckBox("Enable Mule Trading", model.useMule);
        p.add(muleCheck, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Mule username:"), c);
        c.gridx = 1;
        muleNameField = new JTextField(model.muleName, 16);
        p.add(muleNameField, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Mule tile X:"), c);
        c.gridx = 1;
        muleXField = new JTextField(model.muleX, 8);
        p.add(muleXField, c);

        c.gridx = 0; c.gridy = 3;
        p.add(new JLabel("Mule tile Y:"), c);
        c.gridx = 1;
        muleYField = new JTextField(model.muleY, 8);
        p.add(muleYField, c);

        return p;
    }

    // ------------------------------------------------------------------ //
    //  Event handlers                                                      //
    // ------------------------------------------------------------------ //

    private void onStart() {
        readValues();
        model.started = true;
        setVisible(false);
        dispose();
    }

    private void readValues() {
        model.selectedMonster = (String) monsterCombo.getSelectedItem();
        model.useCannon       = cannonCheck.isSelected();
        model.usePrayer       = prayerCheck.isSelected();
        model.useGE           = geCheck.isSelected();
        model.useMule         = muleCheck.isSelected();
        model.muleName        = muleNameField.getText().trim();
        model.eatThreshold    = eatSlider.getValue();
        model.specThreshold   = specSlider.getValue();
        model.foodAmount      = (int) foodSpinner.getValue();
        model.potionAmount    = (int) potionSpinner.getValue();
        model.discordWebhook  = webhookField.getText().trim();
        model.muleX           = muleXField.getText().trim();
        model.muleY           = muleYField.getText().trim();
    }

    private void applyMonsterDefaults() {
        model.applyMonsterDefaults();
        if (cannonCheck != null) cannonCheck.setSelected(model.useCannon);
        if (prayerCheck != null) prayerCheck.setSelected(model.usePrayer);
    }
}
