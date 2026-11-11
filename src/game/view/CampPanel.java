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
    private final JLabel[] students = {new JLabel(), new JLabel(), new JLabel()};
    private final JLabel title = new JLabel("CAMP", SwingConstants.CENTER);
    private final JLabel supplies = new JLabel("", SwingConstants.CENTER);
    private final JLabel help = new JLabel("", SwingConstants.CENTER);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/camp-courtyard-v1.png");
    private final Consumer<GameProgress> nextBattle;
    private final Runnable menu;
    private final Consumer<GameProgress> save;
    private GameProgress progress;
    private int cursor;
    private boolean complete;

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
        this.nextBattle = nextBattle;
        this.menu = menu;
        this.save = save;
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(18, 31, 62));
        setBorder(BorderFactory.createEmptyBorder(35, 125, 35, 125));
        title.setForeground(new Color(250, 218, 143));
        title.setFont(new Font(Font.SERIF, Font.BOLD, 32));
        title.setOpaque(true);
        title.setBackground(new Color(15, 25, 48, 220));
        add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(4, 1, 0, 8));
        center.setOpaque(false);
        for (JLabel label : students) {
            label.setForeground(Color.WHITE);
            label.setIconTextGap(12);
            label.setOpaque(true);
            label.setBackground(new Color(15, 25, 48, 225));
            label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(162, 184, 215), 2),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            center.add(label);
        }
        supplies.setForeground(new Color(250, 218, 143));
        supplies.setOpaque(true);
        supplies.setBackground(new Color(15, 25, 48, 230));
        supplies.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        center.add(supplies);
        add(center, BorderLayout.CENTER);
        help.setForeground(Color.WHITE);
        help.setOpaque(true);
        help.setBackground(new Color(15, 25, 48, 230));
        help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        add(help, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "camp-up", () -> move(-1));
        Keys.bind(this, "DOWN", "camp-down", () -> move(1));
        Keys.bind(this, "ENTER", "camp-use", this::confirm);
        Keys.bind(this, "SPACE", "camp-use-space", this::confirm);
        Keys.bind(this, "C", "camp-continue", this::continueGame);
        Keys.bind(this, "E", "camp-equip", this::toggleVest);
        Keys.bind(this, "S", "camp-save", () -> { if (progress != null && !complete) { this.save.accept(progress); } });
        Keys.bind(this, "ESCAPE", "camp-menu", menu);
    }

    /** Opens camp with the current party state.
     * @param progress shared party state
     * @param complete whether the available encounters are finished
     */
    public void open(GameProgress progress, boolean complete) {
        this.progress = progress; this.complete = complete; cursor = 0;
        refresh(complete ? "The available encounters are complete." : "Prepare for the next encounter.");
    }

    private void move(int direction) {
        if (progress == null) { return; }
        cursor = Math.floorMod(cursor + direction, students.length);
        refresh(complete ? "The available encounters are complete." : "Choose a student to use a First-Aid Kit.");
    }
    private void confirm() {
        if (progress == null) { return; }
        if (complete) { menu.run(); return; }
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
        if (progress == null || complete) { return; }
        Student student = progress.getParty().get(cursor);
        boolean wasEquipped = student.hasVest();
        if (progress.toggleVest(student)) {
            refresh(wasEquipped ? "Campus Vest returned to inventory."
                    : "Campus Vest equipped by " + student.getName() + ".");
        }
    }
    private void continueGame() {
        if (progress == null) { return; }
        if (complete) { menu.run(); } else { nextBattle.accept(progress); }
    }
    private void refresh(String message) {
        title.setText(complete ? "ENCOUNTERS COMPLETE" : "CAMP");
        for (int i = 0; i < students.length; i++) {
            Student student = progress.getParty().get(i);
            students[i].setIcon(portraits[student.getId()]);
            students[i].setText(String.format("%s %-8s HP %3d/%3d  ATK %2d DEF %2d SPD %2d  %s%s", i == cursor ? "▶" : " ",
                    student.getName(), student.getHp(), student.getMaxHp(), student.getAttack(), student.getDefense(),
                    student.getSpeed(), student.hasVest() ? "[Vest]" : "", student.isAlive() ? "" : " KO"));
        }
        supplies.setText("<html><div style='text-align: center'>Kits: " + progress.getKits()
                + " &nbsp;•&nbsp; Campus Pass &nbsp;•&nbsp; Vest: "
                + (progress.getInventory().getVestOwner() == -1 ? "in bag" : "equipped")
                + "<br>" + message + "</div></html>");
        help.setText(complete ? "Enter: Return to menu" : "↑ ↓ Select  •  Enter: Use kit  •  E: Equip/remove vest  •  S: Save  •  C: Route  •  Esc: Menu");
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
