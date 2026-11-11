package game.io;

import game.model.GameProgress;
import game.model.Student;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Reads and writes three independent slots in text or binary format. */
public final class SaveStore {
    private static final long MAX_SAVE_BYTES = 65536;
    private final Path directory;
    /** Supported save formats. */
    public enum Format {
        /** Human-readable save file. */
        TEXT,
        /** Serialized save file. */
        BINARY
    }
    /** Creates a store rooted at a directory.
     * @param directory directory for save files
     */
    public SaveStore(Path directory) { this.directory = directory; }
    /** Returns the normal save directory.
     * @return the normal save directory
     */
    public static Path defaultDirectory() { return Path.of(System.getProperty("user.home"), ".capstone-fantasy", "saves"); }
    private Path file(int slot, Format format) {
        if (slot < 1 || slot > 3 || format == null) { throw new IllegalArgumentException("Invalid save slot"); }
        return directory.resolve("slot-" + slot + (format == Format.TEXT ? ".txt" : ".dat"));
    }
    /** Checks whether a slot has a file in the chosen format.
     * @param slot slot number from one to three
     * @param format save format
     * @return true when the file exists
     */
    public boolean exists(int slot, Format format) { return Files.isRegularFile(file(slot, format)); }
    /** Saves a camp state, replacing only the chosen slot and format.
     * @param slot slot number from one to three
     * @param format save format
     * @param progress party state
     * @throws SaveException if the state cannot be saved
     */
    public void save(int slot, Format format, GameProgress progress) throws SaveException {
        Path temp = null;
        try {
            GameProgress checked = validate(progress);
            Path destination = file(slot, format);
            Files.createDirectories(directory);
            temp = Files.createTempFile(directory, "save-", ".tmp");
            if (format == Format.TEXT) {
                Properties properties = new Properties();
                properties.setProperty("version", "1");
                properties.setProperty("encounter", Integer.toString(checked.getEncounter()));
                properties.setProperty("inventory.firstAidKits", Integer.toString(checked.getKits()));
                for (int i = 0; i < 3; i++) {
                    Student student = checked.getParty().get(i);
                    String prefix = "student." + (i + 1) + ".";
                    properties.setProperty(prefix + "id", Integer.toString(student.getId()));
                    properties.setProperty(prefix + "name", student.getName());
                    properties.setProperty(prefix + "hp", Integer.toString(student.getHp()));
                    properties.setProperty(prefix + "maxHp", Integer.toString(student.getMaxHp()));
                    properties.setProperty(prefix + "level", Integer.toString(student.getLevel()));
                }
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Capstone Fantasy camp save v1");
                }
            } else {
                try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(temp))) {
                    stream.writeObject(checked);
                }
            }
            try { Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | RuntimeException ex) {
            throw new SaveException("Could not save this slot. Check the save folder and try again.", ex);
        } finally {
            if (temp != null) { try { Files.deleteIfExists(temp); } catch (IOException ignored) { temp.toFile().deleteOnExit(); } }
        }
    }
    /** Loads and validates a saved camp state.
     * @param slot slot number from one to three
     * @param format save format
     * @return restored game state
     * @throws SaveException if the save is missing, unreadable, or invalid
     */
    public GameProgress load(int slot, Format format) throws SaveException {
        try {
            Path path = file(slot, format);
            if (!Files.isRegularFile(path)) { throw new SaveException("No save exists in this slot and format.", null); }
            if (Files.size(path) > MAX_SAVE_BYTES) { throw new IOException("Save is too large"); }
            if (format == Format.BINARY) {
                try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(path))) {
                    stream.setObjectInputFilter(info -> {
                        if (info.depth() > 8 || info.references() > 100 || info.streamBytes() > MAX_SAVE_BYTES
                                || info.arrayLength() > 16) { return ObjectInputFilter.Status.REJECTED; }
                        Class<?> type = info.serialClass();
                        if (type == null) { return ObjectInputFilter.Status.UNDECIDED; }
                        if (type == GameProgress.class || type == Student.class || type == ArrayList.class
                                || type == Object[].class) { return ObjectInputFilter.Status.ALLOWED; }
                        return ObjectInputFilter.Status.REJECTED;
                    });
                    Object object = stream.readObject();
                    if (!(object instanceof GameProgress)) { throw new IOException("Unexpected save content"); }
                    return validate((GameProgress) object);
                }
            }
            Properties properties = new Properties();
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
            if (!"1".equals(properties.getProperty("version"))) { throw new IOException("Unsupported save version"); }
            List<Integer> ids = new ArrayList<>();
            List<Integer> hp = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                String prefix = "student." + i + ".";
                int id = Integer.parseInt(properties.getProperty(prefix + "id"));
                Student expected = new Student(id);
                if (!expected.getName().equals(properties.getProperty(prefix + "name"))
                        || expected.getMaxHp() != Integer.parseInt(properties.getProperty(prefix + "maxHp"))
                        || expected.getLevel() != Integer.parseInt(properties.getProperty(prefix + "level"))) {
                    throw new IOException("Invalid student information");
                }
                ids.add(id); hp.add(Integer.parseInt(properties.getProperty(prefix + "hp")));
            }
            return GameProgress.restore(ids, hp, Integer.parseInt(properties.getProperty("inventory.firstAidKits")),
                    Integer.parseInt(properties.getProperty("encounter")));
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            throw new SaveException("This save is damaged or cannot be read.", ex);
        }
    }
    private static GameProgress validate(GameProgress progress) {
        List<Integer> ids = new ArrayList<>();
        List<Integer> hp = new ArrayList<>();
        for (Student student : progress.getParty()) {
            Student expected = new Student(student.getId());
            if (!expected.getName().equals(student.getName()) || expected.getMaxHp() != student.getMaxHp()
                    || expected.getAttack() != student.getAttack() || expected.getDefense() != student.getDefense()
                    || expected.getSpeed() != student.getSpeed() || expected.getLevel() != student.getLevel()) {
                throw new IllegalArgumentException("Invalid student information");
            }
            ids.add(student.getId()); hp.add(student.getHp());
        }
        return GameProgress.restore(ids, hp, progress.getKits(), progress.getEncounter());
    }
}
