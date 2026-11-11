package game.view;

import game.model.GameProgress;
import game.model.Student;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;

/** Between-battle camp for reviewing the party and using supplies. */
public final class CampPanel extends JPanel {
    private static final int WIDTH = 960, HEIGHT = 540;
    private static final int[] POSITIONS = {220, 370, 730};
    private static final Color HP_COLOR = new Color(224, 73, 79);
    private static final Color SP_COLOR = new Color(76, 155, 245);
    private static final Color XP_COLOR = new Color(250, 218, 75);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/camp-bonfire-v1.png");
    private final BufferedImage[] characters = loadCharacters();
    private final Consumer<GameProgress> nextBattle;
    private final Runnable menu;
    private final Consumer<GameProgress> save;
    private final Consumer<GameProgress> inventory;
    private GameProgress progress;
    private int cursor;
    private boolean complete;
    private String message = "";

    private static BufferedImage[] loadCharacters() {
        String[] names = {"zack", "bogart", "charice", "angel"};
        BufferedImage[] result = new BufferedImage[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = trimTransparent(ScreenArt.load("/assets/sprites/camp-" + names[i] + "-v1.png"));
        }
        return result;
    }

    private static BufferedImage trimTransparent(BufferedImage image) {
        if (image == null) { return null; }
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 32) {
                    left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                }
            }
        }
        return right < left ? null : image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    /** Creates a camp screen. */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu) { this(nextBattle, menu, ignored -> { }); }
    /** Creates a camp screen with save support. */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu, Consumer<GameProgress> save) {
        this(nextBattle, menu, save, ignored -> { });
    }
    /** Creates a camp screen with save and inventory actions. */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu, Consumer<GameProgress> save,
            Consumer<GameProgress> inventory) {
        this.nextBattle = nextBattle; this.menu = menu; this.save = save; this.inventory = inventory;
        setPreferredSize(new Dimension(WIDTH, HEIGHT)); setFocusable(true);
        Keys.bind(this, "UP", "camp-up", () -> move(-1));
        Keys.bind(this, "DOWN", "camp-down", () -> move(1));
        Keys.bind(this, "LEFT", "camp-left", () -> move(-1));
        Keys.bind(this, "RIGHT", "camp-right", () -> move(1));
        Keys.bind(this, "ENTER", "camp-use", this::confirm);
        Keys.bind(this, "SPACE", "camp-use-space", this::confirm);
        Keys.bind(this, "C", "camp-continue", this::continueGame);
        Keys.bind(this, "E", "camp-equip", this::toggleVest);
        Keys.bind(this, "I", "camp-inventory", () -> { if (progress != null) { this.inventory.accept(progress); } });
        Keys.bind(this, "S", "camp-save", () -> { if (progress != null) { this.save.accept(progress); } });
        Keys.bind(this, "ESCAPE", "camp-menu", menu);
    }

    /** Opens camp with the current party. */
    public void open(GameProgress progress, boolean complete) { open(progress, complete, null); }
    /** Opens camp and optionally shows a reward message. */
    public void open(GameProgress progress, boolean complete, String reward) {
        this.progress = progress; this.complete = complete; cursor = 0;
        refresh(reward != null ? "Victory reward: " + reward + "! Press I to equip."
                : complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    /** Returns the current party state. */
    public GameProgress getProgress() { return progress; }
    private void move(int direction) {
        if (progress == null) { return; }
        cursor = Math.floorMod(cursor + direction, progress.getParty().size());
        refresh(complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    private void confirm() {
        if (progress == null) { return; }
        Student student = progress.getParty().get(cursor); int before = student.getHp();
        if (progress.useKit(student)) { refresh(student.getName() + " recovered " + (student.getHp() - before) + " HP."); }
        else { refresh(progress.getKits() == 0 ? "No First-Aid Kits remain."
                : !student.isAlive() ? "A knocked-out student cannot use a First-Aid Kit."
                : student.getName() + " already has full HP."); }
    }
    private void toggleVest() {
        if (progress == null) { return; }
        Student student = progress.getParty().get(cursor); boolean equipped = student.hasVest();
        if (progress.toggleVest(student)) { refresh(equipped ? "Campus Vest returned to inventory."
                : "Campus Vest equipped by " + student.getName() + "."); }
    }
    private void continueGame() { if (progress != null) { nextBattle.accept(progress); } }
    private void refresh(String text) { message = text; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.scale(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            ScreenArt.drawBackground(g, background, WIDTH, HEIGHT, 0);
            Graphics2D scene = (Graphics2D) g.create();
            try {
                scene.translate(0, -65);
                ScreenArt.drawBackground(scene, background, WIDTH, HEIGHT, 0);
            } finally { scene.dispose(); }
            g.setColor(new Color(7, 17, 39, 190)); g.fillRect(0, 0, WIDTH, 42);
            g.setFont(ClassicUi.title(28)); g.setColor(new Color(250, 218, 143));
            drawCentered(g, complete ? "ENCOUNTERS COMPLETE" : "CAMP", WIDTH / 2, 31);
            if (progress != null) {
                for (int i = 0; i < progress.getParty().size(); i++) { drawCharacter(g, progress.getParty().get(i), POSITIONS[i]); }
                drawStatus(g, progress.getParty().get(cursor), POSITIONS[cursor]);
                Student selected = progress.getParty().get(cursor);
                g.drawImage(PixelPointer.ART, POSITIONS[cursor] - (selected.getId() == Student.ANGEL ? 115 : 97),
                        285, 38, 28, null);
                g.setColor(new Color(7, 17, 39, 225)); g.fillRect(0, 451, WIDTH, 89);
                g.setFont(ClassicUi.numbers(16)); g.setColor(new Color(250, 218, 143));
                drawCentered(g, "Kits: " + progress.getKits() + "    •    " + progress.getPetot() + " Petot", WIDTH / 2, 474);
                g.setFont(ClassicUi.numbers(14)); g.setColor(Color.WHITE); drawCentered(g, message, WIDTH / 2, 496);
                g.setFont(ClassicUi.body(15)); drawCentered(g, "← → Select  •  Enter: Use kit  •  I: Inventory  •  S: Save  •  C: Route  •  Esc: Menu", WIDTH / 2, 524);
            }
        } finally { g.dispose(); }
    }
    private void drawCharacter(Graphics2D g, Student student, int center) {
        BufferedImage image = characters[student.getId()];
        if (image == null) { return; }
        boolean angel = student.getId() == Student.ANGEL;
        double scale = Math.min((angel ? 160.0 : 135.0) / image.getWidth(),
                (angel ? 190.0 : 160.0) / image.getHeight());
        int width = (int) Math.round(image.getWidth() * scale), height = (int) Math.round(image.getHeight() * scale);
        int left = center - width / 2;
        if (student.getId() == Student.CHARICE || student.getId() == Student.ANGEL) {
            g.drawImage(image, left + width, 429 - height, -width, height, null);
        } else {
            g.drawImage(image, left, 429 - height, width, height, null);
        }
    }
    private void drawStatus(Graphics2D g, Student student, int center) {
        int x = Math.max(8, Math.min(WIDTH - 246, center - 123)), y = 57, w = 238, h = 154;
        g.setColor(new Color(8, 18, 54, 235)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(15)); g.setColor(new Color(250, 218, 143));
        g.drawString(student.getName() + "  L" + student.getLevel() + (student.isAlive() ? "" : " KO"), x + 12, y + 23);
        drawResourceBar(g, x, y + 39, w, "HP", student.getHp(), student.getMaxHp(), HP_COLOR);
        drawResourceBar(g, x, y + 63, w, "SP", student.getSp(), student.getMaxSp(), SP_COLOR);
        int xp = student.getExperience() == Student.MAX_EXPERIENCE ? 100 : student.getExperience() % 100;
        drawResourceBar(g, x, y + 87, w, "XP", xp, 100, XP_COLOR);
        g.setColor(new Color(120, 149, 182)); g.drawLine(x + 12, y + 109, x + w - 12, y + 109);
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        g.drawString("ATK " + student.getAttack(), x + 12, y + 131);
        g.drawString("DEF " + student.getDefense(), x + 87, y + 131);
        g.drawString("SPD " + student.getSpeed(), x + 162, y + 131);
    }
    private static void drawResourceBar(Graphics2D g, int x, int row, int width, String label,
            int value, int maximum, Color color) {
        g.setFont(ClassicUi.numbers(12)); g.setColor(Color.WHITE); g.drawString(label, x + 12, row + 9);
        g.setColor(new Color(38, 49, 76)); g.fillRect(x + 48, row, 113, 9);
        g.setColor(color); g.fillRect(x + 48, row, (int) (113L * value / Math.max(1, maximum)), 9);
        String text = value + "/" + maximum;
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        g.drawString(text, x + width - 10 - g.getFontMetrics().stringWidth(text), row + 9);
    }
    private static void drawCentered(Graphics2D g, String text, int center, int y) {
        g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
