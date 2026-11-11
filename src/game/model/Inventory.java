package game.model;

import java.util.List;

/** Shared party supplies and the owner of its single campus vest. */
public final class Inventory implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int kits;
    private int vestOwner = -1;
    /** Creates an inventory with the supplied number of kits.
     * @param kits number of first-aid kits
     */
    public Inventory(int kits) {
        if (kits < 0 || kits > 3) { throw new IllegalArgumentException("Invalid kit count"); }
        this.kits = kits;
    }
    /** Returns the current item types.
     * @return item categories available to the party
     */
    public List<Item> getItems() { return List.of(new FirstAidKit(), new Equipment(), new KeyItem()); }
    /** Returns remaining kits.
     * @return kit count
     */
    public int getKits() { return kits; }
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
    /** Assigns the vest to a student or removes it.
     * @param id student ID, or -1 to unequip
     */
    public void setVestOwner(int id) {
        if (id < -1 || id > Student.ANGEL) { throw new IllegalArgumentException("Invalid vest owner"); }
        vestOwner = id;
    }
}
