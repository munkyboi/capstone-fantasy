import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Checks party-size XP bonuses and restoration on level-up. */
public final class LevelUpRewardsTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Student student = Student.create(0); student.takeDamage(40); student.restoreSp(0);
        student.gainExperience(99); check(student.getHp() == 40 && student.getSp() == 0, "XP alone does not heal");
        student.gainExperience(1); check(student.getHp() == student.getMaxHp() && student.getSp() == student.getMaxSp(), "level-up fully restores HP and SP");
        student.takeDamage(999); student.restoreSp(0); student.gainExperience(1);
        check(!student.isAlive() && student.getSp() == 0, "KO remains until a level is gained");
        student.gainExperience(99); check(student.isAlive() && student.getHp() == student.getMaxHp() && student.getSp() == student.getMaxSp(), "level-up restores KO student");
        SwingUtilities.invokeAndWait(() -> {
            try {
                for (int size = 1; size <= 4; size++) {
                    List<Integer> ids = java.util.stream.IntStream.range(0, size).boxed().toList();
                    GameProgress progress = new GameProgress(ids); progress.advance();
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), new Random(2));
                    panel.start(progress, false, 1);
                    Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                    Battle battle = (Battle) battleField.get(panel);
                    int base = battle.getEnemies().stream().mapToInt(Enemy::getExperienceDrop).sum();
                    for (Enemy enemy : battle.getEnemies()) enemy.takeDamage(9999);
                    Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                    int expected = (int) Math.round(base * new double[]{0, 3, 2, 1.5, 1}[size]);
                    Field shown = BattlePanel.class.getDeclaredField("victoryExperience"); shown.setAccessible(true);
                    check(shown.getInt(panel) == expected, "popup XP for party size " + size);
                    check(progress.getParty().stream().allMatch(s -> s.getExperience() == expected), "XP for party size " + size);
                    refresh.invoke(panel);
                    check(progress.getParty().stream().allMatch(s -> s.getExperience() == expected), "XP granted only once");
                    Field timer = BattlePanel.class.getDeclaredField("victoryTimer"); timer.setAccessible(true); ((javax.swing.Timer) timer.get(panel)).stop();
                }
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Level-up restoration and party XP checks passed");
    }
}
