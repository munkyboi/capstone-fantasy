package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameSettings;
import game.model.Student;
import java.util.Map;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/** Advances action bars and schedules ready enemy actions. */
final class BattleClock {
    private static final double AP_EPSILON = 0.0000001;
    private final BattlePanel panel;
    private final GameSettings settings;
    private final Map<Student, Double> actionPoints;
    private final Map<Enemy, Double> enemyPoints;
    private final JProgressBar[] apBars;
    private final Timer apTimer, monsterTurnTimer, sparkExplosionTimer;
    private final BattleScene scene;

    BattleClock(BattlePanel panel, GameSettings settings, Map<Student, Double> actionPoints,
            Map<Enemy, Double> enemyPoints, JProgressBar[] apBars, Timer apTimer,
            Timer monsterTurnTimer, Timer sparkExplosionTimer, BattleScene scene) {
        this.panel = panel; this.settings = settings; this.actionPoints = actionPoints;
        this.enemyPoints = enemyPoints; this.apBars = apBars; this.apTimer = apTimer;
        this.monsterTurnTimer = monsterTurnTimer; this.sparkExplosionTimer = sparkExplosionTimer;
        this.scene = scene;
    }
    void reset(boolean ambushed) {
        Battle battle = panel.battleForClock(); actionPoints.clear(); enemyPoints.clear();
        for (Student student : battle.getParty()) actionPoints.put(student, ambushed || !student.isAlive() ? 0.0 : (double)student.getActionThreshold());
        for (Enemy enemy : battle.getEnemies()) enemyPoints.put(enemy, ambushed ? enemy.getActionThreshold() * 0.8 : 0.0);
    }
    void tick() {
        double[] elapsed = panel.clockElapsed(System.nanoTime());
        Battle battle = panel.battleForClock(); Student selected = battle == null ? null : battle.getCurrent();
        if (battle != null && battle.advanceDamageOverTime(elapsed[1])) {
            battle.cancelEscapeIfKnockedOut();
            if (selected != null && !selected.isAlive()) { panel.resetStudentAp(selected); panel.clockClearTarget(); }
            panel.showActionEffects(null); panel.refresh();
        }
        advance(elapsed[0]);
    }
    void advance(double seconds) {
        Battle battle = panel.battleForClock();
        if (battle == null || battle.isOver()) { apTimer.stop(); return; }
        if (battle.cancelEscapeIfKnockedOut()) panel.refresh();
        if (battle.hasPendingSparkExplosion() || battle.hasPendingGuardianLoop()) return;
        // Wait mode: freeze clocks while a ready student chooses a command or target.
        if (battle.getCurrent() != null) {
            if (battle.isEscaping() || battle.getParty().stream().anyMatch(student -> student.isAlive() && battle.getStunTurns(student) > 0)) scene.repaint();
            return;
        }
        boolean changed = false;
        double remaining = Math.max(0.0, seconds);
        double rate = settings.getApPerSecond();
        while (!battle.isOver()) {
            if (panel.selectReadyStudent()) { changed = true; break; }
            boolean enemyActed = false;
            for (Enemy enemy : battle.getEnemies()) {
                if (!monsterTurnTimer.isRunning() && enemy.isAlive() && enemyPoints.get(enemy) + AP_EPSILON >= enemy.getActionThreshold()) {
                    if (battle.enemyAttack(enemy)) {
                        battle.cancelEscapeIfKnockedOut();
                        panel.resetEnemyAp(enemy);
                        enemyPoints.keySet().retainAll(battle.getEnemies());
                        for (Enemy spawned : battle.getEnemies()) enemyPoints.putIfAbsent(spawned, 0.0);
                        panel.applyApRefills();
                        Battle.AbilityAnnouncement enemyAbility = battle.getLastAbility();
                        boolean healing = enemyAbility != null && ("Stone Mend".equals(enemyAbility.skill()) || "Stone Skin".equals(enemyAbility.skill()) || "Recall".equals(enemyAbility.skill()));
                        panel.showActionEffects(healing ? null : enemy);
                        panel.announceAbility();
                        if (battle.hasPendingSparkExplosion() || battle.hasPendingGuardianLoop()) sparkExplosionTimer.restart();
                        if (!battle.isOver()) monsterTurnTimer.restart();
                        enemyActed = true;
                        changed = true;
                    }
                    break;
                }
            }
            if (battle.hasPendingSparkExplosion() || battle.hasPendingGuardianLoop()) break;
            if (enemyActed) { break; }
            if (remaining <= 0.0) { break; }
            double untilReady = Double.POSITIVE_INFINITY;
            for (Student student : battle.getParty()) {
                if (student.isAlive() && (!battle.isEscaping() || actionPoints.get(student) + AP_EPSILON < student.getActionThreshold())) {
                    untilReady = Math.min(untilReady, (student.getActionThreshold() - actionPoints.get(student)) / studentRate(student, rate));
                }
            }
            for (Enemy enemy : battle.getEnemies()) {
                if (enemy.isAlive() && (!monsterTurnTimer.isRunning() || enemyPoints.get(enemy) + AP_EPSILON < enemy.getActionThreshold())) {
                    untilReady = Math.min(untilReady, (enemy.getActionThreshold() - enemyPoints.get(enemy)) / rate);
                }
            }
            double step = Math.min(remaining, untilReady);
            if (step <= 0.0 || !Double.isFinite(step)) { break; }
            java.util.List<Student> newlyReady = new java.util.ArrayList<>();
            for (Student student : battle.getParty()) {
                if (student.isAlive()) {
                    double before = actionPoints.get(student);
                    double after = Math.min(student.getActionThreshold(), before + studentRate(student, rate) * step);
                    actionPoints.put(student, after);
                    if (before + AP_EPSILON < student.getActionThreshold() && after + AP_EPSILON >= student.getActionThreshold()) newlyReady.add(student);
                }
            }
            for (Enemy enemy : battle.getEnemies()) {
                if (enemy.isAlive()) {
                    double before = enemyPoints.get(enemy);
                    double after = Math.min(enemy.getActionThreshold(), before + rate * step);
                    enemyPoints.put(enemy, after);
                }
            }
            battle.cancelEscapeIfKnockedOut();
            newlyReady.removeIf(student -> !student.isAlive());
            if (!newlyReady.isEmpty() && !battle.isOver() && battle.regenerate(newlyReady, false)) {
                panel.showActionEffects(null); changed = true;
            }
            remaining = Math.max(0.0, remaining - step);
        }
        if (battle.isOver()) { apTimer.stop(); }
        if (changed) { panel.refresh(); }
        else { BattleControls.updateBars(battle, apBars, actionPoints); scene.repaint(); }
    }

    static double studentRate(Student student, double baseRate) {
        // A fixed 5% per SPD avoids disproportionately large gains for low-SPD classes.
        double speedBonus = (student.getEffectiveSpeed() - student.getStartingSpeed()) * 0.05;
        return baseRate * Math.max(0.1, 1.0 + speedBonus);
    }

    void applyApRefills() {
        Battle battle = panel.battleForClock();
        java.util.List<game.model.BattleEffect> combined = new java.util.ArrayList<>(battle.getEffects());
        java.util.List<Student> refilled = new java.util.ArrayList<>();
        for (Student student : battle.consumeApRefills()) {
            if (!student.isAlive()) continue;
            if (actionPoints.get(student) + AP_EPSILON < student.getActionThreshold()) {
                if (student.isAlive()) refilled.add(student);
            }
            if (student.isAlive()) actionPoints.put(student, (double) student.getActionThreshold());
            else panel.resetStudentAp(student);
        }
        battle.replaceEffects(combined);
        battle.cancelEscapeIfKnockedOut();
        if (!refilled.isEmpty() && !battle.isOver()) battle.regenerate(refilled, true);
    }

    boolean selectReadyStudent() {
        Battle battle = panel.battleForClock();
        if (battle.isOver() || battle.getCurrent() != null) { return false; }
        for (Student student : battle.getParty()) {
            if (battle.isOver()) return false;
            if (student.isAlive() && actionPoints.get(student) + AP_EPSILON >= student.getActionThreshold()) {
                if (battle.isEscaping() && student != battle.getEscapeInitiator()) {
                    panel.resetStudentAp(student); continue;
                }
                if (battle.consumeStunTurn(student)) {
                    panel.resetStudentAp(student); panel.showActionEffects(null); panel.refresh(); continue;
                }
                if (student == battle.getEscapeInitiator()) {
                    panel.resetStudentAp(student);
                    if (battle.advanceEscape(student)) { apTimer.stop(); panel.escapeComplete(); return true; }
                    panel.refresh(); continue;
                }
                actionPoints.put(student, (double) student.getActionThreshold());
                return battle.selectStudent(student);
            }
        }
        return false;
    }

}
