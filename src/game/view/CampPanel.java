package game.view;

import game.model.GameProgress;
import game.model.Student;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Between-battle screen for reviewing the party and using supplies. */
public final class CampPanel extends JPanel {
    private final JLabel[] students = {new JLabel(), new JLabel(), new JLabel()};
    private final JLabel title = new JLabel("CAMP", SwingConstants.CENTER);
    private final JLabel supplies = new JLabel("", SwingConstants.CENTER);
    private final JLabel help = new JLabel("", SwingConstants.CENTER);
    private final Consumer<GameProgress> nextBattle;
    private final Runnable menu;
    private GameProgress progress;
    private int cursor;
    private boolean complete;

    /** Creates the camp screen.
     * @param nextBattle starts the next encounter
     * @param menu returns to the main menu
     */
    public CampPanel(Consumer<GameProgress> nextBattle, Runnable menu) {
        this.nextBattle = nextBattle;
        this.menu = menu;
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(18, 31, 62));
        setBorder(BorderFactory.createEmptyBorder(35, 125, 35, 125));
        title.setForeground(new Color(250, 218, 143));
        title.setFont(new Font(Font.SERIF, Font.BOLD, 32));
        add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(4, 1, 0, 8));
        center.setOpaque(false);
        for (JLabel label : students) {
            label.setForeground(Color.WHITE);
            label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(162, 184, 215), 2),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            center.add(label);
        }
        supplies.setForeground(new Color(250, 218, 143));
        supplies.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        center.add(supplies);
        add(center, BorderLayout.CENTER);
        help.setForeground(Color.WHITE);
        help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        add(help, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "camp-up", () -> move(-1));
        Keys.bind(this, "DOWN", "camp-down", () -> move(1));
        Keys.bind(this, "ENTER", "camp-use", this::confirm);
        Keys.bind(this, "SPACE", "camp-use-space", this::confirm);
        Keys.bind(this, "C", "camp-continue", this::continueGame);
        Keys.bind(this, "ESCAPE", "camp-menu", menu);
    }

    /** Opens camp with the current party state.
     * @param progress shared party state
     * @param complete whether the available encounters are finished
     */
    public void open(GameProgress progress, boolean complete) {
        this.progress = progress; this.complete = complete; cursor = 0;
        refresh(complete ? "The available encounters are complete." : "Victory! Prepare for the next encounter.");
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
    private void continueGame() {
        if (progress == null) { return; }
        if (complete) { menu.run(); } else { nextBattle.accept(progress); }
    }
    private void refresh(String message) {
        title.setText(complete ? "ENCOUNTERS COMPLETE" : "CAMP");
        for (int i = 0; i < students.length; i++) {
            Student student = progress.getParty().get(i);
            students[i].setText(String.format("%s %-9s  HP %3d / %3d%s", i == cursor ? "▶" : " ",
                    student.getName(), student.getHp(), student.getMaxHp(), student.isAlive() ? "" : "  KO"));
        }
        supplies.setText("First-Aid Kits: " + progress.getKits() + "   •   " + message);
        help.setText(complete ? "Enter: Return to menu" : "↑ ↓ Select student   •   Enter: Use kit   •   C: Continue   •   Esc: Main menu");
    }
}
