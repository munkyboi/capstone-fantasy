package game.view;

import game.model.GameProgress;
import game.model.Student;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Between-battle screen for reviewing the party and using supplies. */
public final class CampPanel extends JPanel {
    private final javax.swing.ImageIcon[] portraits = CharacterPortraits.load();
    private final JLabel[] students = {studentLabel(), studentLabel(), studentLabel()};
    private final JLabel title = new JLabel("CAMP", SwingConstants.CENTER);
    private final JLabel supplies = new JLabel("", SwingConstants.CENTER);
    private final JLabel help = new JLabel("", SwingConstants.CENTER);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/camp-courtyard-v1.png");
    private final Consumer<GameProgress> nextBattle;
    private final Runnable menu;
    private final Consumer<GameProgress> save;
    private final Consumer<GameProgress> inventory;
    private GameProgress progress;
    private int cursor;
    private boolean complete;

    private static JLabel studentLabel() {
        return new JLabel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D backgroundGraphics = (Graphics2D) graphics.create();
                try {
                    backgroundGraphics.setColor(getBackground());
                    backgroundGraphics.fillRect(0, 0, getWidth(), getHeight());
                } finally { backgroundGraphics.dispose(); }
                super.paintComponent(graphics);
                if (!Boolean.TRUE.equals(getClientProperty("selected")) || getIcon() == null) { return; }
                int size = Math.max(1, Math.round(32 * getFont().getSize2D() / 15));
                int x = getInsets().left + getIcon().getIconWidth() + getIconTextGap();
                graphics.drawImage(PixelPointer.HAND.getImage(), x, (getHeight() - size * 3 / 4) / 2,
                        size, size * 3 / 4, null);
            }
        };
    }

    /** Creates the camp screen.
     * @param nextBattle starts the next encounter
     * @param menu returns to the main menu
     */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu) {
        this(nextBattle, menu, ignored -> { });
    }

    /** Creates the camp screen with a save action.
     * @param nextBattle starts the next encounter
     * @param menu returns to the main menu
     * @param save opens the save slots
     */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu, Consumer<GameProgress> save) {
        this(nextBattle, menu, save, ignored -> { });
    }

    /** Creates a camp with save and inventory actions.
     * @param nextBattle returns to the route
     * @param menu returns to the menu
     * @param save opens save slots
     * @param inventory opens the shared bag
     */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu, Consumer<GameProgress> save,
            Consumer<GameProgress> inventory) {
        this.nextBattle = nextBattle;
        this.menu = menu;
        this.save = save;
        this.inventory = inventory;
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(18, 31, 62));
        setBorder(BorderFactory.createEmptyBorder(35, 125, 35, 125));
        title.setForeground(new Color(250, 218, 143));
        title.setFont(ClassicUi.title(32));
        title.setOpaque(true);
        title.setBackground(new Color(15, 25, 48, 220));
        add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(4, 1, 0, 8));
        center.setOpaque(false);
        for (JLabel label : students) {
            label.setForeground(Color.WHITE);
            label.setIconTextGap(12);
            label.setOpaque(false);
            label.setBackground(new Color(15, 25, 48, 225));
            label.setFont(ClassicUi.numbers(15));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(162, 184, 215), 2),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            center.add(label);
        }
        supplies.setForeground(new Color(250, 218, 143));
        supplies.setOpaque(true);
        supplies.setBackground(new Color(15, 25, 48, 230));
        supplies.setFont(ClassicUi.numbers(17));
        center.add(supplies);
        add(center, BorderLayout.CENTER);
        help.setForeground(Color.WHITE);
        help.setOpaque(true);
        help.setBackground(new Color(15, 25, 48, 230));
        help.setFont(ClassicUi.body(14));
        add(help, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "camp-up", () -> move(-1));
        Keys.bind(this, "DOWN", "camp-down", () -> move(1));
        Keys.bind(this, "ENTER", "camp-use", this::confirm);
        Keys.bind(this, "SPACE", "camp-use-space", this::confirm);
        Keys.bind(this, "C", "camp-continue", this::continueGame);
        Keys.bind(this, "E", "camp-equip", this::toggleVest);
        Keys.bind(this, "I", "camp-inventory", () -> { if (progress != null) { this.inventory.accept(progress); } });
        Keys.bind(this, "S", "camp-save", () -> { if (progress != null) { this.save.accept(progress); } });
        Keys.bind(this, "ESCAPE", "camp-menu", menu);
    }

    /** Opens camp with the current party state.
     * @param progress shared party state
     * @param complete whether the available encounters are finished
     */
    public void open(GameProgress progress, boolean complete) { open(progress, complete, null); }

    /** Opens camp and displays a newly earned reward.
     * @param progress shared party state
     * @param complete whether available encounters are finished
     * @param reward newly earned item name, or null
     */
    public void open(GameProgress progress, boolean complete, String reward) {
        this.progress = progress; this.complete = complete; cursor = 0;
        refresh(reward != null ? "Victory reward: " + reward + "! Press I to equip."
                : complete ? "The available encounters are complete." : "Prepare for the next encounter.");
    }

    /** Returns the party state currently displayed at camp.
     * @return current progress
     */
    public GameProgress getProgress() { return progress; }

    private void move(int direction) {
        if (progress == null) { return; }
        cursor = Math.floorMod(cursor + direction, students.length);
        refresh(complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    private void confirm() {
        if (progress == null) { return; }
        Student student = progress.getParty().get(cursor);
        int before = student.getHp();
        if (progress.useKit(student)) {
            refresh(student.getName() + " recovered " + (student.getHp() - before) + " HP.");
        } else {
            refresh(progress.getKits() == 0 ? "No First-Aid Kits remain."
                    : !student.isAlive() ? "A knocked-out student cannot use a First-Aid Kit."
                    : student.getName() + " already has full HP.");
        }
    }
    private void toggleVest() {
        if (progress == null) { return; }
        Student student = progress.getParty().get(cursor);
        boolean wasEquipped = student.hasVest();
        if (progress.toggleVest(student)) {
            refresh(wasEquipped ? "Campus Vest returned to inventory."
                    : "Campus Vest equipped by " + student.getName() + ".");
        }
    }
    private void continueGame() {
        if (progress == null) { return; }
        nextBattle.accept(progress);
    }
    private void refresh(String message) {
        title.setText(complete ? "ENCOUNTERS COMPLETE" : "CAMP");
        for (int i = 0; i < students.length; i++) {
            Student student = progress.getParty().get(i);
            ViewportFonts.setScaledIcon(students[i], portraits[student.getId()]);
            students[i].putClientProperty("selected", i == cursor);
            students[i].setText(String.format("    %-8s HP %3d/%3d  SP %3d/%3d  ATK %2d DEF %2d SPD %2d  %s%s",
                    student.getName(), student.getHp(), student.getMaxHp(), student.getSp(), student.getMaxSp(), student.getAttack(), student.getDefense(),
                    student.getSpeed(), student.hasVest() ? "[Vest]" : "", student.isAlive() ? "" : " KO"));
        }
        supplies.setText("<html><div style='text-align: center'>Kits: " + progress.getKits()
                + " &nbsp;•&nbsp; Campus Pass &nbsp;•&nbsp; Vest: "
                + (progress.getInventory().getVestOwner() == -1 ? "in bag" : "equipped")
                + "<br>" + message + "</div></html>");
        help.setText("↑ ↓ Select  •  Enter: Use kit  •  I: Inventory  •  S: Save  •  C: Route  •  Esc: Menu");
        // Selection is a client property, so it must repaint even when the label text is unchanged.
        repaint();
    }

    /** Draws the camp scene beneath the party controls.
     * @param graphics drawing context
     */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try { ScreenArt.drawBackground(g, background, getWidth(), getHeight(), 65); }
        finally { g.dispose(); }
    }
}
