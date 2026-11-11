package game.model;

/** A reusable piece of defensive equipment. */
public final class Equipment extends Item {
    private static final long serialVersionUID = 1L;
    /** Defense provided by the campus vest. */
    public static final int DEFENSE_BONUS = 4;
    /** Creates the campus vest. */
    public Equipment() { super("Campus Vest"); }
    @Override public String getCategory() { return "Equipment"; }
    /** Returns the defense bonus.
     * @return defense added while equipped
     */
    public int getDefenseBonus() { return DEFENSE_BONUS; }
}
