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
        GameProgress progress = new GameProgress(List.of(0, 1, 3));
        check(new Student(0).getActionThreshold() == 35 && new Student(1).getActionThreshold() == 30
                && new Student(2).getActionThreshold() == 28 && new Student(3).getActionThreshold() == 45, "student AP thresholds");
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
                    route.advance(); route.advance(); map.open(route); key(map, "map-enter");
                    check(entered[0] == 1, "unfinished encounter remains unavailable");
                    BattlePanel panel = new BattlePanel(() -> { });
                    panel.start(List.of(0, 1, 2));
                    Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true);
                    Battle active = (Battle) field.get(panel);
                    Field timerField = BattlePanel.class.getDeclaredField("apTimer"); timerField.setAccessible(true);
                    check(((Timer) timerField.get(panel)).getDelay() == 50, "smooth animation interval");
                    Field pointsField = BattlePanel.class.getDeclaredField("actionPoints"); pointsField.setAccessible(true);
                    Field enemyField = BattlePanel.class.getDeclaredField("enemyPoints"); enemyField.setAccessible(true);
                    Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                    int hpBefore = active.getParty().stream().mapToInt(Student::getHp).sum();
                    advance.invoke(panel, 0.5);
                    check(((Map<?, ?>) pointsField.get(panel)).values().stream().allMatch(v -> v.equals(1.5))
                            && ((Map<?, ?>) enemyField.get(panel)).values().stream().allMatch(v -> v.equals(1.5)),
                            "students and enemies fill smoothly at 3 AP per second");
                    Field barsField = BattlePanel.class.getDeclaredField("apBars"); barsField.setAccessible(true);
                    javax.swing.JProgressBar[] bars = (javax.swing.JProgressBar[]) barsField.get(panel);
                    check(bars[0].getValue() == 150 && bars[0].getMaximum() == 2800,
                            "fractional AP is visible on the bar");
                    advance.invoke(panel, 7.5);
                    check(active.getCurrent() == null && active.getParty().stream().mapToInt(Student::getHp).sum() < hpBefore,
                            "Wisp A attacks before any student is ready");
                    check(((Map<?, ?>) enemyField.get(panel)).get(active.getEnemies().get(0)).equals(0.0), "enemy AP reset");
                    advance.invoke(panel, 2.0 / 3.0); advance.invoke(panel, 2.0 / 3.0);
                    check(active.getCurrent() != null && active.getCurrent().getId() == 2, "Charice selected at 28 AP");
                    @SuppressWarnings("unchecked") Map<Student, Double> points = (Map<Student, Double>) pointsField.get(panel);
                    double zackPoints = points.get(active.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow());
                    advance.invoke(panel, 1.0);
                    check(points.get(active.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow()) == zackPoints,
                            "clocks pause while choosing");
                    int firstHp = active.getEnemies().get(0).getHp(), secondHp = active.getEnemies().get(1).getHp();
                    key(panel, "battle-confirm"); key(panel, "battle-right"); key(panel, "battle-confirm");
                    check(active.getEnemies().get(0).getHp() == firstHp && active.getEnemies().get(1).getHp() < secondHp,
                            "keyboard targets selected enemy");
                    check(points.get(active.getParty().get(0)) == 0.0, "only acting student's AP resets");
                    advance.invoke(panel, 2.0 / 3.0);
                    check(active.getCurrent() != null && active.getCurrent().getId() == 1, "Bogart selected immediately at 30 AP");
                    BattlePanel tied = new BattlePanel(() -> { });
                    tied.start(List.of(0, 1, 3));
                    Battle tiedBattle = (Battle) field.get(tied);
                    @SuppressWarnings("unchecked") Map<Student, Double> tiedPoints = (Map<Student, Double>) pointsField.get(tied);
                    for (Student student : tiedBattle.getParty()) {
                        if (student.getId() == 0 || student.getId() == 1) { tiedPoints.put(student, student.getActionThreshold() - 3.0); }
                    }
                    advance.invoke(tied, 1.0);
                    check(tiedBattle.getCurrent().getId() == 0, "simultaneous ready students use speed order");
                    key(tied, "battle-confirm"); key(tied, "battle-confirm");
                    check(tiedBattle.getCurrent().getId() == 1, "next ready student selected without another tick");
                    GameSettings settings = new GameSettings();
                    check(settings.getApPerSecond() == 3.0, "default AP setting");
                    settings.setApPerSecond(6.0);
                    BattlePanel configured = new BattlePanel(() -> { }, p -> { }, settings);
                    configured.start(List.of(0, 1, 3));
                    advance.invoke(configured, 0.5);
                    check(((Map<?, ?>) pointsField.get(configured)).values().stream().allMatch(v -> v.equals(3.0)),
                            "configured rate controls AP fill");
                    BattlePanel delayed = new BattlePanel(() -> { });
                    delayed.start(List.of(0, 1, 2));
                    Battle delayedBattle = (Battle) field.get(delayed);
                    int fullHp = delayedBattle.getParty().stream().mapToInt(Student::getHp).sum();
                    advance.invoke(delayed, 10.0);
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
