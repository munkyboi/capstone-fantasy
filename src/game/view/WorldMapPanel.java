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
    private static final int[] Y = {104, 70, 104, 70, 104};
    private final Consumer<GameProgress> battle;
    private final Consumer<GameProgress> camp;
    private final Runnable menu;
    private GameProgress progress;
    private int cursor;
    private String notice;

    /** Creates the campus route screen.
     * @param battle starts the current encounter
     * @param camp opens the party's camp
     * @param menu returns to the main menu
     */
    public WorldMapPanel(Consumer<GameProgress> battle, Consumer<GameProgress> camp, Runnable menu) {
        this.battle = battle; this.camp = camp; this.menu = menu;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        Keys.bind(this, "LEFT", "map-left", () -> move(-1));
        Keys.bind(this, "RIGHT", "map-right", () -> move(1));
        Keys.bind(this, "UP", "map-up", () -> move(-1));
        Keys.bind(this, "DOWN", "map-down", () -> move(1));
        Keys.bind(this, "ENTER", "map-enter", this::confirm);
        Keys.bind(this, "SPACE", "map-space", this::confirm);
        Keys.bind(this, "C", "map-camp", () -> { if (progress != null && progress.getEncounter() > 0) { camp.accept(progress); } });
        Keys.bind(this, "ESCAPE", "map-menu", menu);
    }

    /** Displays the party's unlocked position on the route.
     * @param progress current party progress
     */
    public void open(GameProgress progress) {
        this.progress = progress; cursor = progress.getEncounter(); notice = null; repaint();
    }
    private void move(int direction) {
        if (progress == null) { return; }
        cursor = Math.max(0, Math.min(progress.getEncounter(), cursor + direction));
        notice = null; repaint();
    }
    private void confirm() {
        if (progress == null) { return; }
        if (cursor < progress.getEncounter()) { notice = "This encounter is complete."; }
        else if (cursor > progress.getEncounter()) { notice = "Complete the previous encounter to unlock this location."; }
        else if (cursor > 1) { notice = "This encounter is not available yet."; }
        else { battle.accept(progress); return; }
        repaint();
    }
    /** Draws the campus route and its current position.
     * @param graphics drawing context
     */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (progress == null || getWidth() <= 0 || getHeight() <= 0) { return; }
        BufferedImage canvas = new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(new Color(38, 73, 73)); g.fillRect(0, 0, 320, 180);
            g.setColor(new Color(47, 88, 81));
            for (int x = 0; x < 320; x += 20) { for (int y = 28; y < 145; y += 20) { g.fillRect(x, y, 9, 9); } }
            g.setColor(new Color(160, 155, 119)); g.setStroke(new java.awt.BasicStroke(6));
            for (int i = 0; i < X.length - 1; i++) { g.drawLine(X[i], Y[i], X[i + 1], Y[i + 1]); }
            for (int i = 0; i < X.length; i++) {
                int bx = X[i] - 8, by = Y[i] - 38;
                g.setColor(new Color(40, 48, 70)); g.fillRect(bx - 2, by + 4, 20, 13);
                g.setColor(i <= progress.getEncounter() ? new Color(105, 132, 151) : new Color(67, 82, 89));
                g.fillRect(bx, by + 5, 16, 11);
                g.setColor(new Color(25, 39, 58));
                g.fillPolygon(new int[] {bx - 3, bx + 8, bx + 19}, new int[] {by + 5, by, by + 5}, 3);
                g.setColor(new Color(224, 200, 140)); g.fillRect(bx + 3, by + 8, 3, 4); g.fillRect(bx + 10, by + 8, 3, 4);
            }
            for (int i = 0; i < X.length; i++) {
                g.setColor(i <= progress.getEncounter() ? new Color(233, 196, 113) : new Color(81, 93, 97));
                g.fillRect(X[i] - 8, Y[i] - 8, 17, 17);
                g.setColor(new Color(32, 42, 56)); g.drawRect(X[i] - 8, Y[i] - 8, 17, 17);
                g.setColor(new Color(245, 233, 190));
                if (i < progress.getEncounter()) { g.fillRect(X[i] - 2, Y[i] - 2, 5, 5); }
                if (i == cursor) {
                    g.setColor(Color.WHITE);
                    g.fillPolygon(new int[] {X[i] - 5, X[i] + 5, X[i]},
                            new int[] {Y[i] - 20, Y[i] - 20, Y[i] - 12}, 3);
                }
            }
        } finally { g.dispose(); }
        Graphics2D out = (Graphics2D) graphics.create();
        try {
            out.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            out.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);
            out.setColor(new Color(12, 25, 48, 225)); out.fillRect(0, 0, getWidth(), 60); out.fillRect(0, getHeight() - 108, getWidth(), 108);
            out.setColor(new Color(250, 218, 143)); out.setFont(new Font(Font.SERIF, Font.BOLD, 27));
            out.drawString("CAMPUS ROUTE", 30, 40);
            out.setColor(Color.WHITE); out.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            String state = cursor < progress.getEncounter() ? "COMPLETED" : cursor > progress.getEncounter() ? "LOCKED" : cursor > 1 ? "COMING SOON" : "AVAILABLE";
            out.drawString(LOCATIONS[cursor] + "   [" + state + "]", 30, getHeight() - 75);
            out.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            out.drawString(notice != null ? notice : "← → Move   •   Enter: Select   •   C: Camp after victory   •   Esc: Menu", 30, getHeight() - 42);
        } finally { out.dispose(); }
    }
}
