import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.Student;
import game.view.BattlePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Checks outcome overlays and the victory celebration. */
public final class BattleOutcomeTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(JPanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
    private static BufferedImage paint(JPanel panel) {
        panel.setSize(960, 540); layout(panel);
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics g = image.getGraphics();
        try { panel.paint(g); } finally { g.dispose(); }
        return image;
    }
    /** Runs the outcome checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                Field sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                Field startField = BattlePanel.class.getDeclaredField("victoryStartedNanos"); startField.setAccessible(true);
                Field timerField = BattlePanel.class.getDeclaredField("victoryTimer"); timerField.setAccessible(true);
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true);
                int[] continued = {0}, returned = {0};
                BattlePanel panel = TestBattles.twoEnemies(() -> returned[0]++, progress -> continued[0]++, new game.model.GameSettings());
                panel.start(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL));
                Battle battle = (Battle) battleField.get(panel);
                BufferedImage before = paint(panel);
                JPanel scenePanel = (JPanel) sceneField.get(panel);
                java.awt.Point alert = SwingUtilities.convertPoint(scenePanel, scenePanel.getWidth() / 2,
                        Math.max(18, scenePanel.getHeight() / 5) + 1, panel);
                Student down = battle.getParty().get(2); down.takeDamage(999);
                List<Enemy> enemies = battle.getEnemies();
                enemies.get(1).takeDamage(999); enemies.get(0).takeDamage(999);
                refresh.invoke(panel);
                check(battle.isVictory() && ((Timer) timerField.get(panel)).isRunning(), "victory starts celebration");
                Field progressField = BattlePanel.class.getDeclaredField("progress"); progressField.setAccessible(true);
                GameProgress rewardProgress = (GameProgress) progressField.get(panel);
                Field logField = BattlePanel.class.getDeclaredField("log"); logField.setAccessible(true);
                javax.swing.JTextArea log = (javax.swing.JTextArea) logField.get(panel);
                Field rewardsField = BattlePanel.class.getDeclaredField("victoryRewardItems"); rewardsField.setAccessible(true);
                java.util.List<?> rewards = (java.util.List<?>) rewardsField.get(panel);
                check(rewards.contains("focus_charm") && rewards.contains("scholar_staff")
                        && rewards.contains("hunter_crossbows") && rewards.contains("champion_wraps")
                        && rewards.stream().filter(id -> !id.equals("first_aid_kit")).count() == 4,
                        "popup includes exactly the awarded party equipment");
                check(!log.getText().contains("Victory reward:"), "rewards appear in the popup instead of the battle log");
                int gearCount = rewardProgress.getInventory().getEquipmentIds().size();
                refresh.invoke(panel);
                check(rewardProgress.getInventory().getEquipmentIds().size() == gearCount, "refresh does not grant rewards twice");
                check(paint(panel).getRGB(alert.x, alert.y) != before.getRGB(alert.x, alert.y), "victory overlay is drawn");
                Object scene = sceneField.get(panel);
                Method lift = scene.getClass().getDeclaredMethod("victoryLift", Student.class, int.class); lift.setAccessible(true);
                startField.setLong(panel, System.nanoTime() - 150_000_000L);
                check((int) lift.invoke(scene, battle.getParty().get(0), 0) < 0, "survivor celebrates");
                check((int) lift.invoke(scene, down, 2) == 0, "knocked-out student does not celebrate");
                key(panel, "battle-confirm");
                check(continued[0] == 1, "victory continues on Enter");
                ((Timer) timerField.get(panel)).stop();
                panel.start(new GameProgress(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL)), false);
                battle = (Battle) battleField.get(panel);
                BufferedImage active = paint(panel);
                for (Student student : battle.getParty()) { student.takeDamage(999); }
                refresh.invoke(panel);
                check(battle.isDefeat() && !((Timer) timerField.get(panel)).isRunning(), "defeat does not celebrate");
                check(paint(panel).getRGB(alert.x, alert.y) != active.getRGB(alert.x, alert.y), "defeat overlay is drawn");
                key(panel, "battle-confirm");
                check(returned[0] == 1, "defeat returns to menu on Enter");
                System.out.println("Battle outcome checks passed");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
    }
}
