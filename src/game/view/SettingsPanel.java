package game.view;

import game.model.GameSettings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.nio.file.Path;
import javax.swing.JPanel;

/** Keyboard-operated gameplay settings. */
public final class SettingsPanel extends JPanel {
    private static final int[] CAPS = {20, 30, 40, 50, 60, 70, 80, 90, 100};
    /** Serialized state: settings.
     * @serial
     */
    private final GameSettings settings;
    /** Serialized state: file.
     * @serial
     */
    private final Path file;
    /** Serialized state: back.
     * @serial
     */
    private final Runnable back;
    /** Serialized state: background.
     * @serial
     */
    private final java.awt.image.BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/title-campus-v1.png");
    /** Serialized state: cursor.
     * @serial
     */
    private int cursor;
    /** Serialized state: notice.
     * @serial
     */
    private String notice = "Changes apply when starting or loading a game.";
    /** Creates a settings screen with its storage path and return action.
     * @param settings game settings
     * @param file file path
     * @param back the back
     */
    public SettingsPanel(GameSettings settings, Path file, Runnable back) {
        this.settings = settings; this.file = file; this.back = back;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        Keys.bind(this, "UP", "settings-up", () -> { cursor = Math.floorMod(cursor - 1, 8); repaint(); });
        Keys.bind(this, "DOWN", "settings-down", () -> { cursor = (cursor + 1) % 8; repaint(); });
        Keys.bind(this, "LEFT", "settings-left", () -> change(-1));
        Keys.bind(this, "RIGHT", "settings-right", () -> change(1));
        Keys.bind(this, "ESCAPE", "settings-back", this::finish);
        Keys.bind(this, "ENTER", "settings-confirm", this::confirm);
        Keys.bind(this, "SPACE", "settings-space", this::confirm);
    }
    private void change(int direction) {
        switch (cursor) {
            case 0 -> settings.setBattleSpeed(GameSettings.BattleSpeed.values()[Math.max(0, Math.min(GameSettings.BattleSpeed.values().length - 1, settings.getBattleSpeed().ordinal() + direction))]);
            case 1 -> { int index = 0; while (CAPS[index] != settings.getLevelCap()) index++; settings.setLevelCap(CAPS[Math.max(0, Math.min(CAPS.length - 1, index + direction))]); }
            case 2 -> settings.setPartySize(Math.max(1, Math.min(4, settings.getPartySize() + direction)));
            case 3 -> settings.setXpBoost(GameSettings.XpBoost.values()[Math.max(0, Math.min(GameSettings.XpBoost.values().length - 1, settings.getXpBoost().ordinal() + direction))]);
            case 4 -> settings.setDifficulty(GameSettings.Difficulty.values()[Math.max(0, Math.min(GameSettings.Difficulty.values().length - 1, settings.getDifficulty().ordinal() + direction))]);
            case 5 -> settings.setAmbushEnabled(direction > 0);
            case 6 -> settings.setGreedIsGood(direction > 0);
            case 7 -> { return; }
            default -> throw new IllegalStateException();
        }
        notice = "Changes apply when starting or loading a game."; repaint();
    }
    private void confirm() {
        if (cursor == 5) { settings.setAmbushEnabled(!settings.isAmbushEnabled()); repaint(); }
        else if (cursor == 6) { settings.setGreedIsGood(!settings.isGreedIsGood()); repaint(); }
        else if (cursor == 7) { settings.resetDefaults(); notice = "All settings restored to defaults."; repaint(); }
        else { finish(); }
    }
    private void finish() {
        try { settings.save(file); back.run(); }
        catch (java.io.IOException ex) { notice = "Could not save settings. Check the settings folder."; repaint(); }
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.scale(getWidth() / 960.0, getHeight() / 540.0);
            ScreenArt.drawBackground(g, background, 960, 540, 140);
            g.setColor(new Color(8, 18, 54, 235)); g.fillRect(155, 60, 650, 420);
            g.setColor(new Color(250, 218, 143)); g.drawRect(155, 60, 650, 420); g.drawRect(159, 64, 642, 412);
            g.setFont(ClassicUi.title(28)); g.drawString("GAME SETTINGS", 325, 102);
            String[] labels = {"BATTLE SPEED", "LEVEL CAP", "PARTY SIZE", "XP BOOST", "DIFFICULTY", "ENABLE AMBUSH", "GREED IS GOOD"};
            String[] values = {settings.getBattleSpeed().name(),
                    Integer.toString(settings.getLevelCap()), Integer.toString(settings.getPartySize()), settings.getXpBoost().name(), settings.getDifficulty().name(), settings.isAmbushEnabled() ? "ON" : "OFF", settings.isGreedIsGood() ? "ON" : "OFF"};
            int[] steps = {settings.getBattleSpeed().ordinal(),
                    java.util.Arrays.binarySearch(CAPS, settings.getLevelCap()), settings.getPartySize() - 1,
                    settings.getXpBoost().ordinal(), settings.getDifficulty().ordinal(), settings.isAmbushEnabled() ? 1 : 0, settings.isGreedIsGood() ? 1 : 0};
            int[] maxima = {GameSettings.BattleSpeed.values().length - 1, CAPS.length - 1, 3, 2, 3, 1, 1};
            g.setFont(ClassicUi.numbers(17));
            for (int i = 0; i < labels.length; i++) {
                int y = 137 + i * 32; g.setColor(i == cursor ? new Color(250, 218, 143) : Color.WHITE);
                if (i == cursor) g.drawImage(PixelPointer.ART, 178, y - 15, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
                g.drawString(labels[i], 215, y); g.drawString(values[i], 675, y);
                g.setColor(new Color(55, 71, 107)); g.fillRect(430, y - 10, 210, 8);
                g.setColor(new Color(94, 188, 246)); g.fillRect(430, y - 10, 210 * steps[i] / maxima[i], 8);
                g.setColor(Color.WHITE); g.fillRect(427 + 210 * steps[i] / maxima[i], y - 14, 7, 16);
            }
            g.setColor(cursor == 7 ? new Color(250, 218, 143) : new Color(100, 128, 180));
            g.drawRect(345, 382, 270, 31);
            if (cursor == 7) g.drawImage(PixelPointer.ART, 350, 387, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
            g.setFont(ClassicUi.numbers(15)); g.drawString("RESET TO DEFAULTS", 389, 403);
            g.setColor(new Color(250, 218, 143)); g.setFont(ClassicUi.numbers(12));
            g.drawString(notice, 185, 431);
            g.setColor(Color.WHITE); g.setFont(ClassicUi.body(14)); g.drawString("Up/Down: Select   •   Left/Right: Adjust   •   Enter: Select   •   Esc: Save", 185, 456);
        } finally { g.dispose(); }
    }
}
