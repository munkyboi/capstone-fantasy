package game.model;

import java.util.ArrayList;
import java.util.List;

/** Shared consumables, key items and unequipped equipment. */
public final class Inventory implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /** Maximum number of first-aid kits in a save. */
    public static final int MAX_KITS = 9;
    /** Maximum White Flowers held at one time. */
    public static final int MAX_WHITE_FLOWERS = 9;
    /** Serialized state: white flowers.
     * @serial
     */
    private int whiteFlowers;
    /** Serialized state: kits.
     * @serial
     */
    private int kits;
    /** Serialized state: vest owner.
     * @serial
     */
    private int vestOwner = -1;
    // Legacy save representation; the live bag contains typed items.
    /** Serialized state: equipment.
     * @serial
     */
    private ArrayList<String> equipment;
    private transient ItemBag<Item> items;
    /** Serialized state: rewards claimed.
     * @serial
     */
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
        if (items == null) {
            ItemBag<Item> restored = new ItemBag<>();
            restored.add(new KeyItem());
            for (int i = 0; i < kits; i++) restored.add(new FirstAidKit());
            for (int i = 0; i < whiteFlowers; i++) restored.add(new WhiteFlower());
            for (String id : equipment) restored.add(Equipment.fromId(id));
            items = restored;
        }
    }
    /** Returns item categories and the unequipped gear.
     * @return shared item list
     */
    public List<Item> getItems() {
        upgradeLegacy();
        return items.items();
    }
    /** Returns unequipped equipment IDs.
     * @return immutable IDs
     */
    public List<String> getEquipmentIds() { upgradeLegacy(); return items.items().stream().filter(Equipment.class::isInstance).map(Equipment.class::cast).map(Equipment::getId).toList(); }
    /** Returns remaining kits.
     * @return kit count
     */
    public int getKits() { return kits; }
    /** Returns the number of White Flowers in the shared bag.
     * @return the number of White Flowers in the shared bag
     */
    public int getWhiteFlowers() { return whiteFlowers; }
    /** Adds a White Flower when the bag has room.
     * @return the result of add white flower
     */
    public boolean addWhiteFlower() { upgradeLegacy(); if (whiteFlowers >= MAX_WHITE_FLOWERS) return false; items.add(new WhiteFlower()); whiteFlowers++; return true; }
    /** Restores the White Flower count from a save.
     * @param count number to use
     */
    public void restoreWhiteFlowers(int count) {
        if (count < 0 || count > MAX_WHITE_FLOWERS) throw new IllegalArgumentException("Invalid White Flower count");
        upgradeLegacy();
        while (items.removeFirst(WhiteFlower.class::isInstance)) { }
        for (int i = 0; i < count; i++) items.add(new WhiteFlower());
        whiteFlowers = count;
    }
    /** Revives a fallen student and consumes a White Flower on success.
     * @param student student involved in the action
     * @return the result of use white flower
     */
    public int useWhiteFlower(Student student) {
        upgradeLegacy();
        if (whiteFlowers == 0 || student == null || student.isAlive()) return 0;
        int restored = student.heal(Math.max(1, (int) ((3L * student.getMaxHp() + 9) / 10)));
        if (restored > 0) { items.removeFirst(WhiteFlower.class::isInstance); whiteFlowers--; }
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
        upgradeLegacy();
        if (kits == 0 || student == null) { return 0; }
        FirstAidKit kit = items.items().stream().filter(FirstAidKit.class::isInstance).map(FirstAidKit.class::cast).findFirst().orElseThrow();
        int healed = kit.use(student);
        if (healed > 0) { items.removeFirst(item -> item == kit); kits--; }
        return healed;
    }
    /** Adds a kit if there is space in the shared bag.
     * @return the result of add kit
     */
    public boolean addKit() {
        upgradeLegacy();
        if (kits >= MAX_KITS) { return false; }
        items.add(new FirstAidKit()); kits++; return true;
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
    public void addEquipment(String id) { upgradeLegacy(); Equipment item = Equipment.fromId(id); items.add(item); equipment.add(item.getId()); }
    /** Removes one item from the bag.
     * @param id equipment ID
     * @return true if removed
     */
    public boolean removeEquipment(String id) { upgradeLegacy(); boolean removed = items.removeFirst(item -> item instanceof Equipment gear && gear.getId().equals(id)); if (removed) equipment.remove(id); return removed; }
    /** Replaces bag contents during validated save restoration.
     * @param ids equipment IDs
     */
    public void restoreEquipment(List<String> ids) {
        ArrayList<String> copy = new ArrayList<>();
        for (String id : ids) { Equipment.fromId(id); copy.add(id); }
        upgradeLegacy();
        while (items.removeFirst(Equipment.class::isInstance)) { }
        for (String id : copy) items.add(Equipment.fromId(id));
        equipment = copy;
    }
}
