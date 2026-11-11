import game.model.GameProgress;
import game.view.BattlePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Checks that the ambush banner is visible briefly and resets between battles. */
public final class AmbushAlertTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
    /** Runs the alert timing and rendering checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        Field flag = BattlePanel.class.getDeclaredField("showAmbush"); flag.setAccessible(true);
        Field timerField = BattlePanel.class.getDeclaredField("ambushAlertTimer"); timerField.setAccessible(true);
        BattlePanel[] holder = new BattlePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> { }); holder[0] = panel;
                panel.start(new GameProgress(List.of(0, 1, 2)), true);
                check(flag.getBoolean(panel), "ambush banner shown immediately");
                Timer timer = (Timer) timerField.get(panel);
                check(timer.getInitialDelay() == 3000 && !timer.isRepeats() && timer.isRunning(), "three-second one-shot timer");
                panel.setSize(960, 540); layout(panel);
                BufferedImage visible = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
                panel.paint(visible.getGraphics());
                flag.setBoolean(panel, false);
                BufferedImage hidden = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
                panel.paint(hidden.getGraphics());
                check(visible.getRGB(480, 90) != hidden.getRGB(480, 90), "banner renders in upper center of battlefield");
                flag.setBoolean(panel, true);
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        Thread.sleep(3400);
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = holder[0];
                check(!flag.getBoolean(panel), "banner disappears after three seconds");
                panel.start(new GameProgress(List.of(0, 1, 2)), true);
                panel.start(new GameProgress(List.of(0, 1, 2)), false);
                check(!flag.getBoolean(panel) && !((Timer) timerField.get(panel)).isRunning(),
                        "normal battle clears any previous alert");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Ambush alert checks passed");
    }
}
