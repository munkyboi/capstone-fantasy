package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.Student;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
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
    private static final int EFFECT_MILLIS = 1600;
    private static final int AMBUSH_ALERT_MILLIS = 3000;
    private static final double AMBUSH_ENEMY_AP_FRACTION = 0.9;
    private static final int ANIMATION_FRAME_MILLIS = 50;
    private static final int VICTORY_ANIMATION_MILLIS = 1800;
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
    private final Timer ambushAlertTimer;
    private final Timer victoryTimer;
    private long victoryStartedNanos;
    private boolean showAmbush;
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
            @Override public boolean showAmbush() { return showAmbush; }
            @Override public double victoryProgress() {
                return victoryStartedNanos == 0 ? 1.0 : Math.min(1.0,
                        (System.nanoTime() - victoryStartedNanos) / (VICTORY_ANIMATION_MILLIS * 1_000_000.0));
            }
            @Override public double enemyPoints(Enemy enemy) { return enemyPoints.getOrDefault(enemy, 0.0); }
            @Override public Object attackingSprite() { return attackingSprite; }
            @Override public double effectProgress() {
                return showEffects ? Math.min(1.0, (System.nanoTime() - effectStartedNanos) / (EFFECT_MILLIS * 1_000_000.0)) : 1.0;
            }
        });
        victoryTimer = new Timer(ANIMATION_FRAME_MILLIS, event -> {
            if ((System.nanoTime() - victoryStartedNanos) / 1_000_000.0 >= VICTORY_ANIMATION_MILLIS) {
                ((Timer) event.getSource()).stop();
            }
            scene.repaint();
        });
        ambushAlertTimer = new Timer(AMBUSH_ALERT_MILLIS, event -> {
            showAmbush = false;
            scene.repaint();
        });
        ambushAlertTimer.setRepeats(false);
        effectTimer = new Timer(ANIMATION_FRAME_MILLIS, event -> {
            if ((System.nanoTime() - effectStartedNanos) / 1_000_000.0 >= EFFECT_MILLIS) {
                showEffects = false; attackingSprite = null; effectTimerStop();
            }
            scene.repaint();
        });
        BattleControls.build(this, scene, status, partyStatus, apBars, log, logScroll,
                attack, ability, item, defend, finish);
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
                else { apTimer.stop(); victoryTimer.stop(); }
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
        start(progress, false);
    }

    /** Begins an encounter with full AP, or empty AP when the party is ambushed.
     * @param progress party and encounter progress
     * @param ambushed whether students start with empty AP bars
     */
    public void start(GameProgress progress, boolean ambushed) {
        this.progress = progress;
        battle = new Battle(progress, new Random(), ambushed); commandIndex = 0;
        targeting = TargetAction.NONE; targetIndex = 0; notice = null;
        apTimer.stop(); effectTimer.stop(); ambushAlertTimer.stop(); victoryTimer.stop();
        victoryStartedNanos = 0;
        showEffects = false; attackingSprite = null;
        showAmbush = ambushed;
        if (ambushed) { ambushAlertTimer.restart(); }
        resetActionPoints(ambushed);
        if (!ambushed) { selectReadyStudent(); }
        if (isShowing()) { lastTickNanos = System.nanoTime(); apTimer.start(); }
        refresh();
    }

    private void resetActionPoints(boolean ambushed) {
        actionPoints.clear(); enemyPoints.clear();
        for (Student student : battle.getParty()) {
            actionPoints.put(student, ambushed || !student.isAlive() ? 0.0 : (double) student.getActionThreshold());
        }
        for (Enemy enemy : battle.getEnemies()) {
            enemyPoints.put(enemy, ambushed ? enemy.getActionThreshold() * AMBUSH_ENEMY_AP_FRACTION : 0.0);
        }
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
        else { BattleControls.updateBars(battle, apBars, actionPoints); scene.repaint(); }
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
        BattleControls.updateCommands(commands, commandIndex);
    }

    private boolean targetsParty() {
        return targeting == TargetAction.ITEM
                || (targeting == TargetAction.ABILITY && battle.getCurrent().getId() == Student.ANGEL);
    }

    private boolean targetsAllEnemies() {
        return targeting == TargetAction.ABILITY && battle.getCurrent().getId() == Student.ZACK;
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
                    || (action == TargetAction.ABILITY && battle.getCurrent().getId() != Student.ANGEL);
            performAction(() -> {
                if (action == TargetAction.ATTACK) { battle.attack(enemy); }
                else if (action == TargetAction.ABILITY) { battle.ability(target, enemy); }
                else if (!battle.useKit(target)) { notice = target.getName() + " already has full HP."; }
            }, attacking);
        }
        else if (battle.getCurrent() != null) { commands[commandIndex].doClick(); }
    }

    private void refresh() {
        boolean over = battle.isOver();
        if (over) {
            showAmbush = false;
            ambushAlertTimer.stop();
            if (battle.isVictory() && victoryStartedNanos == 0) {
                victoryStartedNanos = System.nanoTime();
                victoryTimer.start();
            }
        }
        refreshStatus();
        String targetMessage = targeting == TargetAction.NONE ? null : targetsParty()
                ? "Choose " + battle.getParty().get(targetIndex).getName() + " as the target."
                : targeting == TargetAction.ABILITY && battle.getCurrent().getId() == Student.ZACK
                ? "Arcane Burst targets all enemies." : "Choose " + battle.getEnemies().get(targetIndex).getName() + " as the target.";
        log.setText(targetMessage != null ? targetMessage : notice != null ? notice : battle.getMessage());
        log.setCaretPosition(0);
        BattleControls.updateParty(battle, partyStatus);
        BattleControls.updateBars(battle, apBars, actionPoints);
        boolean canChoose = !over && targeting == TargetAction.NONE && battle.getCurrent() != null;
        attack.setEnabled(canChoose); ability.setEnabled(canChoose); item.setEnabled(canChoose); defend.setEnabled(canChoose);
        finish.setVisible(over);
        finish.setText(battle.isVictory() ? "Continue" : "Return to Menu");
        BattleControls.updateCommands(commands, commandIndex);
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



}
