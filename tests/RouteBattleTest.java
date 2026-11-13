import game.io.SaveStore;
import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.Student;
import game.view.BattlePanel;
import game.view.WorldMapPanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Regression checks for the route, action points and HP feedback. */
public final class RouteBattleTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(JPanel panel, String name) {
        Action action = panel.getActionMap().get(name);
        if (action == null) { throw new AssertionError("Missing action " + name); }
        action.actionPerformed(new ActionEvent(panel, 0, name));
    }
    /** Runs the route and combat checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        GameSettings ambushSettings = new GameSettings();
        check(ambushSettings.getAmbushChance() == 0.5, "ambush chance is 50 percent");
        check(ambushSettings.rollAmbush(0, new Random() { @Override public double nextDouble() { return 0.499; } })
                && !ambushSettings.rollAmbush(0, new Random() { @Override public double nextDouble() { return 0.5; } }),
                "ambush roll uses the 50 percent boundary");
        check(!ambushSettings.rollAmbush(GameProgress.FINAL_BOSS_ENCOUNTER,
                new Random() { @Override public double nextDouble() { return 0.0; } }),
                "final boss never triggers an ambush, even with a winning roll");
        check(ambushSettings.rollAmbush(GameProgress.FINAL_BOSS_ENCOUNTER - 1,
                new Random() { @Override public double nextDouble() { return 0.0; } }),
                "ordinary stages remain eligible for ambushes");
        GameProgress progress = new GameProgress(List.of(0, 1, 3));
        check(new Student(0).getActionThreshold() == 35 && new Student(1).getActionThreshold() == 30
                && new Student(2).getActionThreshold() == 28 && new Student(3).getActionThreshold() == 50, "student AP thresholds");
        check(new Student(Student.CHARICE).getSpeed() > new Student(Student.BOGART).getSpeed()
                && new Student(Student.BOGART).getSpeed() < new Student(Student.ZACK).getSpeed()
                && new Student(Student.ZACK).getSpeed() > new Student(Student.ANGEL).getSpeed(),
                "speed order matches configured stats, with Angel slowest");
        Battle battle = new Battle(progress, new Random(1));
        Enemy enemy = battle.getEnemies().get(0);
        check(battle.getCurrent() == null && !battle.attack(enemy), "no action before ready");
        Student zack = battle.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow();
        Student angel = battle.getParty().stream().filter(s -> s.getId() == 3).findFirst().orElseThrow();
        check(battle.selectStudent(zack) && !battle.enemyAttack(enemy), "enemy waits during selection");
        battle.attack(enemy);
        check(battle.getEffects().stream().anyMatch(e -> e.getEnemy() == enemy && e.getAmount() < 0), "damage feedback");
        angel.takeDamage(20);
        check(battle.selectStudent(angel) && battle.ability(angel, null), "Angel heals");
        check(battle.getEffects().stream().anyMatch(e -> e.getStudent() == angel && e.getAmount() > 0), "healing feedback");
        check(battle.enemyAttack(enemy), "enemy can act while no player selected");
        Path directory = Files.createTempDirectory("capstone-route-test-");
        try {
            SaveStore store = new SaveStore(directory);
            progress.advance();
            store.save(1, SaveStore.Format.TEXT, progress);
            progress.advance();
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(2, format, progress);
                check(store.load(2, format).getEncounter() == 2, "route saved");
            }
            check(store.load(1, SaveStore.Format.TEXT).getEncounter() == 1, "previous save compatibility");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    int[] entered = {0};
                    WorldMapPanel map = new WorldMapPanel(p -> entered[0]++, p -> { }, () -> { });
                    GameProgress route = new GameProgress(List.of(0, 1, 3));
                    map.open(route); key(map, "map-right");
                    Field cursor = WorldMapPanel.class.getDeclaredField("cursor"); cursor.setAccessible(true);
                    check(cursor.getInt(map) == 0, "locked route");
                    key(map, "map-enter"); check(entered[0] == 1, "available route");
                    route.advance(); route.advance(); route.advance(); map.open(route); key(map, "map-enter");
                    check(entered[0] == 2, "stage four is available after stage three");
                    BattlePanel panel = TestBattles.twoEnemies(() -> { });
                    panel.start(new GameProgress(List.of(0, 1, 2)), true);
                    Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true);
                    Battle active = (Battle) field.get(panel);
                    Field timerField = BattlePanel.class.getDeclaredField("apTimer"); timerField.setAccessible(true);
                    check(((Timer) timerField.get(panel)).getDelay() == 16, "smooth animation interval");
                    Field pointsField = BattlePanel.class.getDeclaredField("actionPoints"); pointsField.setAccessible(true);
                    Field enemyField = BattlePanel.class.getDeclaredField("enemyPoints"); enemyField.setAccessible(true);
                    Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                    BattlePanel normal = TestBattles.twoEnemies(() -> { });
                    normal.start(List.of(0, 1, 2));
                    Battle normalBattle = (Battle) field.get(normal);
                    @SuppressWarnings("unchecked") Map<Student, Double> normalPoints = (Map<Student, Double>) pointsField.get(normal);
                    check(normalBattle.getCurrent().getId() == Student.CHARICE
                            && normalBattle.getParty().stream().allMatch(student -> normalPoints.get(student) == student.getActionThreshold()),
                            "normal battle starts full with fastest student ready");
                    Map<?, ?> normalEnemyPoints = (Map<?, ?>) enemyField.get(normal);
                    check(normalBattle.getEnemies().stream().allMatch(monster -> normalEnemyPoints.get(monster).equals(0.0)),
                            "normal battle starts with empty enemy AP");
                    @SuppressWarnings("unchecked") Map<Enemy, Double> ambushEnemyPoints = (Map<Enemy, Double>) enemyField.get(panel);
                    check(active.getEnemies().stream().allMatch(monster ->
                            Math.abs((double) ambushEnemyPoints.get(monster) - monster.getActionThreshold() * 0.8) < 0.0001),
                            "ambush enemies start at 80% of their individual thresholds");
                    Field logField = BattlePanel.class.getDeclaredField("log"); logField.setAccessible(true);
                    check(((javax.swing.JTextArea) logField.get(panel)).getText().contains("You have been ambushed!"),
                            "ambush alert appears in the battle log");
                    Map<?, ?> initialPoints = (Map<?, ?>) pointsField.get(panel);
                    check(active.getParty().stream().allMatch(student -> initialPoints.get(student).equals(0.0)),
                            "ambush starts with empty student AP");
                    GameProgress secondStage = new GameProgress(List.of(0, 1, 3));
                    secondStage.advance();
                    Battle ambush = new Battle(secondStage, new Random(1), true);
                    check(ambush.getMessage().contains("You have been ambushed!")
                            && ambush.getEnemies().stream().anyMatch(Enemy::isSentinel)
                            && secondStage.getEncounter() == 1,
                            "stage ambush retains stage enemies and progress");
                    BattlePanel secondPanel = TestBattles.twoEnemies(() -> { });
                    secondPanel.start(secondStage, true);
                    Battle secondBattle = (Battle) field.get(secondPanel);
                    Map<?, ?> secondEnemyPoints = (Map<?, ?>) enemyField.get(secondPanel);
                    check(secondBattle.getEnemies().stream().allMatch(monster ->
                            Math.abs((double) secondEnemyPoints.get(monster) - monster.getActionThreshold() * 0.8) < 0.0001),
                            "ambush starts Wisp and Sentinel at 80% AP");
                    int hpBefore = active.getParty().stream().mapToInt(Student::getHp).sum();
                    advance.invoke(panel, 0.5);
                    check(((Map<?, ?>) pointsField.get(panel)).values().stream().allMatch(v -> v.equals(1.5))
                            && active.getEnemies().stream().allMatch(monster ->
                                    Math.abs((double) ambushEnemyPoints.get(monster) - (monster.getActionThreshold() * 0.8 + 1.5)) < 0.0001),
                            "students and enemies fill smoothly at 3 AP per second");
                    Field barsField = BattlePanel.class.getDeclaredField("apBars"); barsField.setAccessible(true);
                    javax.swing.JProgressBar[] bars = (javax.swing.JProgressBar[]) barsField.get(panel);
                    check(bars[0].getValue() == 150 && bars[0].getMaximum() == 2800,
                            "fractional AP is visible on the bar");
                    advance.invoke(panel, 1.9);
                    check(active.getCurrent() == null && active.getMessage().contains("attacks"),
                            "Wisp A attacks before any student is ready");
                    check(Math.abs((double) ((Map<?, ?>) enemyField.get(panel)).get(active.getEnemies().get(0))) < 0.0001, "enemy AP reset");
                    ambushEnemyPoints.replaceAll((monster,value)->-1000.0);
                    advance.invoke(panel, 20.8 / 3.0);
                    check(active.getCurrent() != null && active.getCurrent().getId() == 2, "Charice selected at 28 AP");
                    @SuppressWarnings("unchecked") Map<Student, Double> points = (Map<Student, Double>) pointsField.get(panel);
                    double zackPoints = points.get(active.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow());
                    advance.invoke(panel, 1.0);
                    check(points.get(active.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow()) == zackPoints,
                            "clocks pause while choosing");
                    int firstHp = active.getEnemies().get(0).getHp(), secondHp = active.getEnemies().get(1).getHp();
                    key(panel, "battle-confirm"); key(panel, "battle-right"); key(panel, "battle-confirm");
                    check(active.getEnemies().get(0).getHp() == firstHp && (active.getEnemies().get(1).getHp() < secondHp || active.getEffects().stream().anyMatch(e -> e.getEnemy() == active.getEnemies().get(1) && "Dodge".equals(e.getLabel()))),
                            "keyboard targets selected enemy");
                    check(points.get(active.getParty().get(0)) == 0.0, "only acting student's AP resets");
                    advance.invoke(panel, 2.0 / 3.0);
                    check(active.getCurrent() != null && active.getCurrent().getId() == 1, "Bogart selected immediately at 30 AP");
                    BattlePanel tied = TestBattles.twoEnemies(() -> { });
                    tied.start(new GameProgress(List.of(0, 1, 3)), true);
                    Battle tiedBattle = (Battle) field.get(tied);
                    @SuppressWarnings("unchecked") Map<Student, Double> tiedPoints = (Map<Student, Double>) pointsField.get(tied);
                    for (Student student : tiedBattle.getParty()) {
                        if (student.getId() == 0 || student.getId() == 1) { tiedPoints.put(student, student.getActionThreshold() - 3.0); }
                    }
                    advance.invoke(tied, 1.0);
                    check(tiedBattle.getCurrent().getId() == 0, "equal-speed ready students retain party order");
                    key(tied, "battle-confirm"); key(tied, "battle-confirm");
                    check(tiedBattle.getCurrent().getId() == 1, "next ready student selected without another tick");
                    GameSettings settings = new GameSettings();
                    check(settings.getApPerSecond() == 10.0, "default AP setting");
                    settings.setApPerSecond(6.0);
                    BattlePanel configured = TestBattles.twoEnemies(() -> { }, p -> { }, settings);
                    configured.start(new GameProgress(List.of(0, 1, 3)), true);
                    advance.invoke(configured, 0.5);
                    check(((Map<?, ?>) pointsField.get(configured)).values().stream().allMatch(v -> v.equals(3.0)),
                            "configured rate controls AP fill");
                    BattlePanel delayed = TestBattles.twoEnemies(() -> { });
                    delayed.start(new GameProgress(List.of(0, 1, 2)), true);
                    Battle delayedBattle = (Battle) field.get(delayed);
                    int fullHp = delayedBattle.getParty().stream().mapToInt(Student::getHp).sum();
                    for (int step=0;step<8 && delayedBattle.getCurrent()==null;step++) {
                        Field pause = BattlePanel.class.getDeclaredField("monsterTurnTimer"); pause.setAccessible(true); ((Timer)pause.get(delayed)).stop();
                        advance.invoke(delayed, 10.0);
                    }
                    check(delayedBattle.getCurrent().getId() == 2
                            && delayedBattle.getParty().stream().mapToInt(Student::getHp).sum() < fullHp,
                            "delayed frame processes enemy actions before a later ready student");
                } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
            });
            System.out.println("Route, independent AP and HP feedback checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                });
            }
        }
    }
}
