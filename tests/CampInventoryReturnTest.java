import game.model.GameProgress;
import game.model.Student;
import game.view.CampPanel;
import game.view.InventoryPanel;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Checks camp healing and selection across an inventory equipment change. */
public final class CampInventoryReturnTest {
    private static void key(JComponent panel, String name) {
        panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name));
    }
    private static void check(boolean value, String message) {
        if (!value) { throw new AssertionError(message); }
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
                GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
                Student first = progress.getParty().get(0), second = progress.getParty().get(1);
                first.takeDamage(30); second.takeDamage(30);
                CampPanel camp = new CampPanel(ignored -> { }, () -> { });
                camp.open(progress, false);
                key(camp, "camp-use");
                check(progress.getKits() == 2 && first.getHp() == first.getMaxHp(), "first kit heals once");
                InventoryPanel bag = new InventoryPanel(() -> { }); bag.open(progress);
                key(bag, "inventory-right");
                key(bag, "inventory-confirm");
                key(bag, "inventory-down");
                key(bag, "inventory-confirm");
                check(first.getDefense() > first.getBaseDefense(), "equipment change persists");
                camp.open(progress, false);
                key(camp, "camp-down");
                key(camp, "camp-use");
                check(progress.getKits() == 1 && second.getHp() == second.getMaxHp(), "second kit heals the selected student");
                System.out.println("Camp inventory return checks passed");
        });
    }
}
