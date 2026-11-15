import game.io.SaveStore;
import game.model.GameProgress;
import game.view.SaveSlotPanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;

/** Verifies that save selection follows the current game's last successful save or load. */
public final class SaveSlotSelectionTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("save-slot-selection-");
        try {
            SwingUtilities.invokeAndWait(() -> {
                GameProgress[] loaded = new GameProgress[1];
                SaveSlotPanel panel = new SaveSlotPanel(new SaveStore(directory), () -> { }, result -> loaded[0] = result);
                GameProgress game = new GameProgress(List.of(0, 1, 3));
                game.advance();
                panel.openSave(game); check(panel, 0, SaveStore.Format.TEXT);
                act(panel, "slot-down"); act(panel, "slot-down"); act(panel, "slot-right");
                act(panel, "slot-confirm");
                if (!Files.exists(directory.resolve("slot-3.dat"))) throw new AssertionError("First save failed: " + info(panel));
                panel.openSave(game); check(panel, 2, SaveStore.Format.BINARY);
                act(panel, "slot-up"); act(panel, "slot-confirm");
                panel.openSave(game); check(panel, 1, SaveStore.Format.BINARY);
                panel.openSave(new GameProgress(List.of(0))); check(panel, 0, SaveStore.Format.TEXT);
                panel.openLoad(); act(panel, "slot-right"); act(panel, "slot-down"); act(panel, "slot-down");
                act(panel, "slot-confirm");
                if (loaded[0] == null) throw new AssertionError("Game did not load");
                panel.openSave(loaded[0]); check(panel, 2, SaveStore.Format.BINARY);
                act(panel, "slot-confirm"); act(panel, "slot-back");
                panel.openSave(loaded[0]); check(panel, 2, SaveStore.Format.BINARY);
            });
        } finally {
            try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.delete(file); }
            Files.delete(directory);
        }
        System.out.println("Save slot selection checks passed");
    }
    private static void act(SaveSlotPanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
    }
    private static String info(SaveSlotPanel panel) {
        try {
            Field field = SaveSlotPanel.class.getDeclaredField("info"); field.setAccessible(true);
            return ((javax.swing.JLabel) field.get(panel)).getText();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void check(SaveSlotPanel panel, int slot, SaveStore.Format format) {
        try {
            Field slotField = SaveSlotPanel.class.getDeclaredField("slot"); slotField.setAccessible(true);
            Field formatField = SaveSlotPanel.class.getDeclaredField("format"); formatField.setAccessible(true);
            if (slotField.getInt(panel) != slot || formatField.get(panel) != format) throw new AssertionError("Incorrect default selection");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
