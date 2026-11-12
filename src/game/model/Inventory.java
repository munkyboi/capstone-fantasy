package game.model;

import java.util.ArrayList;
import java.util.List;

/** Shared consumables, key items and unequipped equipment. */
public final class Inventory implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /** Maximum number of first-aid kits in a save. */
    public static final int MAX_KITS = 9;
    public static final int MAX_WHITE_FLOWERS = 9;
    private int whiteFlowers;
    private int kits;
    private int vestOwner = -1;
    private ArrayList<String> equipment;
    private boolean[] rewardsClaimed;
    /** Creates an inventory with the supplied number of kits.
     * @param kits number of first-aid kits
     */
    public Inventory(int kits) {
        if (kits < 0 || kits > MAX_KITS) { throw new IllegalArgumentException("Invalid kit count"); }
        this.kits = kits;
        equipment = new ArrayList<>(); equipment.add("campus_vest");
        rewardsClaimed = new boolean[5];
    }
    /** Checks if this inventory came from a save before equipment slots existed.
     * @return true for an older inventory
     */
    public boolean isLegacy() { return equipment == null || rewardsClaimed == null; }
    /** Initializes an older inventory without losing its vest ownership. */
    public void upgradeLegacy() {
        if (equipment == null) {
            equipment = new ArrayList<>();
            if (vestOwner == -1) { equipment.add("campus_vest"); }
        }
        if (rewardsClaimed == null) { rewardsClaimed = new boolean[5]; }
    }
    /** Returns item categories and the unequipped gear.
     * @return shared item list
     */
    public List<Item> getItems() {
        upgradeLegacy();
        List<Item> items = new ArrayList<>(); items.add(new FirstAidKit()); items.add(new WhiteFlower()); items.add(new KeyItem());
        for (String id : equipment) { items.add(Equipment.fromId(id)); }
        return List.copyOf(items);
    }
    /** Returns unequipped equipment IDs.
     * @return immutable IDs
     */
    public List<String> getEquipmentIds() { upgradeLegacy(); return List.copyOf(equipment); }
    /** Returns remaining kits.
     * @return kit count
     */
    public int getKits() { return kits; }
    public int getWhiteFlowers() { return whiteFlowers; }
    public boolean addWhiteFlower() { if (whiteFlowers >= MAX_WHITE_FLOWERS) return false; whiteFlowers++; return true; }
    public void restoreWhiteFlowers(int count) {
        if (count < 0 || count > MAX_WHITE_FLOWERS) throw new IllegalArgumentException("Invalid White Flower count");
        whiteFlowers = count;
    }
    public int useWhiteFlower(Student student) {
        if (whiteFlowers == 0 || student == null || student.isAlive()) return 0;
        int restored = student.heal(Math.max(1, (int) ((3L * student.getMaxHp() + 9) / 10)));
        if (restored > 0) whiteFlowers--;
        return restored;
    }
    /** Returns the vest owner's student ID, or -1 if unequipped.
     * @return owner ID
     */
    public int getVestOwner() { return vestOwner; }
    /** Uses one kit if healing is possible.
     * @param student target
     * @return HP restored
     */
    public int useKit(Student student) {
        if (kits == 0 || student == null) { return 0; }
        int healed = new FirstAidKit().use(student);
        if (healed > 0) { kits--; }
        return healed;
    }
    /** Adds a kit if there is space in the shared bag. */
    public boolean addKit() {
        if (kits >= MAX_KITS) { return false; }
        kits++; return true;
    }
    /** Sets the vest owner for backward-compatible saves.
     * @param id student ID, or -1
     */
    public void setVestOwner(int id) {
        if (id < -1 || id > Student.ANGEL) { throw new IllegalArgumentException("Invalid vest owner"); }
        vestOwner = id;
    }
    /** Returns whether a stage reward has already been collected.
     * @param stage zero-based stage
     * @return true if collected
     */
    public boolean isRewardClaimed(int stage) { upgradeLegacy(); return rewardsClaimed[stage]; }
    /** Marks a stage reward as collected.
     * @param stage zero-based stage
     */
    public void markRewardClaimed(int stage) { upgradeLegacy(); rewardsClaimed[stage] = true; }
    /** Adds an unequipped item.
     * @param id equipment ID
     */
    public void addEquipment(String id) { upgradeLegacy(); Equipment.fromId(id); equipment.add(id); }
    /** Removes one item from the bag.
     * @param id equipment ID
     * @return true if removed
     */
    public boolean removeEquipment(String id) { upgradeLegacy(); return equipment.remove(id); }
    /** Replaces bag contents during validated save restoration.
     * @param ids equipment IDs
     */
    public void restoreEquipment(List<String> ids) {
        ArrayList<String> copy = new ArrayList<>();
        for (String id : ids) { Equipment.fromId(id); copy.add(id); }
        equipment = copy;
    }
}
