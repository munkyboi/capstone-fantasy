package game.io;

import game.model.GameProgress;
import game.model.Student;
import game.model.Hero;
import game.model.GameCharacter;
import game.model.Inventory;
import game.model.Equipment;
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
    private static final int LEGACY_ANGEL_ATTACK = 10;
    private static final int LEGACY_ANGEL_SPEED = 9;
    private static final int LEGACY_BOGART_SPEED = 7;
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
                properties.setProperty("version", "6");
                properties.setProperty("encounter", Integer.toString(checked.getEncounter()));
                properties.setProperty("inventory.petot", Integer.toString(checked.getPetot()));
                properties.setProperty("inventory.firstAidKits", Integer.toString(checked.getKits()));
                properties.setProperty("inventory.campusPass", "true");
                properties.setProperty("inventory.equipment", String.join(",", checked.getInventory().getEquipmentIds()));
                for (int stage = 0; stage < 5; stage++) {
                    properties.setProperty("reward." + stage, Boolean.toString(checked.getInventory().isRewardClaimed(stage)));
                }
                for (int i = 0; i < 3; i++) {
                    Student student = checked.getParty().get(i);
                    String prefix = "student." + (i + 1) + ".";
                    properties.setProperty(prefix + "id", Integer.toString(student.getId()));
                    properties.setProperty(prefix + "name", student.getName());
                    properties.setProperty(prefix + "hp", Integer.toString(student.getHp()));
                    properties.setProperty(prefix + "sp", Integer.toString(student.getSp()));
                    properties.setProperty(prefix + "maxHp", Integer.toString(student.getMaxHp()));
                    properties.setProperty(prefix + "level", Integer.toString(student.getLevel()));
                    properties.setProperty(prefix + "experience", Integer.toString(student.getExperience()));
                    for (Student.Stat stat : Student.Stat.values()) {
                        properties.setProperty(prefix + "allocated." + stat.name(), Integer.toString(student.getAllocatedStatPoints(stat)));
                    }
                    for (Equipment.Slot gearSlot : Equipment.Slot.values()) {
                        String id = student.getEquipmentId(gearSlot);
                        properties.setProperty(prefix + gearSlot.name().toLowerCase(java.util.Locale.ROOT), id == null ? "" : id);
                    }
                }
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Capstone Fantasy camp save v6");
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
                        if (type == GameProgress.class || type == Student.class || type == Hero.class || type == GameCharacter.class
                                || type == Hero.Mage.class || type == Hero.Warrior.class || type == Hero.Hunter.class
                                || type == Hero.Monk.class || type == Inventory.class || type == ArrayList.class || type == boolean[].class
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
            String version = properties.getProperty("version");
            if (!"1".equals(version) && !"2".equals(version) && !"3".equals(version) && !"4".equals(version) && !"5".equals(version) && !"6".equals(version)) { throw new IOException("Unsupported save version"); }
            List<Integer> ids = new ArrayList<>();
            List<Integer> hp = new ArrayList<>();
            List<Integer> xp = new ArrayList<>();
            List<int[]> allocations = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                String prefix = "student." + i + ".";
                int id = Integer.parseInt(properties.getProperty(prefix + "id"));
                Student expected = new Student(id);
                int experience = ("5".equals(version) || "6".equals(version)) ? Integer.parseInt(properties.getProperty(prefix + "experience")) : 0;
                expected.restoreExperience(experience);
                int[] points = new int[Student.Stat.values().length];
                if ("6".equals(version)) {
                    for (Student.Stat stat : Student.Stat.values()) {
                        points[stat.ordinal()] = Integer.parseInt(properties.getProperty(prefix + "allocated." + stat.name()));
                    }
                }
                expected.restoreStatAllocations(points[0], points[1], points[2], points[3], points[4]);
                allocations.add(points);
                if (!expected.getName().equals(properties.getProperty(prefix + "name"))
                        || expected.getMaxHp() != Integer.parseInt(properties.getProperty(prefix + "maxHp"))
                        || expected.getLevel() != Integer.parseInt(properties.getProperty(prefix + "level"))) {
                    throw new IOException("Invalid student information");
                }
                ids.add(id); hp.add(Integer.parseInt(properties.getProperty(prefix + "hp"))); xp.add(experience);
            }
            if (!"true".equals(properties.getProperty("inventory.campusPass", "true"))) {
                throw new IOException("Invalid key item");
            }
            GameProgress restored = GameProgress.restore(ids, hp, xp, allocations, Integer.parseInt(properties.getProperty("inventory.firstAidKits")),
                    Integer.parseInt(properties.getProperty("encounter")));
            if ("3".equals(version) || "4".equals(version) || "5".equals(version) || "6".equals(version)) {
                for (int i = 1; i <= 3; i++) {
                    int id = Integer.parseInt(properties.getProperty("student." + i + ".id"));
                    int sp = Integer.parseInt(properties.getProperty("student." + i + ".sp"));
                    restored.getParty().stream().filter(student -> student.getId() == id)
                            .findFirst().orElseThrow().restoreSp(sp);
                }
            }
            if ("4".equals(version) || "5".equals(version) || "6".equals(version)) {
                restored.restorePetot(Integer.parseInt(properties.getProperty("inventory.petot")));
            }
            if ("1".equals(version)) {
                restored.setVestOwner(Integer.parseInt(properties.getProperty("inventory.campusVestOwner", "-1")));
                restored.grantLegacyRewards();
            } else {
                String bag = properties.getProperty("inventory.equipment");
                if (bag == null) { throw new IOException("Missing equipment"); }
                List<String> equipment = bag.isEmpty() ? List.of() : List.of(bag.split(",", -1));
                List<List<String>> slots = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    List<String> itemSlots = new ArrayList<>();
                    for (Equipment.Slot gearSlot : Equipment.Slot.values()) {
                        String value = properties.getProperty("student." + i + "." + gearSlot.name().toLowerCase(java.util.Locale.ROOT));
                        if (value == null) { throw new IOException("Missing equipment slot"); }
                        itemSlots.add(value.isEmpty() ? null : value);
                    }
                    slots.add(itemSlots);
                }
                boolean[] claimed = new boolean[5];
                for (int stage = 0; stage < claimed.length; stage++) {
                    String value = properties.getProperty("reward." + stage);
                    if (!"true".equals(value) && !"false".equals(value)) { throw new IOException("Invalid reward state"); }
                    claimed[stage] = Boolean.parseBoolean(value);
                }
                restored.restoreEquipment(equipment, slots, claimed);
            }
            return restored;
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            throw new SaveException("This save is damaged or cannot be read.", ex);
        }
    }
    private static GameProgress validate(GameProgress progress) {
        boolean legacy = progress.hasLegacyInventory();
        Inventory inventory = progress.getInventory();
        List<Integer> ids = new ArrayList<>();
        List<Integer> hp = new ArrayList<>();
        List<Integer> xp = new ArrayList<>();
        List<int[]> allocations = new ArrayList<>();
        java.util.Map<Integer, List<String>> slotsByStudent = new java.util.HashMap<>();
        for (Student student : progress.getParty()) {
            Student expected = new Student(student.getId());
            expected.restoreExperience(student.getExperience());
            int[] points = new int[Student.Stat.values().length];
            for (Student.Stat stat : Student.Stat.values()) { points[stat.ordinal()] = student.getAllocatedStatPoints(stat); }
            expected.restoreStatAllocations(points[0], points[1], points[2], points[3], points[4]);
            allocations.add(points);
            boolean oldAttack = (student.getId() == Student.ZACK && student.getBaseAttack() == 18)
                    || (student.getId() == Student.ANGEL && (student.getBaseAttack() == 10 || student.getBaseAttack() == 39));
            boolean oldDefense = student.getId() == Student.BOGART && student.getBaseDefense() == 14;
            boolean oldSpeed = (student.getId() == Student.ZACK && student.getSpeed() == 10)
                    || (student.getId() == Student.BOGART && (student.getSpeed() == 7 || student.getSpeed() == 11))
                    || (student.getId() == Student.CHARICE && student.getSpeed() == 16)
                    || (student.getId() == Student.ANGEL && (student.getSpeed() == 7 || student.getSpeed() == 9));
            if (!expected.getName().equals(student.getName()) || expected.getMaxHp() != student.getMaxHp()
                    || (expected.getBaseAttack() != student.getBaseAttack() && !oldAttack)
                    || (expected.getBaseDefense() != student.getBaseDefense() && !oldDefense)
                    || (expected.getSpeed() != student.getSpeed() && !oldSpeed) || expected.getLevel() != student.getLevel()
                    || student.getSp() < 0 || student.getSp() > student.getMaxSp()) {
                throw new IllegalArgumentException("Invalid student information");
            }
            ids.add(student.getId()); hp.add(student.getHp()); xp.add(student.getExperience());
            List<String> items = new ArrayList<>();
            for (Equipment.Slot slot : Equipment.Slot.values()) { items.add(student.getEquipmentId(slot)); }
            slotsByStudent.put(student.getId(), items);
        }
        GameProgress restored = GameProgress.restore(ids, hp, xp, allocations, progress.getKits(), progress.getEncounter());
        restored.restorePetot(progress.getPetot());
        List<List<String>> slots = new ArrayList<>();
        for (Student student : restored.getParty()) {
            slots.add(slotsByStudent.get(student.getId()));
            int id = student.getId();
            student.restoreSp(progress.getParty().stream().filter(old -> old.getId() == id).findFirst().orElseThrow().getSp());
        }
        boolean[] claimed = new boolean[5];
        for (int stage = 0; stage < claimed.length; stage++) { claimed[stage] = inventory.isRewardClaimed(stage); }
        restored.restoreEquipment(inventory.getEquipmentIds(), slots, claimed);
        if (legacy) { restored.grantLegacyRewards(); }
        return restored;
    }
}
