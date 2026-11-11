package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Party and shared supplies carried between encounters. */
public final class GameProgress implements java.io.Serializable {
    /** Zero-based route position reserved for the final boss. */
    public static final int FINAL_BOSS_ENCOUNTER = 4;
    private static final long serialVersionUID = 1L;
    private static final int STARTING_KITS = 3;

    private final List<Student> party = new ArrayList<>();
    private int kits = STARTING_KITS;
    private Inventory inventory = new Inventory(STARTING_KITS);
    private int encounter;
    private int petot;
    /** Maximum supported balance. */
    public static final int MAX_PETOT = 999999;

    /** Creates a new party of three distinct students.
     * @param ids selected character indices
     */
    public GameProgress(List<Integer> ids) {
        if (ids.size() != 3 || ids.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Choose three different students");
        }
        for (int id : ids) {
            Student student = Student.create(id);
            student.setEquipmentId(Equipment.Slot.WEAPON, Equipment.starterWeapon(id));
            party.add(student);
        }
        party.sort(Comparator.comparingInt(Student::getSpeed).reversed());
    }
    /** Returns the current party in speed order.
     * @return the current party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns first-aid kits remaining.
     * @return first-aid kits remaining
     */ public int getKits() { return getInventory().getKits(); }
    /** Returns the shared Petot balance. */
    public int getPetot() { return petot; }
    /** Adds earned Petot up to the balance limit and returns the amount credited. */
    public int addPetot(int amount) {
        if (amount < 0) { throw new IllegalArgumentException("Invalid Petot amount"); }
        int credited = (int) Math.min((long) amount, (long) MAX_PETOT - petot);
        petot += credited; return credited;
    }
    /** Restores a validated Petot balance. */
    public void restorePetot(int amount) {
        if (amount < 0 || amount > MAX_PETOT) { throw new IllegalArgumentException("Invalid Petot balance"); }
        petot = amount;
    }
    /** Spends Petot if sufficient funds are available. */
    public boolean spendPetot(int amount) {
        if (amount < 0) { throw new IllegalArgumentException("Invalid Petot amount"); }
        if (amount > petot) { return false; }
        petot -= amount; return true;
    }
    /** Checks whether a deserialized save predates equipment slots.
     * @return true for an older save
     */
    public boolean hasLegacyInventory() { return inventory == null || inventory.isLegacy(); }
    /** Returns the party's shared inventory, upgrading older saves when needed.
     * @return shared supplies
     */
    public Inventory getInventory() {
        boolean legacy = hasLegacyInventory();
        if (inventory == null) { inventory = new Inventory(kits); }
        if (legacy) {
            int vestOwner = inventory.getVestOwner();
            inventory.upgradeLegacy();
            for (Student student : party) {
                if (student.getEquipmentId(Equipment.Slot.WEAPON) == null) {
                    student.setEquipmentId(Equipment.Slot.WEAPON, Equipment.starterWeapon(student.getId()));
                }
                if (student.getId() == vestOwner) { student.setVestEquipped(true); }
            }
        }
        return inventory;
    }
    /** Equips a compatible item from the shared bag, or unequips the slot.
     * @param student party member
     * @param slot selected slot
     * @param itemId item in the bag, or null to unequip
     * @return true if the change succeeded
     */
    public boolean equip(Student student, Equipment.Slot slot, String itemId) {
        if (!party.contains(student) || slot == null) { return false; }
        Inventory bag = getInventory();
        String previous = student.getEquipmentId(slot);
        if (java.util.Objects.equals(previous, itemId)) { return true; }
        if (itemId != null) {
            Equipment item;
            try { item = Equipment.fromId(itemId); } catch (IllegalArgumentException error) { return false; }
            if (item.getSlot() != slot || !item.canEquip(student.getId()) || !bag.getEquipmentIds().contains(itemId)) { return false; }
        }
        if (itemId != null && !bag.removeEquipment(itemId)) { return false; }
        if (previous != null) { bag.addEquipment(previous); }
        student.setEquipmentId(slot, itemId);
        if ("campus_vest".equals(previous)) { bag.setVestOwner(-1); }
        if ("campus_vest".equals(itemId)) { bag.setVestOwner(student.getId()); }
        return true;
    }
    /** Equips or removes the original campus vest as a shortcut.
     * @param student selected party member
     * @return true if the vest can be changed
     */
    public boolean toggleVest(Student student) {
        if (!party.contains(student)) { return false; }
        if (student.hasVest()) { return equip(student, Equipment.Slot.ARMOR, null); }
        return equip(student, Equipment.Slot.ARMOR, "campus_vest");
    }
    /** Restores legacy vest ownership.
     * @param owner student ID, or -1 for unequipped
     */
    public void setVestOwner(int owner) {
        if (owner == -1) { return; }
        Student student = party.stream().filter(member -> member.getId() == owner).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vest owner is not in the party"));
        if (!equip(student, Equipment.Slot.ARMOR, "campus_vest")) { throw new IllegalArgumentException("Invalid vest ownership"); }
    }
    /** Claims the current stage's reward once.
     * @return reward name, or null if already claimed
     */
    public String claimStageReward() { return claimReward(encounter); }
    /** Grants the stage reward once and returns the actual equipment IDs added to the bag.
     * @return newly awarded equipment IDs, or an empty list when already claimed
     */
    public List<String> claimStageRewardItems() {
        return claimStageRewardItems(encounter);
    }
    /** Awards the specific unlocked stage's one-time rewards. */
    public List<String> claimStageRewardItems(int stage) {
        if (stage < 0 || stage > encounter) { throw new IllegalArgumentException("Stage is locked"); }
        List<String> previous = new ArrayList<>(getInventory().getEquipmentIds());
        if (claimReward(stage) == null) { return List.of(); }
        List<String> awarded = new ArrayList<>();
        for (String id : getInventory().getEquipmentIds()) {
            if (!previous.remove(id)) { awarded.add(id); }
        }
        return List.copyOf(awarded);
    }
    private String claimReward(int stage) {
        String[] rewards = {"focus_charm", "training_uniform", "guardian_band", "courage_charm", "campus_medal"};
        if (stage < 0 || stage >= rewards.length || getInventory().isRewardClaimed(stage)) { return null; }
        String id = rewards[stage];
        getInventory().addEquipment(id);
        if (stage == 0) {
            for (Student student : party) { getInventory().addEquipment(Equipment.upgradedWeapon(student.getId())); }
        }
        getInventory().markRewardClaimed(stage);
        return stage == 0 ? "Focus Charm and party weapon upgrades" : Equipment.fromId(id).getName();
    }
    /** Awards already completed stages when upgrading an older save. */
    public void grantLegacyRewards() {
        for (int stage = 0; stage < encounter; stage++) { claimReward(stage); }
    }
    /** Restores equipment from validated save data.
     * @param bag unequipped equipment IDs
     * @param itemIds per-party weapon, armor and accessory IDs
     * @param claimed stage reward flags
     */
    public void restoreEquipment(List<String> bag, List<List<String>> itemIds, boolean[] claimed) {
        if (bag == null || itemIds == null || itemIds.size() != party.size() || claimed == null || claimed.length != 5) {
            throw new IllegalArgumentException("Invalid inventory data");
        }
        java.util.Set<String> unique = new java.util.HashSet<>();
        for (String id : bag) { Equipment.fromId(id); if (!unique.add(id)) { throw new IllegalArgumentException("Duplicate item"); } }
        for (int i = 0; i < party.size(); i++) {
            Student student = party.get(i);
            List<String> items = itemIds.get(i);
            if (items.size() != Equipment.Slot.values().length) { throw new IllegalArgumentException("Invalid equipment slots"); }
            for (Equipment.Slot slot : Equipment.Slot.values()) {
                String id = items.get(slot.ordinal());
                if (id != null) {
                    Equipment item = Equipment.fromId(id);
                    if (item.getSlot() != slot || !item.canEquip(student.getId()) || !unique.add(id)) {
                        throw new IllegalArgumentException("Invalid or duplicate equipment");
                    }
                }
            }
        }
        Inventory bagState = getInventory(); bagState.restoreEquipment(bag); bagState.setVestOwner(-1);
        for (int i = 0; i < party.size(); i++) {
            Student student = party.get(i);
            for (Equipment.Slot slot : Equipment.Slot.values()) {
                String id = itemIds.get(i).get(slot.ordinal());
                student.setEquipmentId(slot, id);
                if ("campus_vest".equals(id)) { bagState.setVestOwner(student.getId()); }
            }
        }
        for (int i = 0; i < claimed.length; i++) { if (claimed[i]) { bagState.markRewardClaimed(i); } }
    }
    /** Returns zero-based current encounter.
     * @return zero-based current encounter
     */ public int getEncounter() { return encounter; }
    /** Unlocks the next route location after an encounter.
     * @return whether the route position advanced
     */
    public boolean advance() { if (encounter >= 5) { return false; } encounter++; return true; }
    /** Reconstructs and validates a saved camp state.
     * @param ids student indices
     * @param hitPoints corresponding current HP values
     * @param kits remaining first-aid kits
     * @param encounter next encounter index
     * @return reconstructed progress
     */
    public static GameProgress restore(List<Integer> ids, List<Integer> hitPoints, int kits, int encounter) {
        return restore(ids, hitPoints, java.util.List.of(0, 0, 0), kits, encounter);
    }
    /** Reconstructs a saved party including experience. */
    public static GameProgress restore(List<Integer> ids, List<Integer> hitPoints, List<Integer> experience, int kits, int encounter) {
        if (ids == null || hitPoints == null || experience == null || ids.size() != 3 || hitPoints.size() != 3 || experience.size() != 3
                || kits < 0 || kits > Inventory.MAX_KITS || encounter < 1 || encounter > 5) {
            throw new IllegalArgumentException("Invalid saved progress");
        }
        GameProgress result = new GameProgress(ids);
        for (int i = 0; i < ids.size(); i++) {
            final int id = ids.get(i);
            Student student = result.party.stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
            student.restoreExperience(experience.get(i));
            student.restoreHp(hitPoints.get(i));
        }
        if (result.party.stream().noneMatch(Student::isAlive)) { throw new IllegalArgumentException("Party is defeated"); }
        result.kits = kits;
        result.inventory = new Inventory(kits);
        result.encounter = encounter;
        return result;
    }
    /** Uses a kit on a living, injured member.
     * @param target the student to heal
     * @return true if a kit was consumed
     */
    public boolean useKit(Student target) {
        if (!party.contains(target)) { return false; }
        int healed = getInventory().useKit(target);
        kits = getInventory().getKits();
        return healed > 0;
    }
}
