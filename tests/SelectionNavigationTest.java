import game.view.SelectionPanel;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Verifies keyboard navigation between the party cards and footer buttons. */
public final class SelectionNavigationTest {
    private static void key(JComponent c, String action) {
        c.getActionMap().get(action).actionPerformed(new ActionEvent(c, 0, action));
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int[] back = {0}, next = {0};
            SelectionPanel panel = new SelectionPanel(() -> back[0]++, party -> next[0]++);
            key(panel, "party-confirm"); key(panel, "party-right"); key(panel, "party-confirm");
            key(panel, "party-right"); key(panel, "party-confirm");
            key(panel, "party-continue"); key(panel, "party-left"); key(panel, "party-confirm");
            if (back[0] != 1 || next[0] != 0) { throw new AssertionError("Back must be reachable from footer"); }
            key(panel, "party-right"); key(panel, "party-confirm");
            if (next[0] != 1) { throw new AssertionError("Continue must remain reachable from footer"); }
            System.out.println("Character selection navigation checks passed");
        });
    }
}
