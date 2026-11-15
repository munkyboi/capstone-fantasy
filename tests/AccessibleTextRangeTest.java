package game.view;

import javax.accessibility.AccessibleEditableText;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/** Checks valid and stale accessibility requests after text changes. */
public final class AccessibleTextRangeTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            check(new AccessibleTextComponents.Pane());
            check(new AccessibleTextComponents.Area());
        });
        System.out.println("Accessible text range checks passed");
    }

    private static void check(JTextComponent component) {
        component.setText("Hello");
        AccessibleEditableText accessible = component.getAccessibleContext().getAccessibleEditableText();
        if (!"Hello".equals(accessible.getTextRange(0, 5))) { throw new AssertionError("Valid range"); }
        component.setText("Hi");
        if (accessible.getTextRange(0, 5) != null || accessible.getTextRange(-1, 1) != null) {
            throw new AssertionError("Stale ranges must be unavailable");
        }
        if (!"Hi".equals(accessible.getTextRange(0, 2))) { throw new AssertionError("Updated range"); }
    }
}
