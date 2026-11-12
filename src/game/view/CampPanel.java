package game.view;

import game.model.GameProgress;
import game.model.Student;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;

/** Between-battle camp for reviewing the party and using supplies. */
public final class CampPanel extends JPanel {
    private static final int WIDTH = 960, HEIGHT = 540;
    private static final int[] POSITIONS = {230, 365, 595, 735};
    private static final Color HP_COLOR = new Color(224, 73, 79);
    private static final Color SP_COLOR = new Color(76, 155, 245);
    private static final Color XP_COLOR = new Color(250, 218, 75);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/camp-parallax-base-v1.png");
    private final BufferedImage sky = ScreenArt.prepareBackground("/assets/backgrounds/camp-sky-v1.png");
    private final BufferedImage distance = ScreenArt.prepareBackground("/assets/backgrounds/camp-distant-v1.png");
    private final BufferedImage courtyard = ScreenArt.prepareBackground("/assets/backgrounds/camp-courtyard-layer-v2.png");
    private final BufferedImage fire = trimTransparent(ScreenArt.load("/assets/sprites/camp-fire-v1.png"));
    private final BufferedImage foreground = ScreenArt.load("/assets/backgrounds/camp-near-layer-v1.png");
    private final BufferedImage[] characters = loadCharacters();
    private final BufferedImage scenePixels = new BufferedImage(480, 270, BufferedImage.TYPE_INT_ARGB);
    private double cameraOffset;
    private double cameraTarget;
    private final java.util.Map<Integer, Integer> healingNumbers = new java.util.HashMap<>();
    private long healingStartedNanos;
    private final javax.swing.Timer healingAnimation = new javax.swing.Timer(33, event -> advanceHealing());
    private final javax.swing.Timer animation = new javax.swing.Timer(16, event -> advanceCamera());
    private final Consumer<GameProgress> nextBattle;
    private final Runnable menu;
    private final Consumer<GameProgress> save;
    private final Consumer<GameProgress> inventory;
    private final java.util.Random regenerationRandom;
    private GameProgress progress;
    private int cursor;
    private boolean complete;
    private boolean allocating;
    private boolean exitPrompt;
    private boolean exitYes;
    private int statCursor;
    private String message = "";

    private static BufferedImage[] loadCharacters() {
        String[] names = {"zack", "bogart", "charice", "angel"};
        BufferedImage[] result = new BufferedImage[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = trimTransparent(ScreenArt.load("/assets/sprites/camp-" + names[i] + (i == Student.BOGART ? "-v2.png" : "-v1.png")));
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
        this(nextBattle, menu, save, inventory, new java.util.Random());
    }
    /** Creates a camp with an injectable random source for regeneration. */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu, Consumer<GameProgress> save,
            Consumer<GameProgress> inventory, java.util.Random regenerationRandom) {
        this.nextBattle = nextBattle; this.menu = menu; this.save = save; this.inventory = inventory;
        this.regenerationRandom = java.util.Objects.requireNonNull(regenerationRandom);
        setPreferredSize(new Dimension(WIDTH, HEIGHT)); setFocusable(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!isShowing()) { animation.stop(); healingAnimation.stop(); healingNumbers.clear(); }
            }
        });
        Keys.bind(this, "UP", "camp-up", () -> move(-1));
        Keys.bind(this, "DOWN", "camp-down", () -> move(1));
        Keys.bind(this, "LEFT", "camp-left", () -> move(-1));
        Keys.bind(this, "RIGHT", "camp-right", () -> move(1));
        Keys.bind(this, "ENTER", "camp-use", this::confirm);
        Keys.bind(this, "SPACE", "camp-use-space", this::confirm);
        Keys.bind(this, "C", "camp-continue", this::continueGame);
        Keys.bind(this, "E", "camp-equip", this::toggleVest);
        Keys.bind(this, "I", "camp-inventory", () -> { if (progress != null && !allocating && !exitPrompt) { this.inventory.accept(progress); } });
        Keys.bind(this, "R", "camp-revive", () -> {
            if (progress == null || allocating || exitPrompt) return;
            Student target = campParty().get(cursor);
            int restored = progress.getInventory().useWhiteFlower(target);
            refresh(restored > 0 ? target.getName() + " was revived with " + restored + " HP."
                    : target.isAlive() ? target.getName() + " is already standing." : "No White Flowers remain.");
        });
        Keys.bind(this, "S", "camp-save", () -> { if (progress != null && !allocating && !exitPrompt) { this.save.accept(progress); } });
        Keys.bind(this, "L", "camp-allocate", () -> { if (progress != null && !exitPrompt) { allocating = !allocating; statCursor = 0; repaint(); } });
        Keys.bind(this, "ESCAPE", "camp-menu", this::escape);
    }

    /** Opens camp with the current party. */
    public void open(GameProgress progress, boolean complete) { open(progress, complete, null); }
    /** Opens camp and optionally shows a reward message. */
    public void open(GameProgress progress, boolean complete, String reward) {
        enter(progress, complete, reward, true);
    }
    /** Returns from a camp sub-screen without starting another camp round. */
    public void resume(GameProgress progress, boolean complete) { enter(progress, complete, null, false); }
    private void enter(GameProgress progress, boolean complete, String reward, boolean regenerate) {
        int healed = 0;
        healingAnimation.stop(); healingNumbers.clear();
        if (regenerate) for (Student student : progress.getParty()) {
            int studentHealed = 0;
            for (int attempt = 0; attempt < 5; attempt++) studentHealed += game.model.HealthRegeneration.attempt(student, regenerationRandom);
            if (studentHealed > 0) { healingNumbers.put(student.getId(), studentHealed); healed += studentHealed; }
        }
        if (!healingNumbers.isEmpty()) { healingStartedNanos = System.nanoTime(); healingAnimation.start(); }
        this.progress = progress; this.complete = complete; cursor = 0; allocating = false; exitPrompt = false;
        cameraTarget = focusOffset(campParty().get(cursor)); cameraOffset = cameraTarget; animation.stop();
        refresh(reward != null ? "Victory reward: " + reward + "! Press I to equip."
                : healed > 0 ? "Camp regeneration restored " + healed + " HP."
                : complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    /** Returns the current party state. */
    public GameProgress getProgress() { return progress; }
    private java.util.List<Student> campParty() {
        return progress.getParty().stream().sorted(java.util.Comparator.comparingInt(Student::getId)).toList();
    }
    private void move(int direction) {
        if (progress == null) { return; }
        if (exitPrompt) { exitYes = !exitYes; repaint(); return; }
        if (allocating) { statCursor = Math.floorMod(statCursor + direction, Student.Stat.values().length); repaint(); return; }
        cursor = Math.floorMod(cursor + direction, campParty().size());
        cameraTarget = focusOffset(campParty().get(cursor));
        if (isShowing()) { animation.start(); } else { cameraOffset = cameraTarget; }
        refresh(complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    private static double focusOffset(Student student) { return (480.0 - POSITIONS[student.getId()]) * 0.10; }
    private void advanceCamera() {
        cameraOffset += (cameraTarget - cameraOffset) * 0.22;
        if (Math.abs(cameraTarget - cameraOffset) < 0.05) { cameraOffset = cameraTarget; animation.stop(); }
        repaint();
    }
    private void advanceHealing() {
        if (System.nanoTime() - healingStartedNanos >= 2_000_000_000L) {
            healingAnimation.stop(); healingNumbers.clear();
        }
        repaint();
    }
    private void drawHealingNumbers(Graphics2D g, int middleOffset) {
        double elapsed = (System.nanoTime() - healingStartedNanos) / 1_000_000_000.0;
        if (elapsed >= 2.0) { return; }
        int alpha = (int) Math.round(255 * Math.min(1.0, (2.0 - elapsed) / 0.5));
        g.setFont(ClassicUi.numbers(18));
        for (var entry : healingNumbers.entrySet()) {
            String text = "+" + entry.getValue() + " HP";
            int x = POSITIONS[entry.getKey()] + middleOffset - g.getFontMetrics().stringWidth(text) / 2;
            int y = 351 - (int) (elapsed * 18);
            g.setColor(new Color(255, 255, 255, alpha));
            for (int dx = -2; dx <= 2; dx += 2) for (int dy = -2; dy <= 2; dy += 2) g.drawString(text, x + dx, y + dy);
            g.setColor(new Color(22, 153, 69, alpha)); g.drawString(text, x, y);
        }
    }
    private void confirm() {
        if (progress == null) { return; }
        if (exitPrompt) {
            exitPrompt = false;
            if (exitYes) { menu.run(); } else { repaint(); }
            return;
        }
        Student student = campParty().get(cursor);
        if (allocating) {
            Student.Stat stat = Student.Stat.values()[statCursor];
            refresh(student.allocateStatPoint(stat) ? student.getName() + " increased " + stat + "." : "No stat points available.");
            return;
        }
        int before = student.getHp();
        if (progress.useKit(student)) { refresh(student.getName() + " recovered " + (student.getHp() - before) + " HP."); }
        else { refresh(progress.getKits() == 0 ? "No First-Aid Kits remain."
                : !student.isAlive() ? "A knocked-out student cannot use a First-Aid Kit."
                : student.getName() + " already has full HP."); }
    }
    private void toggleVest() {
        if (progress == null || allocating || exitPrompt) { return; }
        Student student = campParty().get(cursor); boolean equipped = student.hasVest();
        if (progress.toggleVest(student)) { refresh(equipped ? "Campus Vest returned to inventory."
                : "Campus Vest equipped by " + student.getName() + "."); }
    }
    private void continueGame() { if (progress != null && !allocating && !exitPrompt) { nextBattle.accept(progress); } }
    private void escape() {
        if (exitPrompt) { exitPrompt = false; }
        else if (allocating) { allocating = false; }
        else if (progress != null) { exitPrompt = true; exitYes = false; }
        repaint();
    }
    private void refresh(String text) { message = text; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.scale(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            drawPixelScene(g);
            g.setColor(new Color(7, 17, 39, 190)); g.fillRect(0, 0, WIDTH, 42);
            g.setFont(ClassicUi.title(28)); g.setColor(new Color(250, 218, 143));
            drawCentered(g, complete ? "ENCOUNTERS COMPLETE" : "CAMP", WIDTH / 2, 31);
            if (progress != null) {
                int middleOffset = (int) Math.round(cameraOffset * 0.65);
                Student selected = campParty().get(cursor);
                int position = POSITIONS[selected.getId()] + middleOffset;
                drawStatus(g, selected, position);
                g.drawImage(PixelPointer.ART, position - (selected.getId() == Student.ANGEL ? 115 : 97),
                        285, 38, 28, null);
                g.setColor(new Color(7, 17, 39, 225)); g.fillRect(0, 451, WIDTH, 89);
                g.setFont(ClassicUi.numbers(16)); g.setColor(new Color(250, 218, 143));
                drawCentered(g, "Kits: " + progress.getKits() + "  •  Flowers: " + progress.getInventory().getWhiteFlowers() + "  •  " + progress.getPetot() + " Petot", WIDTH / 2, 474);
                g.setFont(ClassicUi.numbers(14)); g.setColor(Color.WHITE); drawCentered(g, message, WIDTH / 2, 496);
                g.setFont(ClassicUi.body(14)); drawCentered(g, allocating
                        ? "← → Choose stat  •  Enter: Add point  •  Esc: Back"
                        : "← → Select  •  Enter: Kit  •  R: Revive  •  L: Stats  •  I: Inventory  •  S: Save  •  C: Route  •  Esc: Menu", WIDTH / 2, 524);
                if (allocating) { drawAllocation(g, selected); }
                if (exitPrompt) { drawExitPrompt(g); }
            }
        } finally { g.dispose(); }
    }
    private void drawExitPrompt(Graphics2D g) {
        int x = 280, y = 195, w = 400, h = 140;
        g.setColor(new Color(8, 18, 54, 245)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(16)); g.setColor(Color.WHITE);
        drawCentered(g, "Return to main menu?", x + w / 2, y + 33);
        g.setFont(ClassicUi.body(14)); drawCentered(g, "Unsaved progress will be lost.", x + w / 2, y + 60);
        g.setFont(ClassicUi.numbers(16));
        g.drawImage(PixelPointer.ART, x + (exitYes ? 200 : 85), y + 84, 27, 20, null);
        g.setColor(exitYes ? Color.WHITE : new Color(250, 218, 143)); g.drawString("No", x + 120, y + 102);
        g.setColor(exitYes ? new Color(250, 218, 143) : Color.WHITE); g.drawString("Yes", x + 235, y + 102);
    }
    private void drawPixelScene(Graphics2D output) {
        Graphics2D pixels = scenePixels.createGraphics();
        try {
            pixels.setComposite(java.awt.AlphaComposite.Clear);
            pixels.fillRect(0, 0, scenePixels.getWidth(), scenePixels.getHeight());
            pixels.setComposite(java.awt.AlphaComposite.SrcOver);
            pixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            pixels.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            pixels.scale(0.5, 0.5);
            drawScenery(pixels, cameraOffset);
            if (progress != null) {
                int middleOffset = (int) Math.round(cameraOffset * 0.65);
                for (Student student : campParty()) { drawCharacter(pixels, student, POSITIONS[student.getId()] + middleOffset); }
                drawFire(pixels, middleOffset);
                drawForeground(pixels, cameraOffset);
                drawHealingNumbers(pixels, middleOffset);
            }
        } finally { pixels.dispose(); }
        output.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        output.drawImage(scenePixels, 0, 0, WIDTH, HEIGHT, null);
    }
    private void drawScenery(Graphics2D g, double focus) {
        ScreenArt.drawBackground(g, background, WIDTH, HEIGHT, 0);
        if (sky == null || distance == null || courtyard == null) { return; }
        drawDepthLayer(g, sky, focus * 0.10, -240);
        drawDepthLayer(g, distance, focus * 0.30, -105);
        drawDepthLayer(g, courtyard, focus * 0.65, -65);
    }
    private static void drawDepthLayer(Graphics2D g, BufferedImage layer, double offset, int top) {
        if (layer != null) { g.drawImage(layer, -32 + (int) Math.round(offset), top, 1024, 576, null); }
    }
    private void drawFire(Graphics2D g, int middleOffset) {
        if (fire != null) {
            double scale = Math.min(112.0 / fire.getWidth(), 120.0 / fire.getHeight());
            int width = (int) Math.round(fire.getWidth() * scale), height = (int) Math.round(fire.getHeight() * scale);
            g.drawImage(fire, 480 - width / 2 + middleOffset, 447 - height, width, height, null);
        }
    }
    private void drawForeground(Graphics2D g, double focus) {
        if (foreground != null) {
            drawDepthLayer(g, foreground, focus, -65);
        }
    }
    private void drawAllocation(Graphics2D g, Student student) {
        int x = 306, y = 235, w = 348, h = 195;
        g.setColor(new Color(8, 18, 54, 245)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(15)); g.drawString(student.getName() + "  •  Points: " + student.getUnspentStatPoints(), x + 15, y + 24);
        String[] options = {"HP +5", "SP +3", "ATK +1", "DEF +1", "SPD +1"};
        g.setFont(ClassicUi.numbers(14));
        for (int i = 0; i < options.length; i++) {
            int row = y + 45 + i * 25;
            g.setColor(i == statCursor ? new Color(250, 218, 143) : Color.WHITE);
            if (i == statCursor) { g.drawImage(PixelPointer.ART, x + 18, row - 12, 25, 19, null); }
            g.drawString(options[i], x + 52, row + 2);
        }
    }
    private void drawCharacter(Graphics2D g, Student student, int center) {
        BufferedImage fallen = student.isAlive() ? null : FallenSprites.forStudent(student.getId());
        if (fallen != null) {
            double scale = Math.min(125.0 / fallen.getWidth(), 65.0 / fallen.getHeight());
            int width = (int) Math.round(fallen.getWidth() * scale), height = (int) Math.round(fallen.getHeight() * scale);
            g.drawImage(fallen, center - width / 2, 429 - height, width, height, null);
            return;
        }
        BufferedImage image = characters[student.getId()];
        if (image == null) { return; }
        boolean angel = student.getId() == Student.ANGEL;
        double scale = Math.min((angel ? 150.0 : 125.0) / image.getWidth(),
                (angel ? 190.0 : 160.0) / image.getHeight());
        int width = (int) Math.round(image.getWidth() * scale), height = (int) Math.round(image.getHeight() * scale);
        int left = center - width / 2;
        if (student.getId() == Student.CHARICE || student.getId() == Student.ANGEL) {
            g.drawImage(image, left + width, 429 - height, -width, height, null);
        } else {
            g.drawImage(image, left, 429 - height, width, height, null);
        }
    }
    private static String formatStat(double value) {
        return Long.toString((long) Math.ceil(value - 1e-9));
    }
    private void drawStatus(Graphics2D g, Student student, int center) {
        int x = Math.max(8, Math.min(WIDTH - 246, center - 123)), y = 57, w = 238, h = 174;
        g.setColor(new Color(8, 18, 54, 235)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(15)); g.setColor(new Color(250, 218, 143));
        g.drawString(student.getName() + " L" + student.getLevel() + " P:" + student.getUnspentStatPoints()
                + (student.isAlive() ? "" : " KO"), x + 12, y + 23);
        drawResourceBar(g, x, y + 39, w, "HP", student.getHp(), student.getMaxHp(), HP_COLOR);
        drawResourceBar(g, x, y + 63, w, "SP", student.getSp(), student.getMaxSp(), SP_COLOR);
        long requiredXp = student.getXpRequired();
        drawResourceBar(g, x, y + 87, w, "XP", requiredXp == 0 ? 1 : student.getXpProgress(),
                requiredXp == 0 ? 1 : requiredXp, XP_COLOR);
        g.setColor(new Color(120, 149, 182)); g.drawLine(x + 12, y + 109, x + w - 12, y + 109);
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        g.drawString("ATK " + student.getAttack(), x + 12, y + 131);
        g.drawString("DEF " + student.getDefense(), x + 87, y + 131);
        g.drawString("SPD " + formatStat(student.getEffectiveSpeed()), x + 162, y + 131);
        g.drawString("AGI " + formatStat(student.getEffectiveAgility()), x + 12, y + 151);
        g.drawString("ACC " + formatStat(student.getEffectiveAccuracy()), x + 87, y + 151);
    }
    private static void drawResourceBar(Graphics2D g, int x, int row, int width, String label,
            long value, long maximum, Color color) {
        g.setFont(ClassicUi.numbers(12)); g.setColor(Color.WHITE); g.drawString(label, x + 12, row + 9);
        g.setColor(new Color(38, 49, 76)); g.fillRect(x + 48, row, 113, 9);
        g.setColor(color); g.fillRect(x + 48, row, (int) (113L * value / Math.max(1, maximum)), 9);
        String text = label.equals("XP") && maximum == 1 ? "MAX" : value + "/" + maximum;
        if (label.equals("XP") && g.getFontMetrics().stringWidth(text) > width - 173) {
            text = compactXp(value) + "/" + compactXp(maximum);
        }
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        if (g.getFontMetrics().stringWidth(text) > width - 173) { g.setFont(ClassicUi.numbers(8)); }
        g.drawString(text, x + width - 10 - g.getFontMetrics().stringWidth(text), row + 9);
    }
    private static String compactXp(long value) {
        if (value >= 1_000_000_000) return String.format(java.util.Locale.ROOT, "%.1fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (value >= 10_000) return String.format(java.util.Locale.ROOT, "%.1fK", value / 1_000.0);
        return Long.toString(value);
    }
    private static void drawCentered(Graphics2D g, String text, int center, int y) {
        g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
