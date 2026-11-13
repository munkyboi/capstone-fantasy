package game.model;

/** A consumable that restores a living student's HP. */
public final class FirstAidKit extends Item {
    private static final long serialVersionUID = 1L;
    private static final int HEAL_AMOUNT = 30;
    /** Creates a first-aid kit. */
    public FirstAidKit() { super("First-Aid Kit"); }
    /** {@inheritDoc} */    @Override public String getCategory() { return "Consumable"; }
    /** Heals a living student.
     * @param student recipient
     * @return HP restored
     */
    public int use(Student student) { return student.isAlive() ? student.heal(HEAL_AMOUNT) : 0; }
}
