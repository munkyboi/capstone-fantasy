package game.model;

/** An opponent in a battle. */
public final class Enemy extends GameCharacter {
    private static final long serialVersionUID = 1L;
    private static final int WISP_MAX_HP = 65, SENTINEL_MAX_HP = 105;
    private static final int WISP_ATTACK = 16, SENTINEL_ATTACK = 20;
    private static final int WISP_DEFENSE = 5, SENTINEL_DEFENSE = 9;
    private static final int WISP_SPEED = 8, SENTINEL_SPEED = 5;
    private static final int WISP_A_AP = 24;
    private static final int WISP_AP_STEP = 2;
    private static final int SENTINEL_AP = 32;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private int hp;
    private final boolean sentinel;
    private final boolean boss;
    private final int actionThreshold;

    /** Creates one Wisp with its own name and battle stats.
     * @param name display name
     */
    public Enemy(String name) {
        this(name, false);
    }
    /** Creates a Wisp or Stone Sentinel.
     * @param name display name
     * @param sentinel whether this is a Stone Sentinel
     */
    public Enemy(String name, boolean sentinel) {
        this(name, sentinel, false);
    }
    /** Creates an enemy and marks whether it is a final boss.
     * @param name display name
     * @param sentinel whether this uses sentinel stats and artwork
     * @param boss whether this enemy uses the final-boss formation slot
     */
    public Enemy(String name, boolean sentinel, boolean boss) {
        this.name = name;
        this.sentinel = sentinel;
        this.boss = boss;
        maxHp = sentinel ? SENTINEL_MAX_HP : WISP_MAX_HP;
        hp = maxHp;
        attack = sentinel ? SENTINEL_ATTACK : WISP_ATTACK;
        defense = sentinel ? SENTINEL_DEFENSE : WISP_DEFENSE;
        speed = sentinel ? SENTINEL_SPEED : WISP_SPEED;
        char suffix = name.isEmpty() ? 'A' : name.charAt(name.length() - 1);
        int wispIndex = suffix >= 'A' && suffix <= 'F' ? suffix - 'A' : 0;
        actionThreshold = sentinel ? SENTINEL_AP : WISP_A_AP + WISP_AP_STEP * wispIndex;
    }
    /** Returns whether this enemy is the final boss.
     * @return true for the final boss
     */
    public boolean isBoss() { return boss; }
    /** Returns whether this enemy is a Stone Sentinel.
     * @return whether this enemy is a Stone Sentinel
     */ public boolean isSentinel() { return sentinel; }
    /** Returns enemy name.
     * @return enemy name
     */ public String getName() { return name; }
    /** Returns current HP.
     * @return current HP
     */ public int getHp() { return hp; }
    /** Returns maximum HP.
     * @return maximum HP
     */ public int getMaxHp() { return maxHp; }
    /** Returns attack stat.
     * @return attack stat
     */ public int getAttack() { return attack; }
    /** Returns defense stat.
     * @return defense stat
     */ public int getDefense() { return defense; }
    /** Returns speed stat.
     * @return speed stat
     */ public int getSpeed() { return speed; }
    /** Returns AP required before this enemy can attack.
     * @return AP required before this enemy can attack
     */ public int getActionThreshold() { return actionThreshold; }
    /** Returns whether this enemy can act.
     * @return whether this enemy can act
     */ public boolean isAlive() { return hp > 0; }
    /** Applies damage and returns the actual HP lost.
     * @param amount incoming damage
     * @return HP lost
     */
    public int takeDamage(int amount) {
        int actual = Math.min(hp, Math.max(0, amount));
        hp -= actual;
        return actual;
    }
}
