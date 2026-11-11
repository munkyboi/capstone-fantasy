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
        JPanel controls = (JPanel) scene.getParent().getComponent(0);
        Rectangle controlsBounds = SwingUtilities.convertRectangle(controls.getParent(), controls.getBounds(), battle);
        check(sceneBounds.intersects(controlsBounds)
                && controlsBounds.y + controlsBounds.height == sceneBounds.y + sceneBounds.height,
                "battle controls overlay the bottom of the battlefield");
        JPanel info = (JPanel) ((java.awt.BorderLayout) controls.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
        var image = new java.awt.image.BufferedImage(info.getWidth(), info.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { info.paint(graphics); } finally { graphics.dispose(); }
        check((image.getRGB(info.getWidth() - 12, 10) >>> 24) == 204, "panel background is 80 percent opaque");
    }
    private static void checkFontScaling() throws Exception {
        BattlePanel battle = new BattlePanel(() -> { });
        battle.start(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        var statusField = BattlePanel.class.getDeclaredField("status"); statusField.setAccessible(true);
        var barsField = BattlePanel.class.getDeclaredField("apBars"); barsField.setAccessible(true);
        javax.swing.JLabel status = (javax.swing.JLabel) statusField.get(battle);
        javax.swing.JProgressBar bar = ((javax.swing.JProgressBar[]) barsField.get(battle))[0];
        battle.setSize(960, 720); layout(battle);
        float base = status.getFont().getSize2D(); int baseBar = bar.getPreferredSize().width;
        battle.setSize(1440, 900); layout(battle);
        check(Math.abs(status.getFont().getSize2D() - base * 1.5) < 0.2, "battle text scales with viewport");
        check(bar.getPreferredSize().width == Math.round(baseBar * 1.5f), "AP bar scales alongside text");
        battle.setSize(2000, 900); layout(battle);
        check(Math.abs(status.getFont().getSize2D() - base * (1600.0 / 960)) < 0.2, "font follows viewport rather than window width");
        battle.setSize(960, 720); layout(battle);
        check(Math.abs(status.getFont().getSize2D() - base) < 0.2 && bar.getPreferredSize().width == baseBar,
                "font and controls return to baseline after resizing back");
    }
    private static void checkCampPortraits() throws Exception {
        var camp = new game.view.CampPanel(progress -> { }, () -> { });
        var progress = new game.model.GameProgress(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL));
        progress.advance(); camp.open(progress, false);
        var viewport = new AspectRatioPanel(camp);
        viewport.setSize(960, 540); layout(viewport);
        check(camp.getWidth() == 960 && camp.getHeight() == 540, "camp fills normal viewport");
        viewport.setSize(1440, 810); layout(viewport);
        check(camp.getWidth() == 1440 && camp.getHeight() == 810, "camp fills larger viewport");
        camp.getActionMap().get("camp-down").actionPerformed(new java.awt.event.ActionEvent(camp, 0, ""));
        check(camp.getWidth() == 1440, "camp selection preserves viewport size");
        viewport.setSize(960, 540); layout(viewport);
        check(camp.getWidth() == 960, "camp returns to baseline after resize");
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
            try { checkBattle(960, 720); checkBattle(1440, 900); checkFontScaling(); checkCampPortraits(); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        System.out.println("Window layout checks passed");
    }
}
