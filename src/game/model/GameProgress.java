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

    /** Creates a new party of three distinct students.
     * @param ids selected character indices
     */
    public GameProgress(List<Integer> ids) {
        if (ids.size() != 3 || ids.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Choose three different students");
        }
        for (int id : ids) { party.add(Student.create(id)); }
        party.sort(Comparator.comparingInt(Student::getSpeed).reversed());
    }
    /** Returns the current party in speed order.
     * @return the current party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns first-aid kits remaining.
     * @return first-aid kits remaining
     */ public int getKits() { return getInventory().getKits(); }
    /** Returns the party's shared inventory.
     * @return shared supplies
     */
    public Inventory getInventory() {
        if (inventory == null) { inventory = new Inventory(kits); }
        return inventory;
    }
    /** Equips or removes the single shared campus vest.
     * @param student the party member selected
     * @return true if the party member is valid
     */
    public boolean toggleVest(Student student) {
        if (!party.contains(student)) { return false; }
        int owner = getInventory().getVestOwner() == student.getId() ? -1 : student.getId();
        setVestOwner(owner);
        return true;
    }
    /** Restores the vest owner and updates the party's equipment.
     * @param owner student ID, or -1 for unequipped
     */
    public void setVestOwner(int owner) {
        if (owner != -1 && party.stream().noneMatch(student -> student.getId() == owner)) {
            throw new IllegalArgumentException("Vest owner is not in the party");
        }
        getInventory().setVestOwner(owner);
        for (Student student : party) { student.setVestEquipped(student.getId() == owner); }
    }
    /** Returns zero-based current encounter.
     * @return zero-based current encounter
     */ public int getEncounter() { return encounter; }
    /** Unlocks the next route location after an encounter.
     * @return whether the route position advanced
     */
    public boolean advance() { if (encounter >= 2) { return false; } encounter++; return true; }
    /** Reconstructs and validates a saved camp state.
     * @param ids student indices
     * @param hitPoints corresponding current HP values
     * @param kits remaining first-aid kits
     * @param encounter next encounter index
     * @return reconstructed progress
     */
    public static GameProgress restore(List<Integer> ids, List<Integer> hitPoints, int kits, int encounter) {
        if (ids == null || hitPoints == null || ids.size() != 3 || hitPoints.size() != 3
                || kits < 0 || kits > STARTING_KITS || encounter < 1 || encounter > 2) {
            throw new IllegalArgumentException("Invalid saved progress");
        }
        GameProgress result = new GameProgress(ids);
        for (int i = 0; i < ids.size(); i++) {
            final int id = ids.get(i);
            Student student = result.party.stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
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
