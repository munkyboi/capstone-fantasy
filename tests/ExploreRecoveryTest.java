import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Checks Explore recovery before the replay callback, including fallen heroes. */
public final class ExploreRecoveryTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void key(BattlePanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
    }
    private static int amount(int maximum, int percent) { return (int) (((long) maximum * percent + 99) / 100); }
    private static final class Rolls extends Random {
        private boolean recovery;
        private int index;
        @Override public int nextInt(int bound) {
            if (recovery && bound == 21) return new int[]{0, 10, 20}[index++ % 3];
            return 0;
        }
        @Override public double nextDouble() { return 0.99; }
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Rolls rolls = new Rolls();
                GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
                int[] callbacks = {0};
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), rolls);
                panel.setExploreAction((state, stage) -> { callbacks[0]++; check(stage == 0, "repeats the same stage"); });
                panel.start(progress, false, 0);
                Battle battle = (Battle) field(panel, "battle");
                for (Enemy enemy : battle.getEnemies()) enemy.takeDamage(Integer.MAX_VALUE);
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                List<Student> heroes = progress.getParty();
                for (Student hero : heroes) { hero.takeDamage(hero.getHp()); hero.restoreSp(0); }
                Student almostFull = heroes.get(1);
                almostFull.heal(almostFull.getMaxHp() - 1); almostFull.restoreSp(almostFull.getMaxSp() - 1);
                rolls.recovery = true;
                key(panel, "battle-left"); key(panel, "battle-confirm");
                check(callbacks[0] == 1, "Explore starts replay once");
                for (int i = 0; i < heroes.size(); i++) {
                    Student hero = heroes.get(i); int percent = 30 + 10 * i;
                    int expectedHp = i == 1 ? hero.getMaxHp() : amount(hero.getMaxHp(), percent);
                    int expectedSp = i == 1 ? hero.getMaxSp() : amount(hero.getMaxSp(), percent);
                    check(hero.getHp() == expectedHp && hero.getSp() == expectedSp, "30-50% recovery and resource caps for hero " + i);
                    check(hero.isAlive(), "fallen heroes recover before replay");
                }
                for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer", "looperHitTimer", "angelAttackTimer", "twinShotTimer", "sparkExplosionTimer", "monsterTurnTimer", "abilityAlertTimer")) ((Timer) field(panel, name)).stop();
                System.out.println("Explore recovery checks passed");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }
}
