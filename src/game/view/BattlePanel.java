package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.Student;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/** Displays the campus entrance encounter and its controls. */
public final class BattlePanel extends JPanel {
    private static final int AP_FRAME_MILLIS = 50;
    private static final double AP_EPSILON = 0.0000001;
    private static final int AP_DISPLAY_SCALE = 100;
    private static final int EFFECT_MILLIS = 1600;
    private static final int ANIMATION_FRAME_MILLIS = 50;
    private final Runnable returnToMenu;
    private final Consumer<GameProgress> onVictory;
    private final GameSettings settings;
    private final JLabel status = new JLabel();
    private final JLabel[] partyStatus = {new JLabel(), new JLabel(), new JLabel()};
    private final JProgressBar[] apBars = {new JProgressBar(), new JProgressBar(), new JProgressBar()};
    private final JTextArea log = new JTextArea();
    private final JScrollPane logScroll = new JScrollPane(log);
    private final JButton attack = new JButton("Attack");
    private final JButton ability = new JButton("Ability");
    private final JButton item = new JButton("Item");
    private final JButton defend = new JButton("Defend");
    private final JButton finish = new JButton("Return to Menu");
    private final BattleScene scene;
    private Battle battle;
    private GameProgress progress;
    private int commandIndex;
    private final JButton[] commands = {attack, ability, item, defend};
    private enum TargetAction { NONE, ATTACK, ABILITY, ITEM }
    private TargetAction targeting = TargetAction.NONE;
    private int targetIndex;
    private String notice;
    private final Map<Student, Double> actionPoints = new HashMap<>();
    private final Map<Enemy, Double> enemyPoints = new HashMap<>();
    private final Timer apTimer = new Timer(AP_FRAME_MILLIS, event -> tickActionPoints());
    private long lastTickNanos;
    private final Timer effectTimer;
    private boolean showEffects;
    private long effectStartedNanos;
    private Object attackingSprite;

    /** Creates the battle screen.
     * @param returnToMenu returns to the title screen
     */
    public BattlePanel(Runnable returnToMenu) {
        this(returnToMenu, ignored -> returnToMenu.run());
    }

    /** Creates a battle screen with a victory transition.
     * @param returnToMenu returns after defeat
     * @param onVictory opens the next screen after victory
     */
    public BattlePanel(Runnable returnToMenu, Consumer<GameProgress> onVictory) {
        this(returnToMenu, onVictory, new GameSettings());
    }

    /** Creates a battle screen using shared game settings.
     * @param returnToMenu returns after defeat
     * @param onVictory opens the next screen after victory
     * @param settings shared gameplay settings
     */
    public BattlePanel(Runnable returnToMenu, Consumer<GameProgress> onVictory, GameSettings settings) {
        this.returnToMenu = returnToMenu;
        this.onVictory = onVictory;
        this.settings = settings;
        scene = new BattleScene(new BattleScene.State() {
            @Override public Battle battle() { return battle; }
            @Override public boolean partyTarget(int index) {
                return targeting != TargetAction.NONE && targetsParty() && index == targetIndex;
            }
            @Override public boolean enemyTarget(int index) {
                return targeting != TargetAction.NONE && !targetsParty()
                        && (targetsAllEnemies() || index == targetIndex);
            }
            @Override public boolean showEffects() { return showEffects; }
            @Override public double enemyPoints(Enemy enemy) { return enemyPoints.getOrDefault(enemy, 0.0); }
            @Override public Object attackingSprite() { return attackingSprite; }
            @Override public double effectProgress() {
                return showEffects ? Math.min(1.0, (System.nanoTime() - effectStartedNanos) / (EFFECT_MILLIS * 1_000_000.0)) : 1.0;
            }
        });
        effectTimer = new Timer(ANIMATION_FRAME_MILLIS, event -> {
            if ((System.nanoTime() - effectStartedNanos) / 1_000_000.0 >= EFFECT_MILLIS) {
                showEffects = false; attackingSprite = null; effectTimerStop();
            }
            scene.repaint();
        });
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout());
        add(scene, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBackground(new Color(8, 14, 38));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        status.setForeground(new Color(250, 218, 143));
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        bottom.add(status, BorderLayout.NORTH);
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        log.setFocusable(false);
        log.setRows(2); log.setBackground(new Color(18, 39, 111));
        log.setForeground(Color.WHITE); log.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        log.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel info = frame();
        info.setLayout(new BorderLayout());
        logScroll.setBorder(null);
        logScroll.setFocusable(false);
        logScroll.getVerticalScrollBar().setFocusable(false);
        info.add(logScroll, BorderLayout.CENTER);
        JPanel members = new JPanel(new GridLayout(3, 1));
        members.setOpaque(false);
        for (int i = 0; i < partyStatus.length; i++) {
            JLabel member = partyStatus[i];
            member.setForeground(Color.WHITE);
            member.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            member.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
            JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
            JProgressBar bar = apBars[i]; bar.setPreferredSize(new Dimension(160, 18));
            bar.setForeground(new Color(102, 198, 223)); bar.setBackground(new Color(15, 26, 61));
            bar.setStringPainted(true); bar.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            row.add(member, BorderLayout.CENTER); row.add(bar, BorderLayout.EAST);
            members.add(row);
        }
        info.add(members, BorderLayout.SOUTH);
        bottom.add(info, BorderLayout.CENTER);
        JPanel actions = frame();
        actions.setLayout(new GridLayout(5, 1, 0, 1));
        actions.setPreferredSize(new Dimension(190, 160));
        for (JButton button : new JButton[] {attack, ability, item, defend, finish}) {
            button.setFocusable(false);
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(18, 39, 111));
            button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            button.setHorizontalAlignment(JButton.LEFT);
            button.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 8));
            button.setOpaque(true);
        }
        actions.add(attack); actions.add(ability); actions.add(item); actions.add(defend); actions.add(finish);
        bottom.add(actions, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
        attack.addActionListener(e -> beginTargeting(TargetAction.ATTACK));
        defend.addActionListener(e -> performAction(() -> { notice = null; battle.defend(); }, false));
        ability.addActionListener(e -> beginTargeting(TargetAction.ABILITY));
        item.addActionListener(e -> {
            if (battle.getKits() == 0) {
                notice = "No First-Aid Kits remain."; refresh(); return;
            }
            beginTargeting(TargetAction.ITEM);
        });
        finish.addActionListener(e -> { if (battle.isVictory()) { this.onVictory.accept(progress); } else { this.returnToMenu.run(); } });
        Keys.bind(this, "UP", "battle-up", () -> moveCursor(-1));
        Keys.bind(this, "DOWN", "battle-down", () -> moveCursor(1));
        Keys.bind(this, "LEFT", "battle-left", () -> moveTarget(-1));
        Keys.bind(this, "RIGHT", "battle-right", () -> moveTarget(1));
        Keys.bind(this, "ENTER", "battle-confirm", this::confirmCommand);
        Keys.bind(this, "SPACE", "battle-confirm-space", this::confirmCommand);
        Keys.bind(this, "ESCAPE", "battle-cancel", this::cancelTargeting);
        Keys.bind(this, "PAGE_DOWN", "log-down", () -> scrollLog(1));
        Keys.bind(this, "PAGE_UP", "log-up", () -> scrollLog(-1));
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing() && battle != null && !battle.isOver()) {
                    lastTickNanos = System.nanoTime();
                    apTimer.start();
                }
                else { apTimer.stop(); }
            }
        });
    }

    private void effectTimerStop() { effectTimer.stop(); }

    private void showActionEffects(Object attacker) {
        showEffects = !battle.getEffects().isEmpty();
        attackingSprite = attacker;
        if (showEffects || attacker != null) {
            effectStartedNanos = System.nanoTime();
            effectTimer.restart();
        }
    }

    private static JPanel frame() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(18, 39, 111));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 239), 3),
                BorderFactory.createLineBorder(new Color(63, 100, 166), 2)));
        return panel;
    }

    private void scrollLog(int direction) {
        javax.swing.JScrollBar bar = logScroll.getVerticalScrollBar();
        bar.setValue(bar.getValue() + direction * bar.getBlockIncrement(direction));
    }

    /** Begins a fresh first encounter with the selected students.
     * @param ids selected student indices
     */
    public void start(List<Integer> ids) {
        start(new GameProgress(ids));
    }

    /** Begins an encounter with the existing party and supplies.
     * @param progress party and encounter progress
     */
    public void start(GameProgress progress) {
        this.progress = progress;
        battle = new Battle(progress, new Random()); commandIndex = 0;
        targeting = TargetAction.NONE; targetIndex = 0; notice = null;
        apTimer.stop(); effectTimer.stop(); showEffects = false; attackingSprite = null;
        resetActionPoints();
        if (isShowing()) { lastTickNanos = System.nanoTime(); apTimer.start(); }
        refresh();
    }

    private void resetActionPoints() {
        actionPoints.clear(); enemyPoints.clear();
        for (Student student : battle.getParty()) { actionPoints.put(student, 0.0); }
        for (Enemy enemy : battle.getEnemies()) { enemyPoints.put(enemy, 0.0); }
    }

    private void tickActionPoints() {
        long now = System.nanoTime();
        double seconds = Math.max(0.0, (now - lastTickNanos) / 1_000_000_000.0);
        lastTickNanos = now;
        advanceActionPoints(seconds);
    }

    private void advanceActionPoints(double seconds) {
        if (battle == null || battle.isOver()) { apTimer.stop(); return; }
        // Wait mode: freeze clocks while a ready student chooses a command or target.
        if (battle.getCurrent() != null) { return; }
        boolean changed = false;
        double remaining = Math.max(0.0, seconds);
        double rate = settings.getApPerSecond();
        while (!battle.isOver()) {
            if (selectReadyStudent()) { changed = true; break; }
            boolean enemyActed = false;
            for (Enemy enemy : battle.getEnemies()) {
                if (enemy.isAlive() && enemyPoints.get(enemy) + AP_EPSILON >= enemy.getActionThreshold()) {
                    if (battle.enemyAttack(enemy)) {
                        enemyPoints.put(enemy, 0.0);
                        showActionEffects(enemy);
                        enemyActed = true;
                        changed = true;
                    }
                    break;
                }
            }
            if (enemyActed) { continue; }
            if (remaining <= 0.0) { break; }
            double untilReady = Double.POSITIVE_INFINITY;
            for (Student student : battle.getParty()) {
                if (student.isAlive()) { untilReady = Math.min(untilReady, (student.getActionThreshold() - actionPoints.get(student)) / rate); }
            }
            for (Enemy enemy : battle.getEnemies()) {
                if (enemy.isAlive()) { untilReady = Math.min(untilReady, (enemy.getActionThreshold() - enemyPoints.get(enemy)) / rate); }
            }
            double step = Math.min(remaining, untilReady);
            if (step <= 0.0 || !Double.isFinite(step)) { break; }
            for (Student student : battle.getParty()) {
                if (student.isAlive()) {
                    actionPoints.put(student, Math.min(student.getActionThreshold(), actionPoints.get(student) + rate * step));
                }
            }
            for (Enemy enemy : battle.getEnemies()) {
                if (enemy.isAlive()) {
                    enemyPoints.put(enemy, Math.min(enemy.getActionThreshold(), enemyPoints.get(enemy) + rate * step));
                }
            }
            remaining = Math.max(0.0, remaining - step);
        }
        if (battle.isOver()) { apTimer.stop(); }
        if (changed) { refresh(); }
        else { refreshBars(); scene.repaint(); }
    }

    private boolean selectReadyStudent() {
        if (battle.isOver() || battle.getCurrent() != null) { return false; }
        for (Student student : battle.getParty()) {
            if (student.isAlive() && actionPoints.get(student) + AP_EPSILON >= student.getActionThreshold()) {
                actionPoints.put(student, (double) student.getActionThreshold());
                return battle.selectStudent(student);
            }
        }
        return false;
    }

    private void performAction(Runnable action, boolean attacking) {
        if (battle == null || battle.isOver() || battle.getCurrent() == null
                || actionPoints.get(battle.getCurrent()) + AP_EPSILON < battle.getCurrent().getActionThreshold()) { return; }
        Student actor = battle.getCurrent();
        action.run();
        if (battle.getCurrent() != actor || battle.isOver()) {
            actionPoints.put(actor, 0.0);
            showActionEffects(attacking ? actor : null);
            if (!battle.isOver()) { selectReadyStudent(); }
        }
        if (battle.isOver()) { apTimer.stop(); }
        lastTickNanos = System.nanoTime();
        refresh();
    }

    private void moveCursor(int direction) {
        if (battle == null || battle.isOver()) { return; }
        if (targeting != TargetAction.NONE) { moveTarget(direction); return; }
        commandIndex = Math.floorMod(commandIndex + direction, commands.length);
        updateCommands();
    }

    private boolean targetsParty() {
        return targeting == TargetAction.ITEM
                || (targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 3);
    }

    private boolean targetsAllEnemies() {
        return targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 0;
    }

    private void beginTargeting(TargetAction action) {
        if (battle == null || battle.isOver() || battle.getCurrent() == null || targeting != TargetAction.NONE
                || actionPoints.get(battle.getCurrent()) + AP_EPSILON < battle.getCurrent().getActionThreshold()) { return; }
        notice = null; targeting = action; targetIndex = 0;
        if (targetsParty()) {
            while (!battle.getParty().get(targetIndex).isAlive()) { targetIndex++; }
        } else {
            while (!battle.getEnemies().get(targetIndex).isAlive()) { targetIndex++; }
        }
        refresh();
    }

    private void moveTarget(int direction) {
        if (battle == null || battle.isOver() || targeting == TargetAction.NONE || targetsAllEnemies()) { return; }
        int size = targetsParty() ? battle.getParty().size() : battle.getEnemies().size();
        do { targetIndex = Math.floorMod(targetIndex + direction, size); }
        while (targetsParty() ? !battle.getParty().get(targetIndex).isAlive() : !battle.getEnemies().get(targetIndex).isAlive());
        refresh();
    }

    private void cancelTargeting() {
        if (targeting != TargetAction.NONE) { targeting = TargetAction.NONE; notice = null; refresh(); }
    }

    private void confirmCommand() {
        if (battle == null) { return; }
        if (battle.isOver()) { finish.doClick(); }
        else if (targeting != TargetAction.NONE) {
            Student target = targetsParty() ? battle.getParty().get(targetIndex) : null;
            Enemy enemy = !targetsParty() ? battle.getEnemies().get(targetIndex) : null;
            TargetAction action = targeting;
            targeting = TargetAction.NONE;
            notice = null;
            boolean attacking = action == TargetAction.ATTACK
                    || (action == TargetAction.ABILITY && battle.getCurrent().getId() != 3);
            performAction(() -> {
                if (action == TargetAction.ATTACK) { battle.attack(enemy); }
                else if (action == TargetAction.ABILITY) { battle.ability(target, enemy); }
                else if (!battle.useKit(target)) { notice = target.getName() + " already has full HP."; }
            }, attacking);
        }
        else if (battle.getCurrent() != null) { commands[commandIndex].doClick(); }
    }

    private void updateCommands() {
        String[] names = {"Attack", "Ability", "Item", "Defend"};
        for (int i = 0; i < commands.length; i++) {
            commands[i].setText((i == commandIndex ? "▶ " : "  ") + names[i]);
        }
    }

    private void refresh() {
        boolean over = battle.isOver();
        refreshStatus();
        String targetMessage = targeting == TargetAction.NONE ? null : targetsParty()
                ? "Choose " + battle.getParty().get(targetIndex).getName() + " as the target."
                : targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 0
                ? "Arcane Burst targets all enemies." : "Choose " + battle.getEnemies().get(targetIndex).getName() + " as the target.";
        log.setText(targetMessage != null ? targetMessage : notice != null ? notice : battle.getMessage());
        log.setCaretPosition(0);
        for (int i = 0; i < partyStatus.length; i++) {
            Student student = battle.getParty().get(i);
            String marker = !over && student == battle.getCurrent() ? "▶ " : "  ";
            partyStatus[i].setText(String.format("%s%-9s  HP %3d / %3d%s", marker,
                    student.getName(), student.getHp(), student.getMaxHp(), student.isAlive() ? "" : "  KO"));
        }
        refreshBars();
        boolean canChoose = !over && targeting == TargetAction.NONE && battle.getCurrent() != null;
        attack.setEnabled(canChoose); ability.setEnabled(canChoose); item.setEnabled(canChoose); defend.setEnabled(canChoose);
        finish.setVisible(over);
        finish.setText(battle.isVictory() ? "Continue" : "Return to Menu");
        updateCommands();
        scene.repaint();
    }

    private void refreshStatus() {
        boolean over = battle.isOver();
        String hint = targeting == TargetAction.NONE ? "↑ ↓ Move  •  Enter Select"
                : targetsParty() ? "← → Choose ally  •  Enter Confirm  •  Esc Cancel"
                : targetsAllEnemies() ? "Enter Confirm  •  Esc Cancel" : "← → Choose enemy  •  Enter Confirm  •  Esc Cancel";
        status.setText(over ? (battle.isVictory() ? "VICTORY  •  Enter: Continue" : "GAME OVER  •  Enter: Return to Menu")
                : (battle.getCurrent() == null ? "Waiting for AP" : battle.getCurrent().getName() + " is ready")
                + "  |  First-Aid Kits: " + battle.getKits() + "  |  " + hint);
    }

    private void refreshBars() {
        for (int i = 0; i < apBars.length; i++) {
            Student student = battle.getParty().get(i);
            double value = actionPoints.get(student);
            apBars[i].setMaximum(student.getActionThreshold() * AP_DISPLAY_SCALE);
            apBars[i].setValue((int) (value * AP_DISPLAY_SCALE));
            apBars[i].setString(!student.isAlive() ? "KO" : (int) value + " / " + student.getActionThreshold() + " AP");
        }
    }

}
