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
    /** Serialized state: return to menu.
     * @serial
     */
    private final Runnable returnToMenu;
    /** Serialized state: on victory.
     * @serial
     */
    private final Consumer<GameProgress> onVictory;
    /** Serialized state: on escape.
     * @serial
     */
    private Consumer<GameProgress> onEscape;
    /** Serialized state: on explore.
     * @serial
     */
    private java.util.function.BiConsumer<GameProgress, Integer> onExplore;
    /** Serialized state: victory choice.
     * @serial
     */
    private int victoryChoice;
    /** Serialized state: settings.
     * @serial
     */
    private final GameSettings settings;
    /** Serialized state: battle random.
     * @serial
     */
    private final Random battleRandom;
    /** Serialized state: status.
     * @serial
     */
    private final JLabel status = new JLabel();
    /** Serialized state: party status.
     * @serial
     */
    private final JLabel[] partyStatus = {new JLabel(), new JLabel(), new JLabel(), new JLabel()};
    /** Serialized state: ap bars.
     * @serial
     */
    private final JProgressBar[] apBars = {new JProgressBar(), new JProgressBar(), new JProgressBar(), new JProgressBar()};
    /** Serialized state: log.
     * @serial
     */
    private final JTextArea log = new JTextArea();
    /** Serialized state: log scroll.
     * @serial
     */
    private final JScrollPane logScroll = new JScrollPane(log);
    /** Serialized state: attack.
     * @serial
     */
    private final JButton attack = new JButton("Attack");
    /** Serialized state: ability.
     * @serial
     */
    private final JButton ability = new JButton("Ability");
    /** Serialized state: item.
     * @serial
     */
    private final JButton item = new JButton("Item");
    /** Serialized state: defend.
     * @serial
     */
    private final JButton defend = new JButton("Defend");
    /** Serialized state: escape.
     * @serial
     */
    private final JButton escape = new JButton("Escape");
    /** Serialized state: finish.
     * @serial
     */
    private final JButton finish = new JButton("Return to Menu");
    /** Serialized state: scene.
     * @serial
     */
    private final BattleScene scene;
    /** Serialized state: battle.
     * @serial
     */
    private Battle battle;
    /** Serialized state: progress.
     * @serial
     */
    private GameProgress progress;
    /** Serialized state: command index.
     * @serial
     */
    private int commandIndex;
    /** Serialized state: commands.
     * @serial
     */
    private final JButton[] commands = {attack, ability, item, defend, escape};
    enum TargetAction { NONE, ATTACK, ABILITY, ITEM, WHITE_FLOWER, DEFEND }
    /** Serialized state: targeting.
     * @serial
     */
    private TargetAction targeting = TargetAction.NONE;
    /** Serialized state: target index.
     * @serial
     */
    private int targetIndex;
    /** Serialized state: choosing item.
     * @serial
     */
    private boolean choosingItem;
    /** Serialized state: item choice.
     * @serial
     */
    private int itemChoice;
    /** Serialized state: ability announcement.
     * @serial
     */
    private Battle.AbilityAnnouncement abilityAnnouncement;
    /** Serialized state: ability alerts.
     * @serial
     */
    private final java.util.ArrayDeque<Battle.AbilityAnnouncement> abilityAlerts = new java.util.ArrayDeque<>();
    /** Serialized state: ability alert timer.
     * @serial
     */
    private final Timer abilityAlertTimer;
    /** Serialized state: notice.
     * @serial
     */
    private String notice;
    /** Serialized state: victory reward items.
     * @serial
     */
    private List<String> victoryRewardItems = List.of();
    /** Serialized state: reward recorded.
     * @serial
     */
    private boolean rewardRecorded;
    /** Serialized state: victory petot.
     * @serial
     */
    private int victoryPetot;
    /** Serialized state: victory experience.
     * @serial
     */
    private int victoryExperience;
    /** Serialized state: leveled students.
     * @serial
     */
    private List<String> leveledStudents = List.of();
    /** Serialized state: bonus xp students.
     * @serial
     */
    private List<String> bonusXpStudents = List.of();
    /** Serialized state: encounter stage.
     * @serial
     */
    private int encounterStage;
    /** Serialized state: action points.
     * @serial
     */
    private final Map<Student, Double> actionPoints = new HashMap<>();
    /** Serialized state: enemy points.
     * @serial
     */
    private final Map<Enemy, Double> enemyPoints = new HashMap<>();
    /** Serialized state: ap timer.
     * @serial
     */
    private final Timer apTimer = new Timer(AP_FRAME_MILLIS, event -> tickActionPoints());
    /** Serialized state: last tick nanos.
     * @serial
     */
    private long lastTickNanos;
    /** Serialized state: last dot nanos.
     * @serial
     */
    private long lastDotNanos;
    /** Serialized state: effect timer.
     * @serial
     */
    private final Timer effectTimer;
    /** Serialized state: ambush alert timer.
     * @serial
     */
    private final Timer ambushAlertTimer;
    /** Serialized state: victory timer.
     * @serial
     */
    private final Timer victoryTimer;
    /** Serialized state: second cast timer.
     * @serial
     */
    private final Timer secondCastTimer;
    /** Serialized state: looper hit timer.
     * @serial
     */
    private final Timer looperHitTimer;
    /** Serialized state: angel attack timer.
     * @serial
     */
    private final Timer angelAttackTimer;
    /** Serialized state: twin shot timer.
     * @serial
     */
    private final Timer twinShotTimer;
    /** Serialized state: spark explosion timer.
     * @serial
     */
    private final Timer sparkExplosionTimer;
    /** Serialized state: monster turn timer.
     * @serial
     */
    private final Timer monsterTurnTimer;
    /** Serialized state: clock.
     * @serial
     */
    private final BattleInput input = new BattleInput(this);
    /** Battle action-bar scheduler.
     * @serial
     */
    private final BattleClock clock;
    /** Coordinates delayed actions.
     * @serial
     */
    private final BattleActionFlow actionFlow = new BattleActionFlow(this);
    /** Serialized state: victory started nanos.
     * @serial
     */
    private long victoryStartedNanos;
    /** Serialized state: show ambush.
     * @serial
     */
    private boolean showAmbush;
    /** Serialized state: show effects.
     * @serial
     */
    private boolean showEffects;
    /** Serialized state: effect batches.
     * @serial
     */
    private final java.util.List<BattleScene.EffectBatch> effectBatches = new java.util.ArrayList<>();
    /** Serialized state: effect sequence.
     * @serial
     */
    private int effectSequence;
    /** Serialized state: effect started nanos.
     * @serial
     */
    private long effectStartedNanos;
    /** Serialized state: attacking sprite.
     * @serial
     */
    private Object attackingSprite;
    /** Serialized state: rain shooter.
     * @serial
     */
    private Student rainShooter;
    /** Serialized state: rain targets.
     * @serial
     */
    private List<Enemy> rainTargets = List.of();
    /** Serialized state: rain started nanos.
     * @serial
     */
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
        scene = new BattleScene(new BattleViewState(this));
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
        clock = new BattleClock(this, settings, actionPoints, enemyPoints, apBars, apTimer, monsterTurnTimer, sparkExplosionTimer, scene);
        BattleControls.build(this, scene, status, partyStatus, apBars, log, logScroll,
                attack, ability, item, defend, escape, finish);
        input.bind();
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
    /** Sets the destination for a successful party escape.
     * @param action the action
     */
    public void setEscapeAction(Consumer<GameProgress> action) { onEscape = java.util.Objects.requireNonNull(action); }

    private void effectTimerStop() { effectTimer.stop(); }

    void showActionEffects(Object attacker) {
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

    void announceAbility() {
        Battle.AbilityAnnouncement latest = battle.getLastAbility();
        if (latest == null) return;
        if (abilityAnnouncement != null) { abilityAlerts.addLast(latest); return; }
        abilityAnnouncement = latest; abilityAlertTimer.restart(); scene.repaint();
    }
    void beginFlowerTargeting() {
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

    /** Starts a specific unlocked stage, including a replay.
     * @param progress current game progress
     * @param ambushed the ambushed
     * @param stage stage index
     */
    public void start(GameProgress progress, boolean ambushed, int stage) {
        startEncounter(progress, ambushed, stage, false);
    }
    /** Starts the merchant encounter using the current party.
     * @param progress current game progress
     */
    public void startMerchant(GameProgress progress) { startEncounter(progress, false, 2, true); }
    /** Sets the action used to replay the current stage after victory.
     * @param action the action
     */
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

    private void resetActionPoints(boolean ambushed) { clock.reset(ambushed); }

    void resetStudentAp(Student student) { actionPoints.put(student, 0.0); }
    void resetEnemyAp(Enemy enemy) { enemyPoints.put(enemy, 0.0); }
    void applyApRefills() { clock.applyApRefills(); }

    private void tickActionPoints() { clock.tick(); }
    double[] clockElapsed(long now) {
        double[] elapsed = {Math.max(0.0, (now - lastTickNanos) / 1_000_000_000.0), Math.max(0.0, (now - lastDotNanos) / 1_000_000_000.0)};
        lastTickNanos = now; lastDotNanos = now; return elapsed;
    }
    void clockClearTarget() { targeting = TargetAction.NONE; choosingItem = false; }

    private void advanceActionPoints(double seconds) { clock.advance(seconds); }
    private double studentRate(Student student, double baseRate) { return BattleClock.studentRate(student, baseRate); }
    Battle battleForClock() { return battle; }

    boolean selectReadyStudent() { return clock.selectReadyStudent(); }
    void escapeComplete() { onEscape.accept(progress); }

    void performAction(Runnable action, boolean attacking) { actionFlow.performAction(action, attacking); }
    private void completeSecondCast() { actionFlow.completeSecondCast(); }
    private void advanceLooperHit() { actionFlow.advanceLooperHit(); }
    private void scheduleTwinShot() { actionFlow.scheduleTwinShot(); }
    private void advanceTwinShot() { actionFlow.advanceTwinShot(); }
    private void scheduleAngelAttack() { actionFlow.scheduleAngelAttack(); }
    private void resolveSparkExplosion() { actionFlow.resolveSparkExplosion(); }
    private void advanceAngelAttack() { actionFlow.advanceAngelAttack(); }
    Battle actionBattle() { return battle; }
    Map<Student, Double> actionPoints() { return actionPoints; }
    Timer actionApTimer() { return apTimer; }
    Timer actionLooperTimer() { return looperHitTimer; }
    Timer actionSecondCastTimer() { return secondCastTimer; }
    Timer actionTwinTimer() { return twinShotTimer; }
    Timer actionAngelTimer() { return angelAttackTimer; }
    Timer actionSparkTimer() { return sparkExplosionTimer; }
    void actionTickNow() { lastTickNanos = System.nanoTime(); }
    void actionRain(Student actor) { rainShooter = actor; rainTargets = battle.getRainArrowTargets(); rainStartedNanos = System.nanoTime(); }
    private void moveCursor(int direction) { input.moveCursor(direction); }
    private void updateCommandDisplay() { input.updateCommandDisplay(); }
    private boolean targetsParty() { return input.targetsParty(); }
    private boolean targetsAllEnemies() { return input.targetsAllEnemies(); }
    private void beginTargeting(TargetAction action) { input.beginTargeting(action); }
    private void moveTarget(int direction) { input.moveTarget(direction); }
    private void cancelTargeting() { input.cancelTargeting(); }
    private void confirmCommand() { input.confirmCommand(); }
    void inputConfirm() { confirmCommand(); }
    boolean inputTargetsParty() { return targetsParty(); } boolean inputTargetsAllEnemies() { return targetsAllEnemies(); }
    int viewStage() { return encounterStage; } int viewPetot() { return victoryPetot; } int viewExperience() { return victoryExperience; }
    java.util.function.BiConsumer<GameProgress, Integer> viewExplore() { return onExplore; }
    List<String> viewRewards() { return victoryRewardItems; } List<String> viewLeveled() { return leveledStudents; } List<String> viewBonus() { return bonusXpStudents; }
    boolean viewShowEffects() { return showEffects; } boolean viewAmbush() { return showAmbush; }
    List<BattleScene.EffectBatch> viewBatches() { return effectBatches; } Battle.AbilityAnnouncement viewAnnouncement() { return abilityAnnouncement; }
    long viewVictoryStarted() { return victoryStartedNanos; } long viewRainStarted() { return rainStartedNanos; } long viewEffectStarted() { return effectStartedNanos; }
    Map<Enemy, Double> viewEnemyPoints() { return enemyPoints; } Object viewAttacker() { return attackingSprite; }
    Student viewRainShooter() { return rainShooter; } List<Enemy> viewRainTargets() { return rainTargets; }
    int inputCommand() { return commandIndex; } void inputCommand(int v) { commandIndex = v; }
    int inputTargetIndex() { return targetIndex; } void inputTargetIndex(int v) { targetIndex = v; }
    int inputItemChoice() { return itemChoice; } void inputItemChoice(int v) { itemChoice = v; }
    int inputVictoryChoice() { return victoryChoice; } void inputVictoryChoice(int v) { victoryChoice = v; }
    TargetAction inputTargeting() { return targeting; } void inputTargeting(TargetAction v) { targeting = v; }
    boolean inputChoosingItem() { return choosingItem; } void inputChoosingItem(boolean v) { choosingItem = v; }
    void inputNotice(String v) { notice = v; }
    JButton[] inputCommands() { return commands; }
    JButton inputItem() { return item; } JButton inputDefend() { return defend; } JButton inputEscape() { return escape; }
    GameProgress inputProgress() { return progress; }
    void inputFinishAction() { if (battle.isVictory()) onVictory.accept(progress); else returnToMenu.run(); }
    void inputScroll(int direction) { scrollLog(direction); }
    JButton inputAttack() { return attack; } JButton inputAbility() { return ability; } JButton inputFinish() { return finish; }
    BattleScene inputScene() { return scene; }
    boolean inputCanExplore() { return onExplore != null; }
    void inputExplore() { onExplore.accept(progress, encounterStage); }
    void refresh() {
        boolean over = battle.isOver();
        if (over) {
            showAmbush = false;
            ambushAlertTimer.stop();
            if (battle.isVictory() && !rewardRecorded) {
                BattleVictoryRewards.Result rewards = BattleVictoryRewards.award(battle, progress, encounterStage, battleRandom, settings);
                victoryRewardItems = rewards.items();
                victoryPetot = rewards.petot();
                victoryExperience = rewards.experience();
                leveledStudents = rewards.leveledStudents();
                bonusXpStudents = rewards.bonusXpStudents();
                rewardRecorded = true;
            }
            if (battle.isVictory() && victoryStartedNanos == 0) {
                victoryStartedNanos = System.nanoTime();
                victoryTimer.start();
            }
        }
        BattleHudController.update(this);
    }
    String inputNoticeValue() { return notice; }
    JTextArea hudLog() { return log; } JLabel hudStatus() { return status; }
    JLabel[] hudPartyStatus() { return partyStatus; } JProgressBar[] hudBars() { return apBars; }
    void inputUpdateCommands() { updateCommandDisplay(); }



}
