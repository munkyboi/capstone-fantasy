import game.model.GameProgress;
import game.model.Student;
import game.view.AspectRatioPanel;
import game.view.InventoryPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Checks the inventory layout, viewport scaling and bag navigation. */
public final class InventoryUiTest {
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
    private static void key(InventoryPanel panel, String name) {
        panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name));
    }
    /** Runs the inventory interface checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
                progress.claimStageReward(); progress.advance(); progress.claimStageReward();
                InventoryPanel panel = new InventoryPanel(() -> { }); panel.open(progress);
                AspectRatioPanel viewport = new AspectRatioPanel(panel);
                viewport.setSize(960, 540); layout(viewport);
                Field profileField = InventoryPanel.class.getDeclaredField("profile"); profileField.setAccessible(true);
                JLabel profile = (JLabel) profileField.get(panel);
                check(profile.getIcon().getIconWidth() == 72 && profile.getText().contains("Charice"),
                        "selected student portrait appears at baseline size");
                Field rowsField = InventoryPanel.class.getDeclaredField("partyRows"); rowsField.setAccessible(true);
                JLabel[] rows = (JLabel[]) rowsField.get(panel);
                check(rows[0].getBackground().getRGB() != rows[1].getBackground().getRGB(), "selected party row is distinct");
                key(panel, "inventory-down");
                check(profile.getText().contains("Bogart") && rows[1].getBackground().getRGB() != rows[0].getBackground().getRGB(),
                        "party selection follows keyboard");
                Field scrollField = InventoryPanel.class.getDeclaredField("bagScroll"); scrollField.setAccessible(true);
                JScrollPane scroll = (JScrollPane) scrollField.get(panel); layout(viewport);
                int before = scroll.getVerticalScrollBar().getValue();
                key(panel, "inventory-bag-down");
                check(scroll.getVerticalScrollBar().getValue() > before, "bag scrolls by keyboard");
                viewport.setSize(1440, 810); layout(viewport);
                check(profile.getIcon().getIconWidth() == 108, "portrait scales with viewport");
                System.out.println("Inventory interface checks passed");
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
    }
}
