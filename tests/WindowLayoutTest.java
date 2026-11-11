import game.view.AspectRatioPanel;
import java.awt.Rectangle;
import javax.swing.JPanel;

/** Checks that the game area maintains its aspect ratio in resized windows. */
public final class WindowLayoutTest {
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    /** Runs the window layout checks.
     * @param args unused
     */
    public static void main(String[] args) {
        JPanel screens = new JPanel();
        AspectRatioPanel frame = new AspectRatioPanel(screens);
        frame.setSize(1200, 900); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(0, 112, 1200, 675)), "letterboxes a tall window");
        frame.setSize(1800, 900); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(100, 0, 1600, 900)), "pillarboxes a wide window");
        frame.setSize(960, 540); frame.doLayout();
        check(screens.getBounds().equals(new Rectangle(0, 0, 960, 540)), "fills the initial window");
        System.out.println("Window layout checks passed");
    }
}
