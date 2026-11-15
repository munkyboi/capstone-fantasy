package game.view;

import javax.accessibility.AccessibleContext;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

/** Text components that tolerate stale macOS accessibility range requests. */
final class AccessibleTextComponents {
    private AccessibleTextComponents() { }

    static boolean validRange(int start, int end, int length) {
        return Math.min(start, end) >= 0 && Math.max(start, end) <= length;
    }

    static class Pane extends JTextPane {
        @Override public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) { accessibleContext = new SafeAccessiblePane(); }
            return accessibleContext;
        }

        protected class SafeAccessiblePane extends AccessibleJEditorPane {
            @Override public String getTextRange(int start, int end) {
                if (!validRange(start, end, getDocument().getLength())) { return null; }
                try { return super.getTextRange(start, end); }
                catch (IllegalArgumentException staleRange) { return null; }
            }
        }
    }

    static class Area extends JTextArea {
        @Override public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) { accessibleContext = new SafeAccessibleArea(); }
            return accessibleContext;
        }

        protected class SafeAccessibleArea extends AccessibleJTextArea {
            @Override public String getTextRange(int start, int end) {
                if (!validRange(start, end, getDocument().getLength())) { return null; }
                try { return super.getTextRange(start, end); }
                catch (IllegalArgumentException staleRange) { return null; }
            }
        }
    }
}
