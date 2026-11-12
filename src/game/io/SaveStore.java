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
                properties.setProperty("version", "9");
                properties.setProperty("mechanics.looperManaLeak", "true");
                properties.setProperty("party.count", Integer.toString(checked.getRoster().size()));
                properties.setProperty("party.active", checked.getParty().stream().map(s -> Integer.toString(s.getId())).collect(java.util.stream.Collectors.joining(",")));
                properties.setProperty("encounter", Integer.toString(checked.getEncounter()));
                properties.setProperty("market.merchantDefeated", Boolean.toString(checked.isMerchantDefeated()));
                properties.setProperty("inventory.petot", Integer.toString(checked.getPetot()));
                properties.setProperty("inventory.firstAidKits", Integer.toString(checked.getKits()));
                properties.setProperty("inventory.whiteFlowers", Integer.toString(checked.getInventory().getWhiteFlowers()));
                properties.setProperty("inventory.campusPass", "true");
                properties.setProperty("inventory.equipment", String.join(",", checked.getInventory().getEquipmentIds()));
                for (int stage = 0; stage < 5; stage++) {
                    properties.setProperty("reward." + stage, Boolean.toString(checked.getInventory().isRewardClaimed(stage)));
                }
                for (int i = 0; i < checked.getRoster().size(); i++) {
                    Student student = checked.getRoster().get(i);
                    String prefix = "student." + (i + 1) + ".";
                    properties.setProperty(prefix + "id", Integer.toString(student.getId()));
                    properties.setProperty(prefix + "name", student.getName());
                    properties.setProperty(prefix + "hp", Integer.toString(student.getHp()));
                    properties.setProperty(prefix + "sp", Integer.toString(student.getSp()));
                    properties.setProperty(prefix + "maxHp", Integer.toString(student.getMaxHp()));
                    properties.setProperty(prefix + "level", Integer.toString(student.getLevel()));
                    properties.setProperty(prefix + "experience", Long.toString(student.getExperience()));
                    properties.setProperty(prefix + "growth", Integer.toString(student.getExperienceGrowth()));
                    properties.setProperty(prefix + "levelCap", Integer.toString(student.getLevelCap()));
                    for (Student.Stat stat : Student.Stat.values()) {
                        properties.setProperty(prefix + "allocated." + stat.name(), Integer.toString(student.getAllocatedStatPoints(stat)));
                    }
                    for (Equipment.Slot gearSlot : Equipment.Slot.values()) {
                        String id = student.getEquipmentId(gearSlot);
                        properties.setProperty(prefix + gearSlot.name().toLowerCase(java.util.Locale.ROOT), id == null ? "" : id);
                    }
                }
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Capstone Fantasy camp save v9");
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
            String looperMechanics = properties.getProperty("mechanics.looperManaLeak", "false");
            if (!looperMechanics.equals("true") && !looperMechanics.equals("false")) throw new IOException("Invalid Looper Ring mechanics");
            boolean legacyLooper = !Boolean.parseBoolean(looperMechanics);
            if (!"1".equals(version) && !"2".equals(version) && !"3".equals(version) && !"4".equals(version) && !"5".equals(version) && !"6".equals(version) && !modern(version)) { throw new IOException("Unsupported save version"); }
            int count = modern(version) ? Integer.parseInt(properties.getProperty("party.count")) : 3;
            if (count < 1 || count > 4) throw new IOException("Invalid party size");
            List<Integer> ids = new ArrayList<>();
            List<Integer> hp = new ArrayList<>();
            List<Long> xp = new ArrayList<>();
            List<int[]> allocations = new ArrayList<>();
            List<int[]> progression = new ArrayList<>();
            List<String> armorIds = new ArrayList<>();
            List<String> accessoryIds = new ArrayList<>();
            List<Integer> oldMaxSps = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                String prefix = "student." + i + ".";
                int id = Integer.parseInt(properties.getProperty(prefix + "id"));
                Student expected = new Student(id);
                int[] rules = modern(version) ? new int[]{Integer.parseInt(properties.getProperty(prefix + "growth")), Integer.parseInt(properties.getProperty(prefix + "levelCap"))} : new int[]{0, 30};
                expected.restoreProgression(rules[0], rules[1]); progression.add(rules);
                long experience = ("5".equals(version) || "6".equals(version) || modern(version)) ? Long.parseLong(properties.getProperty(prefix + "experience")) : 0;
                expected.restoreExperience(experience);
                int[] points = new int[Student.Stat.values().length];
                if ("6".equals(version) || modern(version)) {
                    for (Student.Stat stat : Student.Stat.values()) {
                        points[stat.ordinal()] = Integer.parseInt(properties.getProperty(prefix + "allocated." + stat.name()));
                    }
                }
                expected.restoreStatAllocations(points[0], points[1], points[2], points[3], points[4]);
                allocations.add(points);
                String armor = properties.getProperty(prefix + "armor", "");
                armorIds.add(armor.isEmpty() ? null : armor);
                String accessory = properties.getProperty(prefix + "accessory", "");
                accessoryIds.add(accessory.isEmpty() ? null : accessory);
                int newMax = expected.maxHpWithEquipment(armor.isEmpty() ? null : armor, accessory.isEmpty() ? null : accessory);
                int savedMax = Integer.parseInt(properties.getProperty(prefix + "maxHp"));
                int oldMax = legacyMaxHp(expected, armor);
                boolean oldStats = !currentStats(version) && savedMax == oldMax;
                int savedHp = Integer.parseInt(properties.getProperty(prefix + "hp"));
                if (!expected.getName().equals(properties.getProperty(prefix + "name"))
                        || (savedMax != newMax && !("sentinel_band".equals(accessory) && savedMax == newMax - 50)
                            && !("campus_vest".equals(armor) && savedMax == expected.statWithEquipment(Student.Stat.HP, Equipment.Slot.ARMOR, null)
                                + ("sentinel_band".equals(accessory) ? 50 : 0)) && !oldStats) || savedHp < 0 || savedHp > savedMax
                        || expected.getLevel() != Integer.parseInt(properties.getProperty(prefix + "level"))) {
                    throw new IOException("Invalid student information");
                }
                ids.add(id); hp.add(oldStats ? scaleResource(savedHp, savedMax, newMax) : savedHp); xp.add(experience);
                int oldMaxSp = !currentStats(version) ? legacyMaxSp(expected) : expected.maxSpWithEquipment(Equipment.Slot.ACCESSORY, accessory.isEmpty() ? null : accessory);
                oldMaxSps.add(legacyLooper && "looper_ring".equals(accessory) ? oldMaxSp * 2 : oldMaxSp);
            }
            if (!"true".equals(properties.getProperty("inventory.campusPass", "true"))) {
                throw new IOException("Invalid key item");
            }
            GameProgress restored = GameProgress.restore(ids, hp, xp, allocations, progression, armorIds, accessoryIds, Integer.parseInt(properties.getProperty("inventory.firstAidKits")),
                    Integer.parseInt(properties.getProperty("encounter")));
            String marketState = properties.getProperty("market.merchantDefeated", "false");
            if (!marketState.equals("true") && !marketState.equals("false")) throw new IOException("Invalid market state");
            if (Boolean.parseBoolean(marketState)) restored.unlockCarbonMarket();
            restored.getInventory().restoreWhiteFlowers("9".equals(version) ? Integer.parseInt(properties.getProperty("inventory.whiteFlowers")) : 0);
            if ("3".equals(version) || "4".equals(version) || "5".equals(version) || "6".equals(version) || modern(version)) {
                for (int i = 1; i <= count; i++) {
                    int id = Integer.parseInt(properties.getProperty("student." + i + ".id"));
                    int sp = Integer.parseInt(properties.getProperty("student." + i + ".sp"));
                    Student student = restored.getParty().stream().filter(member -> member.getId() == id).findFirst().orElseThrow();
                    int oldMax = oldMaxSps.get(i - 1);
                    if (sp < 0 || sp > oldMax) throw new IOException("Invalid SP");
                    boolean oldLooper = legacyLooper && "looper_ring".equals(accessoryIds.get(i - 1));
                    student.restoreSp(currentStats(version) && !oldLooper ? sp : scaleResource(sp, oldMax, student.getMaxSp()));
                }
            }
            if ("4".equals(version) || "5".equals(version) || "6".equals(version) || modern(version)) {
                restored.restorePetot(Integer.parseInt(properties.getProperty("inventory.petot")));
            }
            if ("1".equals(version)) {
                restored.setVestOwner(Integer.parseInt(properties.getProperty("inventory.campusVestOwner", "-1")));
                restored.grantLegacyRewards();
            } else {
                String bag = properties.getProperty("inventory.equipment");
                if (bag == null) { throw new IOException("Missing equipment"); }
                List<String> equipment = bag.isEmpty() ? List.of() : List.of(bag.split(",", -1));
                java.util.Map<Integer, List<String>> slotsById = new java.util.HashMap<>();
                for (int i = 1; i <= count; i++) {
                    List<String> itemSlots = new ArrayList<>();
                    for (Equipment.Slot gearSlot : Equipment.Slot.values()) {
                        String value = properties.getProperty("student." + i + "." + gearSlot.name().toLowerCase(java.util.Locale.ROOT));
                        if (value == null) { throw new IOException("Missing equipment slot"); }
                        itemSlots.add(value.isEmpty() ? null : value);
                    }
                    slotsById.put(ids.get(i - 1), itemSlots);
                }
                boolean[] claimed = new boolean[5];
                for (int stage = 0; stage < claimed.length; stage++) {
                    String value = properties.getProperty("reward." + stage);
                    if (!"true".equals(value) && !"false".equals(value)) { throw new IOException("Invalid reward state"); }
                    claimed[stage] = Boolean.parseBoolean(value);
                }
                List<List<String>> slots = new ArrayList<>();
                for (Student student : restored.getParty()) slots.add(slotsById.get(student.getId()));
                restored.restoreEquipment(equipment, slots, claimed);
            }
            if (modern(version)) {
                String active = properties.getProperty("party.active");
                if (active == null || active.isBlank()) throw new IOException("Missing active party");
                List<Integer> activeIds = java.util.Arrays.stream(active.split(",", -1)).map(Integer::parseInt).toList();
                if (!ids.containsAll(activeIds)) throw new IOException("Unknown active student");
                restored.setActiveParty(activeIds);
            }
            return restored;
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            throw new SaveException("This save is damaged or cannot be read.", ex);
        }
    }
    private static boolean currentStats(String version) { return "8".equals(version) || "9".equals(version); }
    private static boolean modern(String version) { return "7".equals(version) || "8".equals(version) || "9".equals(version); }
    private static int scaleResource(int value, int oldMax, int newMax) {
        return value == 0 ? 0 : Math.max(1, Math.min(newMax, (int) Math.round((double) value * newMax / oldMax)));
    }
    private static int legacyMaxSp(Student student) {
        int[] bases = {150, 50, 45, 30};
        return bases[student.getId()] + (student.getLevel() - 1) * 3 + student.getAllocatedStatPoints(Student.Stat.SP) * 3;
    }
    private static int legacyMaxHp(Student student, String armor) {
        int[] oldBases = {80, 120, 90, 105}, newBases = {60, 150, 90, 125};
        int current = student.statWithEquipment(Student.Stat.HP, Equipment.Slot.ARMOR, armor == null || armor.isEmpty() ? null : armor);
        return current + (oldBases[student.getId()] - newBases[student.getId()]) * ("blade_mail".equals(armor) ? 3 : 1);
    }
    private static GameProgress validate(GameProgress progress) {
        boolean legacy = progress.hasLegacyInventory();
        Inventory inventory = progress.getInventory();
        List<Integer> ids = new ArrayList<>();
        List<Integer> hp = new ArrayList<>();
        List<Long> xp = new ArrayList<>();
        List<int[]> allocations = new ArrayList<>();
        List<int[]> progression = new ArrayList<>();
        List<String> armorIds = new ArrayList<>();
        List<String> accessoryIds = new ArrayList<>();
        java.util.Map<Integer, List<String>> slotsByStudent = new java.util.HashMap<>();
        for (Student student : progress.getRoster()) {
            Student expected = new Student(student.getId());
            expected.restoreProgression(student.getExperienceGrowth(), student.getLevelCap());
            progression.add(new int[]{student.getExperienceGrowth(), student.getLevelCap()});
            expected.restoreExperience(student.getExperience());
            int[] points = new int[Student.Stat.values().length];
            for (Student.Stat stat : Student.Stat.values()) { points[stat.ordinal()] = student.getAllocatedStatPoints(stat); }
            expected.restoreStatAllocations(points[0], points[1], points[2], points[3], points[4]);
            allocations.add(points);
            boolean oldAttack = (student.getId() == Student.ZACK && student.getBaseAttack() == 18)
                    || (student.getId() == Student.ANGEL && (student.getBaseAttack() == 10 || student.getBaseAttack() == 39));
            boolean oldDefense = student.getId() == Student.BOGART && student.getBaseDefense() == 14;
            int originalSpeed = student.getUnmodifiedSpeed() - student.getAllocatedStatPoints(Student.Stat.SPD)
                    - (student.getLevel() - 1) / 5;
            boolean oldSpeed = (student.getId() == Student.ZACK && originalSpeed == 10)
                    || (student.getId() == Student.BOGART && (originalSpeed == 7 || originalSpeed == 11))
                    || (student.getId() == Student.CHARICE && originalSpeed == 16)
                    || (student.getId() == Student.ANGEL && (originalSpeed == 7 || originalSpeed == 9));
            boolean oldLooper = !student.hasLooperManaLeakVersion() && "looper_ring".equals(student.getEquipmentId(Equipment.Slot.ACCESSORY));
            int expectedHp = expected.maxHpWithEquipment(student.getEquipmentId(Equipment.Slot.ARMOR), student.getEquipmentId(Equipment.Slot.ACCESSORY));
            boolean legacyStats = student.getMaxHp() == legacyMaxHp(expected, student.getEquipmentId(Equipment.Slot.ARMOR))
                    + ("sentinel_band".equals(student.getEquipmentId(Equipment.Slot.ACCESSORY)) ? 50 : 0)
                    && (student.getMaxHp() != expectedHp || student.getBaseAttack() != expected.getBaseAttack()
                        || student.getBaseDefense() != expected.getBaseDefense() || student.getUnmodifiedSpeed() != expected.getUnmodifiedSpeed());
            if (!expected.getName().equals(student.getName()) || (expectedHp != student.getMaxHp() && !legacyStats)
                    || (expected.getBaseAttack() != student.getBaseAttack() && !oldAttack && !legacyStats)
                    || (expected.getBaseDefense() != student.getBaseDefense() && !oldDefense && !legacyStats)
                    || (expected.getUnmodifiedSpeed() != student.getUnmodifiedSpeed() && !oldSpeed && !legacyStats) || expected.getLevel() != student.getLevel()
                    || student.getHp() < 0 || student.getHp() > student.getMaxHp()
                    || student.getSp() < 0 || student.getSp() > Math.max(oldLooper ? student.getMaxSp() * 2 : student.getMaxSp(), legacyMaxSp(student))) {
                throw new IllegalArgumentException("Invalid student information");
            }
            ids.add(student.getId()); hp.add(legacyStats ? scaleResource(student.getHp(), student.getMaxHp(), expectedHp) : student.getHp()); xp.add(student.getExperience());
            armorIds.add(student.getEquipmentId(Equipment.Slot.ARMOR));
            accessoryIds.add(student.getEquipmentId(Equipment.Slot.ACCESSORY));
            List<String> items = new ArrayList<>();
            for (Equipment.Slot slot : Equipment.Slot.values()) { items.add(student.getEquipmentId(slot)); }
            slotsByStudent.put(student.getId(), items);
        }
        GameProgress restored = GameProgress.restore(ids, hp, xp, allocations, progression, armorIds, accessoryIds, progress.getKits(), progress.getEncounter());
        restored.restorePetot(progress.getPetot());
        if (progress.isMerchantDefeated()) restored.unlockCarbonMarket();
        restored.getInventory().restoreWhiteFlowers(inventory.getWhiteFlowers());
        List<List<String>> slots = new ArrayList<>();
        for (Student student : restored.getParty()) {
            slots.add(slotsByStudent.get(student.getId()));
            int id = student.getId();
            Student old = progress.getRoster().stream().filter(member -> member.getId() == id).findFirst().orElseThrow();
            boolean oldStats = old.getMaxHp() != student.getMaxHp() || old.getBaseAttack() != student.getBaseAttack()
                    || old.getBaseDefense() != student.getBaseDefense() || old.getUnmodifiedSpeed() != student.getUnmodifiedSpeed();
            boolean oldLooper = !old.hasLooperManaLeakVersion() && "looper_ring".equals(old.getEquipmentId(Equipment.Slot.ACCESSORY));
            student.restoreSp(!old.hasInitializedSkillPoints() ? student.getMaxSp()
                    : oldLooper ? scaleResource(old.getSp(), (oldStats ? legacyMaxSp(old) : student.getMaxSp()) * 2, student.getMaxSp())
                    : oldStats ? scaleResource(old.getSp(), legacyMaxSp(old), student.getMaxSp()) : old.getSp());
        }
        boolean[] claimed = new boolean[5];
        for (int stage = 0; stage < claimed.length; stage++) { claimed[stage] = inventory.isRewardClaimed(stage); }
        restored.restoreEquipment(inventory.getEquipmentIds(), slots, claimed);
        if (legacy) { restored.grantLegacyRewards(); }
        restored.setActiveParty(progress.getParty().stream().map(Student::getId).toList());
        return restored;
    }
}
