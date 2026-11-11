import game.view.AspectRatioPanel;
import game.view.BattlePanel;
import game.model.Student;
import java.util.List;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import javax.swing.JPanel;

/** Checks that the game area maintains its aspect ratio in resized windows. */
public final class WindowLayoutTest {
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static void layout(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container) { layout((java.awt.Container) child); }
        }
    }
    private static void checkBattle(int width, int height) throws Exception {
        BattlePanel battle = new BattlePanel(() -> { });
        battle.start(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        battle.setSize(width, height); layout(battle);
        var sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
        JPanel scene = (JPanel) sceneField.get(battle);
        check(Math.abs(scene.getWidth() * 9 - scene.getHeight() * 16) <= 16, "battlefield remains 16:9");
        Rectangle sceneBounds = SwingUtilities.convertRectangle(scene.getParent(), scene.getBounds(), battle);
        check(sceneBounds.y + sceneBounds.height < battle.getHeight(), "battle controls remain below the battlefield");
    }
    /** Runs the window layout checks.
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        JPanel screens = new JPanel();
        AspectRatioPanel frame = new AspectRatioPanel(screens);
        frame.setSize(1200, 900); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(0, 112, 1200, 675)), "letterboxes a tall window");
        frame.setSize(1800, 900); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(100, 0, 1600, 900)), "pillarboxes a wide window");
        frame.setSize(960, 540); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(0, 0, 960, 540)), "fills the initial window");
        SwingUtilities.invokeAndWait(() -> {
            try { checkBattle(960, 720); checkBattle(1440, 900); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        System.out.println("Window layout checks passed");
    }
}
