import game.view.AspectRatioPanel;
import game.view.MainMenuPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/** Checks menu layout and keyboard selection at normal and large window sizes. */
public final class MainMenuLayoutTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container) layout((Container) child);
    }
    private static void collect(Container parent, List<JButton> buttons) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton) buttons.add((JButton) child);
            else if (child instanceof Container) collect((Container) child, buttons);
        }
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int[] actions = {0, 0, 0};
            MainMenuPanel menu = new MainMenuPanel(null, () -> actions[0]++, () -> actions[1]++, () -> actions[2]++);
            AspectRatioPanel frame = new AspectRatioPanel(menu);
            List<JButton> buttons = new ArrayList<>(); collect(menu, buttons);
            check(buttons.size() == 4, "four menu options");
            for (int width : new int[]{960, 1648}) {
                frame.setSize(width, width * 3 / 4); layout(frame);
                Container box = buttons.get(0).getParent();
                check(box.getWidth() == 440, "menu width remains fixed at " + width);
                check(Math.abs(SwingUtilities.convertPoint(box, box.getWidth() / 2, 0, menu).x - menu.getWidth() / 2) <= 1, "menu stays centered");
                for (JButton button : buttons) {
                    check(button.getIcon() == null && button.getHorizontalAlignment() == JButton.CENTER, "labels centered independently");
                    check(button.isBorderPainted() && button.getBorder().getBorderInsets(button).left >= 2, "full visible borders");
                }
            }
            menu.getActionMap().get("menu-down").actionPerformed(new ActionEvent(menu, 0, ""));
            menu.getActionMap().get("menu-confirm").actionPerformed(new ActionEvent(menu, 0, ""));
            check(actions[1] == 1 && Boolean.TRUE.equals(buttons.get(1).getClientProperty("menu.selected")), "keyboard selection and activation");
        });
        System.out.println("Main menu layout checks passed");
    }
}
