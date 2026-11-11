import game.model.Battle;
import game.model.GameProgress;
import game.model.Student;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Verifies that target selection is reflected in the monster status list. */
public final class TargetHighlightTest {
    private static final int GOLD = 0xfada8f;
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(BattlePanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
    }
    private static BufferedImage draw(JPanel scene) {
        scene.setSize(960, 320);
        BufferedImage result = new BufferedImage(960, 320, BufferedImage.TYPE_INT_ARGB);
        var graphics = result.createGraphics();
        try { scene.paint(graphics); } finally { graphics.dispose(); }
        return result;
    }
    private static boolean highlighted(BufferedImage image, int row) {
        return (image.getRGB(960 - 243, 14 + row * 36) & 0xffffff) == GOLD;
    }
    /** Runs target list checks.
     * @param args unused
     * @throws Exception if checks fail
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                Field pointsField = BattlePanel.class.getDeclaredField("actionPoints"); pointsField.setAccessible(true);
                BattlePanel panel = TestBattles.twoEnemies(() -> { });
                panel.start(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL));
                JPanel scene = (JPanel) sceneField.get(panel);
                check(!highlighted(draw(scene), 0), "no target highlighted initially");
                key(panel, "battle-confirm");
                BufferedImage first = draw(scene);
                check(highlighted(first, 0) && !highlighted(first, 1), "first target highlighted");
                key(panel, "battle-right");
                BufferedImage second = draw(scene);
                check(!highlighted(second, 0) && highlighted(second, 1), "highlight follows target");
                key(panel, "battle-cancel");
                check(!highlighted(draw(scene), 1), "highlight clears on cancel");
                panel.start(new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL)), true);
                Battle battle = (Battle) battleField.get(panel);
                Student zack = battle.getParty().stream().filter(student -> student.getId() == Student.ZACK).findFirst().orElseThrow();
                check(battle.selectStudent(zack), "select Zack");
                @SuppressWarnings("unchecked") Map<Student, Double> points = (Map<Student, Double>) pointsField.get(panel);
                points.put(zack, (double) zack.getActionThreshold());
                var refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                key(panel, "battle-down"); key(panel, "battle-confirm");
                BufferedImage group = draw(scene);
                check(highlighted(group, 0) && highlighted(group, 1), "all-target ability highlights both monsters");
                battle.getEnemies().get(1).takeDamage(999);
                BufferedImage withKo = draw(scene);
                check(highlighted(withKo, 0) && !highlighted(withKo, 1), "defeated monsters are not highlighted");
                System.out.println("Target highlight checks passed");
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
    }
}
