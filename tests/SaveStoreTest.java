import game.io.SaveException;
import game.io.SaveStore;
import game.model.GameProgress;
import game.model.Student;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Small regression check for both save formats and damaged files. */
public final class SaveStoreTest {
    private static void check(boolean value, String message) {
        if (!value) { throw new AssertionError(message); }
    }
    private static void sameState(GameProgress expected, GameProgress actual) {
        check(expected.getEncounter() == actual.getEncounter(), "encounter");
        check(expected.getKits() == actual.getKits(), "inventory");
        for (int i = 0; i < 3; i++) {
            Student a = expected.getParty().get(i), b = actual.getParty().get(i);
            check(a.getId() == b.getId() && a.getHp() == b.getHp(), "party and HP");
        }
    }
    private static void fails(SaveStore store, int slot, SaveStore.Format format) throws Exception {
        try { store.load(slot, format); throw new AssertionError("invalid save accepted"); }
        catch (SaveException expected) { check(expected.getMessage() != null, "message"); }
    }
    /** Runs the save checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("capstone-save-test-");
        try {
            SaveStore store = new SaveStore(directory);
            GameProgress progress = new GameProgress(List.of(0, 1, 3));
            progress.advance();
            Student injured = progress.getParty().get(0);
            injured.takeDamage(50); check(progress.useKit(injured), "use kit");
            progress.getParty().get(1).takeDamage(999);
            for (int slot = 1; slot <= 3; slot++) {
                for (SaveStore.Format format : SaveStore.Format.values()) {
                    store.save(slot, format, progress);
                    sameState(progress, store.load(slot, format));
                }
            }
            String text = Files.readString(directory.resolve("slot-1.txt"));
            check(text.contains("Zack") && text.contains("firstAidKits") && text.contains("level"), "readable save");
            Files.writeString(directory.resolve("slot-1.txt"), "version=1\nstudent.1.id=bad\n");
            fails(store, 1, SaveStore.Format.TEXT);
            sameState(progress, store.load(1, SaveStore.Format.BINARY));
            Files.writeString(directory.resolve("slot-2.dat"), "broken");
            fails(store, 2, SaveStore.Format.BINARY);
            Files.delete(directory.resolve("slot-3.txt"));
            fails(store, 3, SaveStore.Format.TEXT);
            System.out.println("Save and load checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                });
            }
        }
    }
}
