package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.GameCharacter;
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
    private static final int AP_FRAME_MILLIS = 16;
    private static final double AP_EPSILON = 0.0000001;
    private static final int EFFECT_MILLIS = 1600;
    private static final int SECOND_CAST_DELAY_MILLIS = 700;
    private static final int LOOPER_HIT_DELAY_MILLIS = 300;
    private static final int MONSTER_TURN_DELAY_MILLIS = 400;
    private static final int AMBUSH_ALERT_MILLIS = 3000;
    private static final double AMBUSH_ENEMY_AP_FRACTION = 0.8;
    private static final int ANIMATION_FRAME_MILLIS = 16;
    private static final int VICTORY_ANIMATION_MILLIS = 1800;
    private final Runnable returnToMenu;
    private final Consumer<GameProgress> onVictory;
    private Consumer<GameProgress> onEscape;
    private java.util.function.BiConsumer<GameProgress, Integer> onExplore;
    private int victoryChoice;
    private final GameSettings settings;
    private final Random battleRandom;
    private final JLabel status = new JLabel();
    private final JLabel[] partyStatus = {new JLabel(), new JLabel(), new JLabel(), new JLabel()};
    private final JProgressBar[] apBars = {new JProgressBar(), new JProgressBar(), new JProgressBar(), new JProgressBar()};
    private final JTextArea log = new JTextArea();
    private final JScrollPane logScroll = new JScrollPane(log);
    private final JButton attack = new JButton("Attack");
    private final JButton ability = new JButton("Ability");
    private final JButton item = new JButton("Item");
    private final JButton defend = new JButton("Defend");
    private final JButton escape = new JButton("Escape");
    private final JButton finish = new JButton("Return to Menu");
    private final BattleScene scene;
    private Battle battle;
    private GameProgress progress;
    private int commandIndex;
    private final JButton[] commands = {attack, ability, item, defend, escape};
    private enum TargetAction { NONE, ATTACK, ABILITY, ITEM, WHITE_FLOWER, DEFEND }
    private TargetAction targeting = TargetAction.NONE;
    private int targetIndex;
    private boolean choosingItem;
    private int itemChoice;
    private Battle.AbilityAnnouncement abilityAnnouncement;
    private final java.util.ArrayDeque<Battle.AbilityAnnouncement> abilityAlerts = new java.util.ArrayDeque<>();
    private final Timer abilityAlertTimer;
    private String notice;
    private List<String> victoryRewardItems = List.of();
    private boolean rewardRecorded;
    private int victoryPetot;
    private int victoryExperience;
    private List<String> leveledStudents = List.of();
    private List<String> bonusXpStudents = List.of();
    private int encounterStage;
    private final Map<Student, Double> actionPoints = new HashMap<>();
    private final Map<Enemy, Double> enemyPoints = new HashMap<>();
    private final Timer apTimer = new Timer(AP_FRAME_MILLIS, event -> tickActionPoints());
    private long lastTickNanos;
    private long lastDotNanos;
    private final Timer effectTimer;
    private final Timer ambushAlertTimer;
    private final Timer victoryTimer;
    private final Timer secondCastTimer;
    private final Timer looperHitTimer;
    private final Timer angelAttackTimer;
    private final Timer twinShotTimer;
    private final Timer sparkExplosionTimer;
    private final Timer monsterTurnTimer;
    private long victoryStartedNanos;
    private boolean showAmbush;
    private boolean showEffects;
    private final java.util.List<BattleScene.EffectBatch> effectBatches = new java.util.ArrayList<>();
    private int effectSequence;
    private long effectStartedNanos;
    private Object attackingSprite;
    private Student rainShooter;
    private List<Enemy> rainTargets = List.of();
    private long rainStartedNanos;

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
        this(returnToMenu, onVictory, settings, new Random());
    }

    /** Creates a battle screen with a supplied random source.
     * @param returnToMenu returns after defeat
     * @param onVictory opens the next screen after victory
     * @param settings shared gameplay settings
     * @param random random source for encounters and combat
     */
    public BattlePanel(Runnable returnToMenu, Consumer<GameProgress> onVictory, GameSettings settings, Random random) {
        this.returnToMenu = returnToMenu;
        this.onVictory = onVictory;
        this.onEscape = ignored -> returnToMenu.run();
        this.settings = settings;
        this.battleRandom = random;
        scene = new BattleScene(new BattleScene.State() {
            @Override public Battle battle() { return battle; }
            @Override public int encounterStage() { return encounterStage; }
            @Override public boolean partyTarget(int index) {
                return targeting != TargetAction.NONE && targetsParty() && index == targetIndex;
            }
            @Override public boolean enemyTarget(int index) {
                return targeting != TargetAction.NONE && !targetsParty()
                        && (targetsAllEnemies() || index == targetIndex);
            }
            @Override public List<String> rewardItems() { return victoryRewardItems; }
            @Override public int rewardPetot() { return victoryPetot; }
            @Override public int rewardExperience() { return victoryExperience; }
            @Override public int victoryChoice() { return victoryChoice; }
            @Override public boolean canExplore() { return !battle.isMerchantEncounter() && onExplore != null; }
            @Override public void chooseVictory(int choice) {
                victoryChoice = choice;
                confirmCommand();
            }
            @Override public List<String> leveledStudents() { return leveledStudents; }
            @Override public List<String> bonusXpStudents() { return bonusXpStudents; }
            @Override public boolean showEffects() { return showEffects; }
            @Override public List<BattleScene.EffectBatch> effectBatches() { return List.copyOf(effectBatches); }
            @Override public boolean escaping() { return battle != null && battle.isEscaping(); }
            @Override public boolean showAmbush() { return showAmbush; }
            @Override public Battle.AbilityAnnouncement abilityAnnouncement() { return abilityAnnouncement; }
            @Override public boolean choosingItem() { return choosingItem; }
            @Override public int itemChoice() { return itemChoice; }
            @Override public int kits() { return battle == null ? 0 : battle.getKits(); }
            @Override public int flowers() { return progress == null ? 0 : progress.getInventory().getWhiteFlowers(); }
            @Override public double victoryProgress() {
                return victoryStartedNanos == 0 ? 1.0 : Math.min(1.0,
                        (System.nanoTime() - victoryStartedNanos) / (VICTORY_ANIMATION_MILLIS * 1_000_000.0));
            }
            @Override public double enemyPoints(Enemy enemy) { return enemyPoints.getOrDefault(enemy, 0.0); }
            @Override public Object attackingSprite() { return attackingSprite; }
            @Override public Student rainShooter() { return rainShooter; }
            @Override public List<Enemy> rainTargets() { return rainTargets; }
            @Override public double rainElapsedMillis() { return (System.nanoTime() - rainStartedNanos) / 1_000_000.0; }
            @Override public Student blockingStudent() { return battle == null || !(attackingSprite instanceof Enemy) ? null : battle.getBlockingStudent(); }
            @Override public Student protectedStudent() { return battle == null || !(attackingSprite instanceof Enemy) ? null : battle.getProtectedStudent(); }
            @Override public double effectProgress() {
                return attackingSprite != null ? Math.min(1.0, (System.nanoTime() - effectStartedNanos) / (EFFECT_MILLIS * 1_000_000.0)) : 1.0;
            }
        });
        abilityAlertTimer = new Timer(2000, event -> {
            abilityAnnouncement = abilityAlerts.pollFirst();
            if (abilityAnnouncement != null) ((Timer) event.getSource()).restart();
            scene.repaint();
        });
        abilityAlertTimer.setRepeats(false);
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
            long now = System.nanoTime();
            effectBatches.removeIf(batch -> now - batch.startedNanos() >= EFFECT_MILLIS * 1_000_000L);
            showEffects = !effectBatches.isEmpty();
            if ((now - effectStartedNanos) / 1_000_000.0 >= EFFECT_MILLIS) attackingSprite = null;
            if (rainShooter != null && (now - rainStartedNanos) / 1_000_000.0 >= 800) rainShooter = null;
            if (!showEffects && attackingSprite == null && rainShooter == null) effectTimerStop();
            scene.repaint();
        });
        secondCastTimer = new Timer(SECOND_CAST_DELAY_MILLIS, event -> completeSecondCast());
        secondCastTimer.setRepeats(false);
        looperHitTimer = new Timer(LOOPER_HIT_DELAY_MILLIS, event -> advanceLooperHit());
        angelAttackTimer = new Timer(300, event -> advanceAngelAttack());
        angelAttackTimer.setRepeats(false);
        twinShotTimer = new Timer(300, event -> advanceTwinShot());
        twinShotTimer.setRepeats(false);
        sparkExplosionTimer = new Timer(300, event -> resolveSparkExplosion());
        sparkExplosionTimer.setRepeats(false);
        monsterTurnTimer = new Timer(MONSTER_TURN_DELAY_MILLIS, event -> { });
        monsterTurnTimer.setRepeats(false);
        BattleControls.build(this, scene, status, partyStatus, apBars, log, logScroll,
                attack, ability, item, defend, escape, finish);
        attack.addActionListener(e -> {
            Student actor = battle.getCurrent();
            if (actor != null && actor.getId() == Student.CHARICE && actor.getSp() < 2) {
                notice = "Not enough SP. Twin Shot needs 2 SP."; refresh(); return;
            }
            beginTargeting(TargetAction.ATTACK);
        });
        escape.addActionListener(e -> performAction(() -> battle.beginEscape(), false));
        defend.addActionListener(e -> beginTargeting(TargetAction.DEFEND));
        ability.addActionListener(e -> {
            Student actor = battle.getCurrent();
            if (actor != null && actor.getSp() < actor.getAbilitySpCost()) {
                notice = "Not enough SP. " + actor.getName() + " needs " + actor.getAbilitySpCost() + " SP.";
                refresh(); return;
            }
            beginTargeting(TargetAction.ABILITY);
        });
        item.addActionListener(e -> {
            int kits = battle.getKits(), flowers = progress.getInventory().getWhiteFlowers();
            if (kits == 0 && flowers == 0) { notice = "No usable items remain."; refresh(); return; }
            boolean fallen = battle.getParty().stream().anyMatch(student -> !student.isAlive());
            if (flowers == 0 || (kits > 0 && !fallen)) { beginTargeting(TargetAction.ITEM); return; }
            if (kits == 0) { beginFlowerTargeting(); return; }
            choosingItem = true; itemChoice = 0; notice = null; refresh();
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
                    lastTickNanos = System.nanoTime(); lastDotNanos = lastTickNanos;
                    apTimer.start();
                    if (battle.hasPendingSparkExplosion() || battle.hasPendingGuardianLoop()) sparkExplosionTimer.restart();
                    else if (battle.hasPendingLooperHits()) looperHitTimer.restart();
                    else if (battle.hasPendingAngelAttack()) scheduleAngelAttack();
                    else if (battle.hasPendingTwinShot()) scheduleTwinShot();
                    else if (battle.hasPendingSecondCast()) secondCastTimer.restart();
                }
                else { apTimer.stop(); victoryTimer.stop(); secondCastTimer.stop(); looperHitTimer.stop(); angelAttackTimer.stop(); twinShotTimer.stop(); sparkExplosionTimer.stop(); monsterTurnTimer.stop(); abilityAlertTimer.stop(); abilityAnnouncement = null; abilityAlerts.clear(); }
            }
        });
    }
    /** Sets the destination for a successful party escape. */
    public void setEscapeAction(Consumer<GameProgress> action) { onEscape = java.util.Objects.requireNonNull(action); }

    private void effectTimerStop() { effectTimer.stop(); }

    private void showActionEffects(Object attacker) {
        long now = System.nanoTime();
        if (!battle.getEffects().isEmpty()) {
            int[] lanes = {0, -20, 20, -36, 36};
            effectBatches.add(new BattleScene.EffectBatch(List.copyOf(battle.getEffects()), now, lanes[effectSequence++ % lanes.length]));
            if (effectBatches.size() > 80) effectBatches.remove(0);
        }
        showEffects = !effectBatches.isEmpty();
        attackingSprite = attacker;
        if (showEffects || attacker != null) {
            effectStartedNanos = now;
            if (!effectTimer.isRunning()) effectTimer.start();
        }
    }

    private void announceAbility() {
        Battle.AbilityAnnouncement latest = battle.getLastAbility();
        if (latest == null) return;
        if (abilityAnnouncement != null) { abilityAlerts.addLast(latest); return; }
        abilityAnnouncement = latest; abilityAlertTimer.restart(); scene.repaint();
    }
    private void beginFlowerTargeting() {
        if (battle.getParty().stream().noneMatch(student -> !student.isAlive())) {
            notice = "No fallen ally needs a White Flower."; refresh(); return;
        }
        beginTargeting(TargetAction.WHITE_FLOWER);
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
        start(progress, ambushed, progress.getEncounter());
    }

    /** Starts a specific unlocked stage, including a replay. */
    public void start(GameProgress progress, boolean ambushed, int stage) {
        startEncounter(progress, ambushed, stage, false);
    }
    public void startMerchant(GameProgress progress) { startEncounter(progress, false, 2, true); }
    /** Sets the action used to replay the current stage after victory. */
    public void setExploreAction(java.util.function.BiConsumer<GameProgress, Integer> action) { onExplore = action; }
    private void startEncounter(GameProgress progress, boolean ambushed, int stage, boolean merchant) {
        ambushed = ambushed && settings.allowsAmbush(stage);
        this.progress = progress; encounterStage = stage;
        battle = merchant ? Battle.merchantBattle(progress, battleRandom, settings.getDifficulty()) : new Battle(progress, battleRandom, ambushed, stage, settings.getDifficulty()); commandIndex = 0;
        targeting = TargetAction.NONE; targetIndex = 0; choosingItem = false; itemChoice = 0; notice = null;
        abilityAnnouncement = null; abilityAlerts.clear(); abilityAlertTimer.stop();
        victoryRewardItems = List.of(); rewardRecorded = false; victoryPetot = 0; victoryExperience = 0; leveledStudents = List.of(); bonusXpStudents = List.of(); victoryChoice = 1;
        apTimer.stop(); effectTimer.stop(); ambushAlertTimer.stop(); victoryTimer.stop(); secondCastTimer.stop(); looperHitTimer.stop(); angelAttackTimer.stop(); twinShotTimer.stop(); sparkExplosionTimer.stop(); monsterTurnTimer.stop();
        victoryStartedNanos = 0;
        showEffects = false; attackingSprite = null; rainShooter = null; rainTargets = List.of(); effectBatches.clear(); effectSequence = 0;
        showAmbush = ambushed;
        if (ambushed) { ambushAlertTimer.restart(); }
        resetActionPoints(ambushed);
        if (!ambushed) { selectReadyStudent(); }
        if (isShowing()) { lastTickNanos = System.nanoTime(); lastDotNanos = lastTickNanos; apTimer.start(); }
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

    private void resetStudentAp(Student student) { actionPoints.put(student, 0.0); }
    private void resetEnemyAp(Enemy enemy) { enemyPoints.put(enemy, 0.0); }
    private void applyApRefills() {
        java.util.List<game.model.BattleEffect> combined = new java.util.ArrayList<>(battle.getEffects());
        java.util.List<Student> refilled = new java.util.ArrayList<>();
        for (Student student : battle.consumeApRefills()) {
            if (!student.isAlive()) continue;
            if (actionPoints.get(student) + AP_EPSILON < student.getActionThreshold()) {
                if (student.isAlive()) refilled.add(student);
            }
            if (student.isAlive()) actionPoints.put(student, (double) student.getActionThreshold());
            else resetStudentAp(student);
        }
        battle.replaceEffects(combined);
        battle.cancelEscapeIfKnockedOut();
        if (!refilled.isEmpty() && !battle.isOver()) battle.regenerate(refilled, true);
    }

    private void tickActionPoints() {
        long now = System.nanoTime();
        double seconds = Math.max(0.0, (now - lastTickNanos) / 1_000_000_000.0);
        double dotSeconds = Math.max(0.0, (now - lastDotNanos) / 1_000_000_000.0);
        lastTickNanos = now; lastDotNanos = now;
        Student selected = battle == null ? null : battle.getCurrent();
        if (battle != null && battle.advanceDamageOverTime(dotSeconds)) {
            battle.cancelEscapeIfKnockedOut();
            if (selected != null && !selected.isAlive()) { resetStudentAp(selected); targeting = TargetAction.NONE; choosingItem = false; }
            showActionEffects(null);
            refresh();
        }
        advanceActionPoints(seconds);
    }

    private void advanceActionPoints(double seconds) {
        if (battle == null || battle.isOver()) { apTimer.stop(); return; }
        if (battle.cancelEscapeIfKnockedOut()) refresh();
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
            if (selectReadyStudent()) { changed = true; break; }
            boolean enemyActed = false;
            for (Enemy enemy : battle.getEnemies()) {
                if (!monsterTurnTimer.isRunning() && enemy.isAlive() && enemyPoints.get(enemy) + AP_EPSILON >= enemy.getActionThreshold()) {
                    if (battle.enemyAttack(enemy)) {
                        battle.cancelEscapeIfKnockedOut();
                        resetEnemyAp(enemy);
                        enemyPoints.keySet().retainAll(battle.getEnemies());
                        for (Enemy spawned : battle.getEnemies()) enemyPoints.putIfAbsent(spawned, 0.0);
                        applyApRefills();
                        Battle.AbilityAnnouncement enemyAbility = battle.getLastAbility();
                        boolean healing = enemyAbility != null && ("Stone Mend".equals(enemyAbility.skill()) || "Stone Skin".equals(enemyAbility.skill()) || "Recall".equals(enemyAbility.skill()));
                        showActionEffects(healing ? null : enemy);
                        announceAbility();
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
                showActionEffects(null); changed = true;
            }
            remaining = Math.max(0.0, remaining - step);
        }
        if (battle.isOver()) { apTimer.stop(); }
        if (changed) { refresh(); }
        else { BattleControls.updateBars(battle, apBars, actionPoints); scene.repaint(); }
    }

    private double studentRate(Student student, double baseRate) {
        // A fixed 5% per SPD avoids disproportionately large gains for low-SPD classes.
        double speedBonus = (student.getEffectiveSpeed() - student.getStartingSpeed()) * 0.05;
        return baseRate * Math.max(0.1, 1.0 + speedBonus);
    }

    private boolean selectReadyStudent() {
        if (battle.isOver() || battle.getCurrent() != null) { return false; }
        for (Student student : battle.getParty()) {
            if (battle.isOver()) return false;
            if (student.isAlive() && actionPoints.get(student) + AP_EPSILON >= student.getActionThreshold()) {
                if (battle.isEscaping() && student != battle.getEscapeInitiator()) {
                    resetStudentAp(student); continue;
                }
                if (battle.consumeStunTurn(student)) {
                    resetStudentAp(student); showActionEffects(null); refresh(); continue;
                }
                if (student == battle.getEscapeInitiator()) {
                    resetStudentAp(student);
                    if (battle.advanceEscape(student)) { apTimer.stop(); onEscape.accept(progress); return true; }
                    refresh(); continue;
                }
                actionPoints.put(student, (double) student.getActionThreshold());
                return battle.selectStudent(student);
            }
        }
        return false;
    }

    private void performAction(Runnable action, boolean attacking) {
        if (battle == null || battle.isOver() || battle.isActionAnimating() || battle.getCurrent() == null
                || actionPoints.get(battle.getCurrent()) + AP_EPSILON < battle.getCurrent().getActionThreshold()) { return; }
        Student actor = battle.getCurrent();
        action.run();
        if (attacking && actor.getId() == Student.CHARICE && battle.getLastAbility() != null
                && "Rain of Arrows".equals(battle.getLastAbility().skill())) {
            rainShooter = actor; rainTargets = battle.getRainArrowTargets(); rainStartedNanos = System.nanoTime();
        }
        if (battle.hasPendingTwinShot()) {
            resetStudentAp(actor);
            showActionEffects(attacking ? actor : null);
            scheduleTwinShot();
        } else if (battle.hasPendingAngelAttack()) {
            resetStudentAp(actor);
            showActionEffects(attacking ? actor : null);
            scheduleAngelAttack();
        } else if (battle.hasPendingLooperHits()) {
            resetStudentAp(actor);
            showActionEffects(attacking ? actor : null);
            looperHitTimer.restart();
        } else if (battle.hasPendingSecondCast()) {
            resetStudentAp(actor);
            showActionEffects(attacking ? actor : null);
            secondCastTimer.restart();
        } else if (battle.getCurrent() != actor || battle.isOver()) {
            resetStudentAp(actor);
            showActionEffects(attacking ? actor : null);
            if (!battle.isOver()) { selectReadyStudent(); }
        }
        if (battle.isOver()) { apTimer.stop(); }
        lastTickNanos = System.nanoTime();
        refresh();
    }

    private void completeSecondCast() {
        if (battle == null || !battle.hasPendingSecondCast()) return;
        Student actor = battle.getCurrent();
        if (battle.completeSecondCast()) {
            announceAbility();
            showActionEffects(actor);
            if (battle.hasPendingLooperHits()) looperHitTimer.restart();
            else if (!battle.isOver()) selectReadyStudent();
        }
        if (battle.isOver()) apTimer.stop();
        lastTickNanos = System.nanoTime();
        refresh();
    }

    private void advanceLooperHit() {
        if (battle == null || !battle.hasPendingLooperHits()) { looperHitTimer.stop(); return; }
        battle.advanceLooperHit();
        showActionEffects(null);
        if (!battle.hasPendingLooperHits()) {
            looperHitTimer.stop();
            if (battle.hasPendingAngelAttack() && !battle.isOver()) scheduleAngelAttack();
            else if (battle.hasPendingSecondCast() && !battle.isOver()) secondCastTimer.restart();
            else if (!battle.isOver()) selectReadyStudent();
        }
        if (battle.isOver()) apTimer.stop();
        lastTickNanos = System.nanoTime();
        refresh();
    }

    private void scheduleTwinShot() {
        twinShotTimer.setInitialDelay(battle.twinShotDelayMillis());
        twinShotTimer.restart();
    }
    private void advanceTwinShot() {
        if (battle == null || !battle.hasPendingTwinShot()) return;
        Student actor = battle.getCurrent();
        battle.advanceTwinShot();
        showActionEffects(actor);
        if (battle.hasPendingTwinShot()) scheduleTwinShot();
        else if (battle.hasPendingLooperHits()) looperHitTimer.restart();
        else if (!battle.isOver()) selectReadyStudent();
        if (battle.isOver()) apTimer.stop();
        lastTickNanos = System.nanoTime();
        refresh();
    }
    private void scheduleAngelAttack() {
        angelAttackTimer.setInitialDelay(300);
        angelAttackTimer.restart();
    }
    private void resolveSparkExplosion() {
        if (battle == null) return;
        boolean resolved = battle.hasPendingGuardianLoop() ? battle.resolveGuardianLoop() : battle.resolveSparkExplosion();
        if (!resolved) return;
        battle.cancelEscapeIfKnockedOut();
        applyApRefills();
        showActionEffects(null);
        if (battle.hasPendingGuardianLoop() && !battle.isOver()) sparkExplosionTimer.restart();
        if (battle.isOver()) apTimer.stop();
        lastTickNanos = System.nanoTime();
        refresh();
    }
    private void advanceAngelAttack() {
        if (battle == null || !battle.hasPendingAngelAttack()) return;
        Student actor = battle.getCurrent();
        battle.advanceAngelAttack();
        showActionEffects(actor);
        if (battle.hasPendingLooperHits()) looperHitTimer.restart();
        else if (battle.hasPendingAngelAttack()) scheduleAngelAttack();
        else if (!battle.isOver()) selectReadyStudent();
        if (battle.isOver()) apTimer.stop();
        lastTickNanos = System.nanoTime();
        refresh();
    }

    private void moveCursor(int direction) {
        if (battle == null || battle.isOver() || battle.isActionAnimating()) { return; }
        if (choosingItem) { itemChoice = Math.floorMod(itemChoice + direction, 2); refresh(); return; }
        if (targeting != TargetAction.NONE) { moveTarget(direction); return; }
        if (java.util.Arrays.stream(commands).noneMatch(JButton::isEnabled)) return;
        do { commandIndex = Math.floorMod(commandIndex + direction, commands.length); }
        while (!commands[commandIndex].isEnabled());
        updateCommandDisplay();
    }

    private void updateCommandDisplay() {
        BattleControls.updateCommands(commands, commandIndex);
        if (battle != null && battle.getCurrent() != null && !battle.isOver()) {
            if (battle.getCurrent().getId() == Student.CHARICE) attack.setText("Twin Shot 2 SP");
            ability.setText("Ability " + battle.getCurrent().getAbilitySpCost() + " SP");
        }
    }

    private boolean targetsParty() {
        return targeting == TargetAction.ITEM || targeting == TargetAction.WHITE_FLOWER || targeting == TargetAction.DEFEND
                || (targeting == TargetAction.ABILITY && battle.getCurrent().getAbilityType() == Student.AbilityType.HEALING_KISS);
    }

    private boolean targetsAllEnemies() {
        return targeting == TargetAction.ABILITY && (battle.getCurrent().getAbilityType() == Student.AbilityType.ARCANE_BURST
                || battle.getCurrent().getAbilityType() == Student.AbilityType.RAIN_OF_ARROWS);
    }

    private void beginTargeting(TargetAction action) {
        if (battle == null || battle.isOver() || battle.isActionAnimating() || battle.getCurrent() == null || targeting != TargetAction.NONE
                || actionPoints.get(battle.getCurrent()) + AP_EPSILON < battle.getCurrent().getActionThreshold()) { return; }
        notice = null; targeting = action; targetIndex = 0;
        if (targetsParty()) {
            while (!validPartyTarget(battle.getParty().get(targetIndex))) { targetIndex++; }
        } else {
            while (!battle.getEnemies().get(targetIndex).isAlive()) { targetIndex++; }
        }
        refresh();
    }

    private boolean validPartyTarget(Student target) {
        return targeting == TargetAction.DEFEND ? battle.canDefend(target)
                : targeting == TargetAction.WHITE_FLOWER ? !target.isAlive()
                : targeting == TargetAction.ABILITY && battle.getCurrent().getAbilityType() == Student.AbilityType.HEALING_KISS || target.isAlive();
    }

    private void moveTarget(int direction) {
        if (battle != null && battle.isVictory() && !battle.isMerchantEncounter() && onExplore != null) {
            victoryChoice = Math.floorMod(victoryChoice + direction, 2); scene.repaint(); return;
        }
        if (battle == null || battle.isOver() || targeting == TargetAction.NONE || targetsAllEnemies()) { return; }
        int size = targetsParty() ? battle.getParty().size() : battle.getEnemies().size();
        do { targetIndex = Math.floorMod(targetIndex + direction, size); }
        while (targetsParty() ? !validPartyTarget(battle.getParty().get(targetIndex)) : !battle.getEnemies().get(targetIndex).isAlive());
        refresh();
    }

    private void cancelTargeting() {
        if (choosingItem) { choosingItem = false; refresh(); }
        else if (targeting != TargetAction.NONE) { targeting = TargetAction.NONE; notice = null; refresh(); }
    }

    private void confirmCommand() {
        if (battle == null || battle.isActionAnimating()) { return; }
        if (battle.isOver()) {
            if (battle.isVictory() && victoryChoice == 0 && !battle.isMerchantEncounter() && onExplore != null) onExplore.accept(progress, encounterStage);
            else finish.doClick();
        }
        else if (choosingItem) {
            choosingItem = false;
            if (itemChoice == 0) beginTargeting(TargetAction.ITEM); else beginFlowerTargeting();
        }
        else if (targeting != TargetAction.NONE) {
            Student target = targetsParty() ? battle.getParty().get(targetIndex) : null;
            Enemy enemy = !targetsParty() ? battle.getEnemies().get(targetIndex) : null;
            TargetAction action = targeting;
            targeting = TargetAction.NONE;
            notice = null;
            boolean attacking = action == TargetAction.ATTACK
                    || (action == TargetAction.ABILITY && battle.getCurrent().getAbilityType() != Student.AbilityType.HEALING_KISS);
            performAction(() -> {
                if (action == TargetAction.ATTACK) { battle.attack(enemy); }
                else if (action == TargetAction.ABILITY) {
                    boolean fallen = target != null && !target.isAlive();
                    if (battle.ability(target, enemy)) { if (fallen) resetStudentAp(target); announceAbility(); }
                }
                else if (action == TargetAction.DEFEND) { battle.defend(target); }
                else if (action == TargetAction.WHITE_FLOWER) {
                    if (battle.useWhiteFlower(target)) resetStudentAp(target);
                }
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
            if (battle.isVictory() && !rewardRecorded) {
                java.util.List<String> rewards = new java.util.ArrayList<>();
                if (battle.isMerchantEncounter()) progress.unlockCarbonMarket();
                else { rewards.addAll(progress.claimStageRewardItems(encounterStage)); rewards.addAll(game.model.MonsterLoot.award(battle.getRewardEnemies(), progress, battleRandom, settings.getDifficulty())); }
                victoryRewardItems = List.copyOf(rewards);
                victoryPetot = progress.addPetot(battle.victoryPetot(settings.isGreedIsGood()));
                int baseExperience = battle.getRewardEnemies().stream().filter(Enemy::isDefeated).mapToInt(Enemy::getExperienceDrop).sum();
                double multiplier = switch (progress.getParty().size()) {
                    case 1 -> 3.0; case 2 -> 2.0; case 3 -> 1.5; case 4 -> 1.0;
                    default -> throw new IllegalStateException("Invalid party size");
                };
                victoryExperience = (int) Math.round(baseExperience * multiplier * settings.getXpBoost().multiplier());
                java.util.List<String> leveled = new java.util.ArrayList<>();
                java.util.List<String> bonus = new java.util.ArrayList<>();
                for (Student student : progress.getParty()) {
                    boolean boosted = "designer_leather_vest".equals(student.getEquipmentId(game.model.Equipment.Slot.ARMOR));
                    if (boosted) bonus.add(student.getName() + " +" + victoryExperience);
                    if (student.gainExperience((long) victoryExperience * (boosted ? 2 : 1)) > 0) { leveled.add(student.getName()); }
                }
                bonusXpStudents = List.copyOf(bonus);
                leveledStudents = List.copyOf(leveled);
                rewardRecorded = true;
            }
            if (battle.isVictory() && victoryStartedNanos == 0) {
                victoryStartedNanos = System.nanoTime();
                victoryTimer.start();
            }
        }
        refreshStatus();
        String targetMessage = targeting == TargetAction.NONE ? null : targetsParty()
                ? "Choose " + battle.getParty().get(targetIndex).getName() + " as the target."
                : targeting == TargetAction.ABILITY && battle.getCurrent().getAbilityType() == Student.AbilityType.ARCANE_BURST
                ? "Arcane Burst targets all enemies." : "Choose " + battle.getEnemies().get(targetIndex).getName() + " as the target.";
        String message = choosingItem ? "Choose a consumable." : targetMessage != null ? targetMessage
                : notice != null ? notice : battle.getMessage();
        log.setText(message);
        log.setCaretPosition(0);
        BattleControls.updateParty(battle, partyStatus);
        BattleControls.updateBars(battle, apBars, actionPoints);
        boolean canChoose = !over && !battle.isActionAnimating() && targeting == TargetAction.NONE && !choosingItem && battle.getCurrent() != null;
        attack.setEnabled(canChoose); ability.setEnabled(canChoose); item.setEnabled(canChoose);
        defend.setEnabled(canChoose && battle.canUseDefendOrEscape());
        escape.setEnabled(canChoose && !battle.isEscaping() && battle.canUseDefendOrEscape());
        if (canChoose && !commands[commandIndex].isEnabled()) commandIndex = 0;
        finish.setVisible(over && !battle.isVictory());
        finish.setText(battle.isVictory() ? "Continue" : "Return to Menu");
        updateCommandDisplay();
        scene.repaint();
    }

    private void refreshStatus() {
        boolean over = battle.isOver();
        String hint = battle.hasPendingLooperHits() ? "Looper Ring strikes..." : battle.hasPendingSecondCast() ? "Second cast incoming..." : choosingItem ? "Up/Down Choose item  •  Enter Confirm  •  Esc Cancel" : targeting == TargetAction.NONE ? "Up/Down Move  •  Enter Select"
                : targetsParty() ? "Left/Right Choose ally  •  Enter Confirm  •  Esc Cancel"
                : targetsAllEnemies() ? "Enter Confirm  •  Esc Cancel" : "Left/Right Choose enemy  •  Enter Confirm  •  Esc Cancel";
        status.setText(over ? (battle.isVictory() ? (battle.isMerchantEncounter() || onExplore == null
                ? "VICTORY  •  Enter: Camp" : "VICTORY  •  Left/Right: Choose  •  Enter: Confirm") : "GAME OVER  •  Enter: Return to Menu")
                : (battle.getCurrent() == null ? "Waiting for AP" : battle.getCurrent().getName() + " is ready")
                + (battle.isEscaping() ? "  |  Escaping: " + battle.getEscapeInitiator().getName() + " (" + battle.getEscapeTurnsRemaining() + ")"
                    : "  |  Kits: " + battle.getKits() + "  Flowers: " + progress.getInventory().getWhiteFlowers()) + "  |  " + hint);
    }



}
