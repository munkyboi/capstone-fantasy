import game.io.SaveStore;
import game.view.SaveSlotPanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** Checks timestamps for existing saves in each format. */
public final class SaveSlotTimestampTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("save-slot-time-");
        try {
            Path text = Files.writeString(directory.resolve("slot-2.txt"), "existing save");
            Path binary = Files.writeString(directory.resolve("slot-2.dat"), "other save");
            Files.setLastModifiedTime(text, timestamp(2026, 8, 20, 13, 54));
            Files.setLastModifiedTime(binary, timestamp(2025, 1, 3, 0, 5));
            SwingUtilities.invokeAndWait(() -> {
                SaveSlotPanel panel = new SaveSlotPanel(new SaveStore(directory), () -> { }, ignored -> { });
                panel.openLoad();
                checkPointers(panel, 0);
                check(label(panel, 1), "20 Aug 2026 1:54 PM");
                check(label(panel, 0), "Empty");
                panel.getActionMap().get("slot-down").actionPerformed(new ActionEvent(panel, 0, "slot-down"));
                checkPointers(panel, 1);
                panel.getActionMap().get("slot-right").actionPerformed(new ActionEvent(panel, 0, "slot-right"));
                check(label(panel, 1), "3 Jan 2025 12:05 AM");
            });
        } finally {
            try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.delete(file); }
            Files.delete(directory);
        }
        System.out.println("Save slot timestamp checks passed");
    }
    private static FileTime timestamp(int year, int month, int day, int hour, int minute) {
        return FileTime.from(LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant());
    }
    private static String label(SaveSlotPanel panel, int slot) {
        try {
            Field field = SaveSlotPanel.class.getDeclaredField("slots"); field.setAccessible(true);
            return ((JLabel[]) field.get(panel))[slot].getText();
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void checkPointers(SaveSlotPanel panel, int selected) {
        try {
            Field slotField = SaveSlotPanel.class.getDeclaredField("slots"); slotField.setAccessible(true);
            Field pointerField = SaveSlotPanel.class.getDeclaredField("pointers"); pointerField.setAccessible(true);
            JLabel[] slots = (JLabel[]) slotField.get(panel), pointers = (JLabel[]) pointerField.get(panel);
            for (int i = 0; i < slots.length; i++) {
                if (slots[i].getIcon() != null || slots[i] == pointers[i]
                        || slots[i].getParent() != pointers[i].getParent()) throw new AssertionError("Pointer must be separate from slot");
                java.awt.image.BufferedImage image = (java.awt.image.BufferedImage) ((javax.swing.ImageIcon) pointers[i].getIcon()).getImage();
                boolean visible = false;
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) visible |= (image.getRGB(x, y) >>> 24) != 0;
                if (visible != (i == selected)) throw new AssertionError("Incorrect selected pointer");
            }
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
    private static void check(String actual, String expected) {
        if (!actual.contains(expected)) throw new AssertionError("Expected " + expected + " in " + actual);
    }
}
