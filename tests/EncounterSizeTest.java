import game.model.Battle;
import game.model.EncounterRules;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.Student;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Checks stage-specific encounter sizes and six-target navigation. */
public final class EncounterSizeTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static Random roll(int offset) {
        return new Random(12) {
            @Override public int nextInt(int bound) { return bound == 3 ? offset : super.nextInt(bound); }
        };
    }
    private static void key(BattlePanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
    }
    /** Runs encounter and targeting checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        int[] minimum = {1, 2, 3, 4, 6}, maximum = {3, 4, 5, 6, 6};
        for (int stage = 0; stage < minimum.length; stage++) {
            check(EncounterRules.rollMonsterCount(stage, roll(0)) == minimum[stage], "stage minimum " + (stage + 1));
            check(EncounterRules.rollMonsterCount(stage, roll(2)) == maximum[stage], "stage maximum " + (stage + 1));
        }
        for (int offset = 0; offset < 3; offset++) {
            GameProgress first = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
            Battle wisps = new Battle(first, roll(offset));
            check(wisps.getEnemies().size() == 1 + offset && wisps.getEnemies().stream().noneMatch(Enemy::isSentinel), "stage 1 size");
            GameProgress second = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE)); second.advance();
            Battle guardian = new Battle(second, roll(offset));
            check(guardian.getEnemies().size() == 2 + offset && guardian.getEnemies().stream().filter(Enemy::isSentinel).count() == 1,
                    "stage 2 size and sentinel");
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> { }, progress -> { }, new GameSettings(), roll(2));
                panel.start(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
                Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true);
                Battle battle = (Battle) field.get(panel);
                Field enemiesField = Battle.class.getDeclaredField("enemies"); enemiesField.setAccessible(true);
                @SuppressWarnings("unchecked") List<Enemy> enemies = (List<Enemy>) enemiesField.get(battle);
                Field enemyPointsField = BattlePanel.class.getDeclaredField("enemyPoints"); enemyPointsField.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Enemy, Double> enemyPoints = (Map<Enemy, Double>) enemyPointsField.get(panel);
                while (enemies.size() < 6) {
                    Enemy enemy = new Enemy("Library Wisp " + (char) ('A' + enemies.size()));
                    enemies.add(enemy); enemyPoints.put(enemy, 0.0);
                }
                Field target = BattlePanel.class.getDeclaredField("targetIndex"); target.setAccessible(true);
                key(panel, "battle-confirm");
                for (int i = 1; i < 6; i++) {
                    key(panel, "battle-right");
                    check(target.getInt(panel) == i, "can select monster " + (i + 1));
                }
                Field sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                javax.swing.JPanel scene = (javax.swing.JPanel) sceneField.get(panel);
                scene.setSize(960, 540);
                var image = new java.awt.image.BufferedImage(960, 540, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics();
                try { scene.paint(graphics); } finally { graphics.dispose(); }
                check((image.getRGB(737, 86) & 0xffffff) == 0xfada8f, "sixth target highlighted in second column");
                int before = enemies.get(5).getHp();
                key(panel, "battle-confirm");
                check(enemies.get(5).getHp() < before, "sixth monster can be attacked");
                System.out.println("Encounter sizes and six-target checks passed");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
    }
}
