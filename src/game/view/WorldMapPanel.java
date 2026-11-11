package game.view;

import game.model.GameProgress;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;

/** A fixed campus route connecting the game's encounters. */
public final class WorldMapPanel extends JPanel {
    private static final String[] LOCATIONS = {
        "Campus Entrance", "Library Hall", "Training Ground", "Old Campus Building", "Crystal Chamber"
    };
    private static final int[] X = {35, 94, 156, 218, 282};
    private static final int[] Y = {105, 82, 105, 82, 105};
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/campus-route-v1.png");
    private final java.util.function.BiConsumer<GameProgress, Integer> battle;
    private final Consumer<GameProgress> camp;
    private final Runnable menu;
    private GameProgress progress;
    private int cursor;
    private String notice;
    private boolean replayPrompt;
    private boolean replayYes;

    /** Creates the campus route screen.
     * @param battle starts the current encounter
     * @param camp opens the party's camp
     * @param menu returns to the main menu
     */
    public WorldMapPanel(Consumer<GameProgress> battle, Consumer<GameProgress> camp, Runnable menu) {
        this((progress, stage) -> battle.accept(progress), camp, menu);
    }

    /** Creates a route that supplies the selected stage to the battle. */
    public WorldMapPanel(java.util.function.BiConsumer<GameProgress, Integer> battle,
            Consumer<GameProgress> camp, Runnable menu) {
        this.battle = battle; this.camp = camp; this.menu = menu;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        Keys.bind(this, "LEFT", "map-left", () -> move(-1));
        Keys.bind(this, "RIGHT", "map-right", () -> move(1));
        Keys.bind(this, "UP", "map-up", () -> move(-1));
        Keys.bind(this, "DOWN", "map-down", () -> move(1));
        Keys.bind(this, "ENTER", "map-enter", this::confirm);
        Keys.bind(this, "SPACE", "map-space", this::confirm);
        Keys.bind(this, "C", "map-camp", () -> { if (!replayPrompt && progress != null && progress.getEncounter() > 0) { camp.accept(progress); } });
        Keys.bind(this, "ESCAPE", "map-menu", () -> { if (replayPrompt) { replayPrompt = false; repaint(); } else { menu.run(); } });
    }

    /** Displays the party's unlocked position on the route.
     * @param progress current party progress
     */
    public void open(GameProgress progress) {
        this.progress = progress; cursor = progress.getEncounter(); notice = null; replayPrompt = false; repaint();
    }
    private void move(int direction) {
        if (progress == null) { return; }
        if (replayPrompt) { replayYes = !replayYes; repaint(); return; }
        cursor = Math.max(0, Math.min(progress.getEncounter(), cursor + direction));
        notice = null; repaint();
    }
    private void confirm() {
        if (progress == null) { return; }
        if (replayPrompt) {
            replayPrompt = false;
            if (replayYes) { battle.accept(progress, cursor); return; }
        } else if (cursor < progress.getEncounter()) { replayPrompt = true; replayYes = false; }
        else if (cursor > progress.getEncounter()) { notice = "Complete the previous encounter to unlock this location."; }
        else if (cursor > 1) { notice = "This encounter is not available yet."; }
        else { battle.accept(progress, cursor); return; }
        repaint();
    }
    /** Draws the campus route and its current position.
     * @param graphics drawing context
     */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (progress == null || getWidth() <= 0 || getHeight() <= 0) { return; }
        BufferedImage canvas = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.scale(2.0, 2.0);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(new Color(18, 42, 59)); g.fillRect(0, 0, 320, 180);
            if (background != null) { g.drawImage(background, 0, 0, 320, 180, null); }
            g.setColor(new Color(7, 18, 38, 55)); g.fillRect(0, 0, 320, 180);
            g.setColor(new Color(17, 30, 48)); g.setStroke(new java.awt.BasicStroke(7));
            for (int i = 0; i < X.length - 1; i++) { g.drawLine(X[i], Y[i], X[i + 1], Y[i + 1]); }
            g.setStroke(new java.awt.BasicStroke(4));
            for (int i = 0; i < X.length - 1; i++) {
                g.setColor(i < progress.getEncounter() ? new Color(222, 188, 116) : new Color(92, 111, 119));
                g.drawLine(X[i], Y[i], X[i + 1], Y[i + 1]);
            }
            for (int i = 0; i < X.length; i++) {
                g.setColor(new Color(15, 27, 46)); g.fillOval(X[i] - 9, Y[i] - 9, 19, 19);
                g.setColor(i < progress.getEncounter() ? new Color(100, 198, 199)
                        : i == progress.getEncounter() ? new Color(242, 205, 128) : new Color(91, 111, 123));
                g.fillOval(X[i] - 7, Y[i] - 7, 15, 15);
                g.setColor(new Color(24, 39, 60)); g.fillOval(X[i] - 5, Y[i] - 5, 11, 11);
                g.setFont(ClassicUi.numbers(8));
                g.setColor(i <= progress.getEncounter() ? new Color(250, 226, 166) : new Color(159, 176, 178));
                String number = Integer.toString(i + 1);
                g.drawString(number, X[i] - g.getFontMetrics().stringWidth(number) / 2, Y[i] + 3);
                if (i == cursor) {
                    g.setColor(new Color(15, 27, 46));
                    g.fillPolygon(new int[] {X[i] - 6, X[i] + 6, X[i]},
                            new int[] {Y[i] - 22, Y[i] - 22, Y[i] - 12}, 3);
                    g.setColor(new Color(255, 239, 190));
                    g.fillPolygon(new int[] {X[i] - 4, X[i] + 4, X[i]},
                            new int[] {Y[i] - 21, Y[i] - 21, Y[i] - 14}, 3);
                }
            }
        } finally { g.dispose(); }
        Graphics2D out = (Graphics2D) graphics.create();
        try {
            out.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            out.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);
            double uiScale = getWidth() / 960.0;
            out.scale(uiScale, uiScale);
            int uiWidth = 960, uiHeight = 540;
            out.setColor(new Color(12, 25, 48, 225)); out.fillRect(0, 0, uiWidth, 60); out.fillRect(0, uiHeight - 108, uiWidth, 108);
            out.setColor(new Color(250, 218, 143)); out.setFont(ClassicUi.title(27));
            out.drawString("CAMPUS ROUTE", 30, 40);
            out.setColor(Color.WHITE); out.setFont(ClassicUi.numbers(17));
            String state = cursor < progress.getEncounter() ? "COMPLETED" : cursor > progress.getEncounter() ? "LOCKED" : cursor > 1 ? "COMING SOON" : "AVAILABLE";
            out.drawString(LOCATIONS[cursor] + "   [" + state + "]", 30, uiHeight - 75);
            out.setFont(ClassicUi.body(14));
            out.drawString(notice != null ? notice : "← → Move   •   Enter: Select   •   C: Camp after victory   •   Esc: Menu", 30, uiHeight - 42);
            if (replayPrompt) {
                int x = 280, y = 200, w = 400, h = 125;
                out.setColor(new Color(12, 25, 48, 240)); out.fillRect(x, y, w, h);
                out.setColor(new Color(247, 204, 114)); out.drawRect(x, y, w - 1, h - 1);
                out.drawRect(x + 3, y + 3, w - 7, h - 7);
                out.setFont(ClassicUi.numbers(16)); out.setColor(Color.WHITE);
                String title = "Replay " + LOCATIONS[cursor] + "?";
                out.drawString(title, x + (w - out.getFontMetrics().stringWidth(title)) / 2, y + 35);
                out.setFont(ClassicUi.body(14));
                out.drawString("Stage rewards are earned once.", x + 70, y + 61);
                out.setFont(ClassicUi.numbers(16));
                int pointerX = x + (replayYes ? 200 : 85);
                out.drawImage(PixelPointer.ART, pointerX, y + 78, 27, 20, null);
                out.setColor(replayYes ? Color.WHITE : new Color(250, 218, 143)); out.drawString("No", x + 120, y + 96);
                out.setColor(replayYes ? new Color(250, 218, 143) : Color.WHITE); out.drawString("Yes", x + 235, y + 96);
            }
        } finally { out.dispose(); }
    }
}
