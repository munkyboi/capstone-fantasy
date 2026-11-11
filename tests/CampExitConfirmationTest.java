import game.model.GameProgress;
import game.view.CampPanel;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.SwingUtilities;

/** Checks the camp exit dialog and its keyboard actions. */
public final class CampExitConfirmationTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void key(CampPanel panel, String name) { panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, "")); }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int[] menu = {0}, inventory = {0}, save = {0}, route = {0};
            var progress = new GameProgress(List.of(0, 1, 2));
            CampPanel panel = new CampPanel(p -> route[0]++, () -> menu[0]++, p -> save[0]++, p -> inventory[0]++);
            panel.open(progress, false); int kits = progress.getKits();
            key(panel, "camp-menu"); check(menu[0] == 0, "escape opens confirmation");
            key(panel, "camp-inventory"); key(panel, "camp-save"); key(panel, "camp-continue"); key(panel, "camp-equip");
            check(inventory[0] == 0 && save[0] == 0 && route[0] == 0 && progress.getKits() == kits, "dialog blocks camp actions");
            key(panel, "camp-use"); check(menu[0] == 0, "default No stays in camp");
            key(panel, "camp-menu"); key(panel, "camp-right"); key(panel, "camp-menu");
            check(menu[0] == 0, "escape cancels dialog");
            key(panel, "camp-menu"); key(panel, "camp-use"); check(menu[0] == 0, "No is default every time");
            key(panel, "camp-menu"); key(panel, "camp-right"); key(panel, "camp-use"); check(menu[0] == 1, "Yes returns to menu");
            panel.open(progress, false); key(panel, "camp-allocate"); key(panel, "camp-menu");
            check(menu[0] == 1, "escape closes allocation before opening exit dialog");
        });
        System.out.println("Camp exit confirmation checks passed");
    }
}
