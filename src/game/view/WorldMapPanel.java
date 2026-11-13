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
    /** Serialized state: background.
     * @serial
     */
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/campus-route-v1.png");
    /** Serialized state: locked icons.
     * @serial
     */
    private final BufferedImage lockedIcons = ScreenArt.load("/assets/ui/carbon-market-locked-icons-v1.png");
    /** Serialized state: open icons.
     * @serial
     */
    private final BufferedImage openIcons = ScreenArt.load("/assets/ui/carbon-market-open-icons-v1.png");
    /** Serialized state: battle.
     * @serial
     */
    private final java.util.function.BiConsumer<GameProgress, Integer> battle;
    /** Serialized state: camp.
     * @serial
     */
    private final Consumer<GameProgress> camp;
    /** Serialized state: menu.
     * @serial
     */
    private final Runnable menu;
    /** Serialized state: shop.
     * @serial
     */
    private Consumer<GameProgress> shop = ignored -> { };
    /** Serialized state: merchant.
     * @serial
     */
    private Consumer<GameProgress> merchant = ignored -> { };
    /** Serialized state: shop selected.
     * @serial
     */
    private boolean shopSelected;
    /** Serialized state: progress.
     * @serial
     */
    private GameProgress progress;
    /** Serialized state: cursor.
     * @serial
     */
    private int cursor;
    /** Serialized state: notice.
     * @serial
     */
    private String notice;
    /** Serialized state: replay prompt.
     * @serial
     */
    private boolean replayPrompt;
    /** Serialized state: replay yes.
     * @serial
     */
    private boolean replayYes;
    /** Serialized state: exit prompt.
     * @serial
     */
    private boolean exitPrompt;
    /** Serialized state: exit yes.
     * @serial
     */
    private boolean exitYes;

    /** Creates the campus route screen.
     * @param battle starts the current encounter
     * @param camp opens the party's camp
     * @param menu returns to the main menu
     */
    public WorldMapPanel(Consumer<GameProgress> battle, Consumer<GameProgress> camp, Runnable menu) {
        this((progress, stage) -> battle.accept(progress), camp, menu);
    }

    /** Creates a route that supplies the selected stage to the battle.
     * @param battle the battle
     * @param camp the camp
     * @param menu the menu
     */
    public WorldMapPanel(java.util.function.BiConsumer<GameProgress, Integer> battle,
            Consumer<GameProgress> camp, Runnable menu) {
        this.battle = battle; this.camp = camp; this.menu = menu;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        Keys.bind(this, "LEFT", "map-left", () -> move(-1));
        Keys.bind(this, "RIGHT", "map-right", () -> move(1));
        Keys.bind(this, "UP", "map-up", () -> {
            if (!replayPrompt && !exitPrompt && progress != null && progress.getEncounter() >= 3 && cursor == 2) {
                shopSelected = true; notice = null; repaint();
            } else { move(-1); }
        });
        Keys.bind(this, "DOWN", "map-down", () -> {
            if (shopSelected && !replayPrompt && !exitPrompt) { shopSelected = false; repaint(); }
            else { move(1); }
        });
        Keys.bind(this, "ENTER", "map-enter", this::confirm);
        Keys.bind(this, "SPACE", "map-space", this::confirm);
        Keys.bind(this, "C", "map-camp", () -> { if (!replayPrompt && !exitPrompt && progress != null && progress.getEncounter() > 0) { camp.accept(progress); } });
        Keys.bind(this, "ESCAPE", "map-menu", () -> {
            if (replayPrompt) replayPrompt = false;
            else if (exitPrompt) exitPrompt = false;
            else if (progress != null) { exitPrompt = true; exitYes = false; }
            repaint();
        });
    }

    /** Supplies the action used to enter Carbon Market.
     * @param action the action
     */
    public void setShopAction(Consumer<GameProgress> action) { shop = java.util.Objects.requireNonNull(action); }
    /** Sets the action used to start the merchant encounter.
     * @param action the action
     */
    public void setMerchantAction(Consumer<GameProgress> action) { merchant = java.util.Objects.requireNonNull(action); }

    /** Displays the party's unlocked position on the route.
     * @param progress current party progress
     */
    public void open(GameProgress progress) {
        this.progress = progress; cursor = Math.min(progress.getEncounter(), LOCATIONS.length - 1); notice = null; replayPrompt = false; exitPrompt = false; shopSelected = false; repaint();
    }
    private void move(int direction) {
        if (progress == null) { return; }
        if (exitPrompt) { exitYes = !exitYes; repaint(); return; }
        if (replayPrompt) { replayYes = !replayYes; repaint(); return; }
        shopSelected = false;
        cursor = Math.max(0, Math.min(Math.min(progress.getEncounter(), LOCATIONS.length - 1), cursor + direction));
        notice = null; repaint();
    }
    private void confirm() {
        if (progress == null) { return; }
        if (exitPrompt) {
            exitPrompt = false;
            if (exitYes) { menu.run(); return; }
            repaint(); return;
        }
        if (shopSelected && !replayPrompt) { if (progress.isMerchantDefeated()) shop.accept(progress); else merchant.accept(progress); return; }
        if (replayPrompt) {
            replayPrompt = false;
            if (replayYes) { battle.accept(progress, cursor); return; }
        } else if (cursor < progress.getEncounter()) { replayPrompt = true; replayYes = false; }
        else if (cursor > progress.getEncounter()) { notice = "Complete the previous encounter to unlock this location."; }
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
                if (i == cursor && !shopSelected) {
                    g.setColor(new Color(15, 27, 46));
                    g.fillPolygon(new int[] {X[i] - 6, X[i] + 6, X[i]},
                            new int[] {Y[i] - 22, Y[i] - 22, Y[i] - 12}, 3);
                    g.setColor(new Color(255, 239, 190));
                    g.fillPolygon(new int[] {X[i] - 4, X[i] + 4, X[i]},
                            new int[] {Y[i] - 21, Y[i] - 21, Y[i] - 14}, 3);
                }
            }
            {
                int sx = X[2], sy = Y[2] - 38;
                BufferedImage icons = progress.isMerchantDefeated() ? openIcons : lockedIcons;
                if (icons != null) {
                    int half = icons.getWidth() / 2;
                    int side = progress.getEncounter() >= 3 ? 1 : 0;
                    g.drawImage(icons, sx - 21, sy - 14, sx + 21, sy + 14,
                            side * half, 0, (side + 1) * half, icons.getHeight(), null);
                }
                if (shopSelected) {
                    g.setColor(Color.BLACK); g.fillPolygon(new int[] {sx - 5, sx + 5, sx}, new int[] {sy - 18, sy - 18, sy - 10}, 3);
                    g.setColor(new Color(255, 239, 190)); g.fillPolygon(new int[] {sx - 4, sx + 4, sx}, new int[] {sy - 17, sy - 17, sy - 11}, 3);
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
            String state = cursor < progress.getEncounter() ? "COMPLETED" : cursor > progress.getEncounter() ? "LOCKED" : "AVAILABLE";
            out.drawString(shopSelected ? "Carbon Market   [" + (progress.isMerchantDefeated() ? "SHOP" : "LOCKED") + "]" : LOCATIONS[cursor] + "   [" + state + "]", 30, uiHeight - 75);
            String balance = progress.getPetot() + " Petot";
            out.drawString(balance, uiWidth - 30 - out.getFontMetrics().stringWidth(balance), uiHeight - 75);
            out.setFont(ClassicUi.body(14));
            out.drawString(notice != null ? notice : shopSelected ? (progress.isMerchantDefeated() ? "Enter: Visit shop" : "Enter: Challenge the Mysterious Merchant") + "   •   Down: Route   •   Esc: Menu"
                    : cursor == 2 && progress.getEncounter() >= 3 ? "↑ Carbon Market   •   ← → Move   •   Enter: Select   •   C: Camp"
                    : "← → Move   •   Enter: Select   •   C: Camp after victory   •   Esc: Menu", 30, uiHeight - 42);
            if (replayPrompt || exitPrompt) {
                int x = 280, y = 200, w = 400, h = 125;
                out.setColor(new Color(12, 25, 48, 240)); out.fillRect(x, y, w, h);
                out.setColor(new Color(247, 204, 114)); out.drawRect(x, y, w - 1, h - 1);
                out.drawRect(x + 3, y + 3, w - 7, h - 7);
                out.setFont(ClassicUi.numbers(16)); out.setColor(Color.WHITE);
                String title = exitPrompt ? "Return to main menu?" : "Replay " + LOCATIONS[cursor] + "?";
                out.drawString(title, x + (w - out.getFontMetrics().stringWidth(title)) / 2, y + 35);
                out.setFont(ClassicUi.body(14));
                String details = exitPrompt ? "Unsaved progress will be lost." : "Stage rewards are earned once.";
                out.drawString(details, x + (w - out.getFontMetrics().stringWidth(details)) / 2, y + 61);
                out.setFont(ClassicUi.numbers(16));
                boolean yes = exitPrompt ? exitYes : replayYes;
                int pointerX = x + (yes ? 200 : 85);
                out.drawImage(PixelPointer.ART, pointerX, y + 78, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
                out.setColor(yes ? Color.WHITE : new Color(250, 218, 143)); out.drawString("No", x + 120, y + 96);
                out.setColor(yes ? new Color(250, 218, 143) : Color.WHITE); out.drawString("Yes", x + 235, y + 96);
            }
        } finally { out.dispose(); }
    }
}
