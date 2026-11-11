package game.model;

/** A student taking part in the first battle. */
public final class Student implements java.io.Serializable {
    /** Character selection index for Zack. */
    public static final int ZACK = 0;
    /** Character selection index for Bogart. */
    public static final int BOGART = 1;
    /** Character selection index for Charice. */
    public static final int CHARICE = 2;
    /** Character selection index for Angel. */
    public static final int ANGEL = 3;
    private static final int STARTING_LEVEL = 1;
    private static final int MINIMUM_DAMAGE = 1;
    private static final long serialVersionUID = 1L;
    private static final int[] STARTING_HEALTH = {80, 120, 90, 105};
    private static final int[] STARTING_ATTACK = {18, 15, 14, 10};
    private static final int[] STARTING_DEFENSE = {6, 14, 8, 10};
    private static final int[] STARTING_SPEED = {10, 7, 16, 9};
    private static final int DEFENSE_DIVISOR = 2;
    private static final int[] ACTION_THRESHOLDS = {35, 30, 28, 45};
    private final int id;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private final int level = STARTING_LEVEL;
    private int hp;
    private boolean defending;

    /** Creates a student from the character-selection index.
     * @param id character index from zero to three
     */
    public Student(int id) {
        String[] names = {"Zack", "Bogart", "Charice", "Angel"};
        if (id < 0 || id >= names.length) { throw new IllegalArgumentException("Unknown student"); }
        this.id = id; name = names[id]; maxHp = STARTING_HEALTH[id]; hp = maxHp;
        attack = STARTING_ATTACK[id]; defense = STARTING_DEFENSE[id]; speed = STARTING_SPEED[id];
    }

    /** Returns character index.
     * @return character index
     */ public int getId() { return id; }
    /** Returns student name.
     * @return student name
     */ public String getName() { return name; }
    /** Returns current HP.
     * @return current HP
     */ public int getHp() { return hp; }
    /** Returns maximum HP.
     * @return maximum HP
     */ public int getMaxHp() { return maxHp; }
    /** Returns attack value.
     * @return attack value
     */ public int getAttack() { return attack; }
    /** Returns defense value.
     * @return defense value
     */ public int getDefense() { return defense; }
    /** Returns speed value.
     * @return speed value
     */ public int getSpeed() { return speed; }
    /** Returns current level.
     * @return current level
     */ public int getLevel() { return level; }
    /** Returns AP required before this student can act.
     * @return AP required before this student can act
     */
    public int getActionThreshold() { return ACTION_THRESHOLDS[id]; }
    /** Returns whether the student can act.
     * @return whether the student can act
     */ public boolean isAlive() { return hp > 0; }
    /** Protects against the next incoming attack. */ public void defend() { defending = true; }
    /** Clears defense between encounters. */ public void clearDefense() { defending = false; }
    /** Restores validated HP from a saved game.
     * @param savedHp current HP to restore
     */
    void restoreHp(int savedHp) {
        if (savedHp < 0 || savedHp > maxHp) { throw new IllegalArgumentException("Invalid HP"); }
        hp = savedHp;
        defending = false;
    }
    /** Restores HP up to the maximum.
     * @param amount amount to restore
     * @return actual HP restored
     */
    public int heal(int amount) { int old = hp; hp = Math.min(maxHp, hp + Math.max(0, amount)); return hp - old; }
    /** Applies damage, using and clearing a defend action if present.
     * @param amount incoming damage
     * @return actual HP lost
     */
    public int takeDamage(int amount) {
        int damage = Math.max(MINIMUM_DAMAGE, defending ? amount / DEFENSE_DIVISOR : amount);
        defending = false;
        int old = hp; hp = Math.max(0, hp - damage); return old - hp;
    }
}
