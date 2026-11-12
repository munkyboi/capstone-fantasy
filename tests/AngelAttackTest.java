import game.model.Battle;
import game.model.BattleEffect;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.Student;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Verifies Angel's multi-hit basic attack and one-action AP cost. */
public final class AngelAttackTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static Random fixed(int count) {
        return new Random() {
            private boolean initial = true;
            @Override public double nextDouble() { return 0; }
            @Override public int nextInt(int bound) {
                if (initial) { initial = false; return count - 1; }
                return bound == 5 ? 0 : bound - 1;
            }
        };
    }
    private static Battle battle(int count) { return new Battle(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL), fixed(count)); }
    private static Student angel(Battle battle) {
        return battle.getParty().stream().filter(s -> s.getId() == Student.ANGEL).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        for (int count = 1; count <= 3; count++) {
            Battle battle = battle(count);
            List<Enemy> enemies = battle.getEnemies();
            check(enemies.size() == count && battle.selectStudent(angel(battle)), "select Angel for " + count);
            check(battle.attack(enemies.get(0)), "attack succeeds");
            List<BattleEffect> effects = battle.getEffects();
            check(effects.size() == (count == 1 ? 2 : 3), "correct number of hits for " + count);
            check(effects.get(0).getEnemy() == enemies.get(0), "first hit uses selected target");
            for (int i = 1; i < effects.size(); i++) {
                check(effects.get(i).getEnemy() == enemies.get(count - 1), "follow-up target uses random living monster");
                check(effects.get(i).getAmount() == -20, "follow-up damage and indicator");
            }
            check(battle.getCurrent() == null, "all hits consume only one action");
            check(battle.getMessage().contains("takes 20 damage"), "attack log identifies damage");
        }
        Battle retarget = battle(2);
        Enemy first = retarget.getEnemies().get(0), other = retarget.getEnemies().get(1);
        first.takeDamage(60);
        check(retarget.selectStudent(angel(retarget)) && retarget.attack(first), "attack weak selected enemy");
        check(!first.isAlive() && retarget.getEffects().size() == 3
                && retarget.getEffects().get(1).getEnemy() == other && retarget.getEffects().get(2).getEnemy() == other,
                "follow-ups retarget a living enemy after knockout");
        Battle finish = battle(1);
        Enemy last = finish.getEnemies().get(0); last.takeDamage(60);
        check(finish.selectStudent(angel(finish)) && finish.attack(last), "finish last enemy");
        check(finish.getEffects().size() == 1 && finish.isVictory(), "attack stops when battle ends");
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> {}, p -> {}, new game.model.GameSettings(), fixed(2));
                panel.start(new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL)), true);
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                Battle active = (Battle) battleField.get(panel); Student actor = angel(active);
                Field pointsField = BattlePanel.class.getDeclaredField("actionPoints"); pointsField.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Student, Double> points = (Map<Student, Double>) pointsField.get(panel);
                points.put(actor, (double) actor.getActionThreshold());
                check(active.selectStudent(actor), "Angel is selected for UI action");
                var refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel, 0, ""));
                panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel, 0, ""));
                check(points.get(actor) == 0.0 && active.getEffects().size() == 3, "three hits reset AP once");
                Field timer = BattlePanel.class.getDeclaredField("effectTimer"); timer.setAccessible(true);
                ((javax.swing.Timer) timer.get(panel)).stop();
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        System.out.println("Angel multi-hit checks passed");
    }
}
