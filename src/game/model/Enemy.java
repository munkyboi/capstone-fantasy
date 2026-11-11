package game.model;

/** An opponent in a battle. */
public final class Enemy {
    private static final int WISP_A_AP = 24;
    private static final int WISP_B_AP = 26;
    private static final int SENTINEL_AP = 32;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private int hp;
    private final boolean sentinel;
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
        this.name = name;
        this.sentinel = sentinel;
        maxHp = sentinel ? 105 : 65;
        hp = maxHp;
        attack = sentinel ? 20 : 16;
        defense = sentinel ? 9 : 5;
        speed = sentinel ? 5 : 8;
        actionThreshold = sentinel ? SENTINEL_AP : name.endsWith("B") ? WISP_B_AP : WISP_A_AP;
    }
    /** @return whether this enemy is a Stone Sentinel */ public boolean isSentinel() { return sentinel; }
    /** @return enemy name */ public String getName() { return name; }
    /** @return current HP */ public int getHp() { return hp; }
    /** @return maximum HP */ public int getMaxHp() { return maxHp; }
    /** @return attack stat */ public int getAttack() { return attack; }
    /** @return defense stat */ public int getDefense() { return defense; }
    /** @return speed stat */ public int getSpeed() { return speed; }
    /** @return AP required before this enemy can attack */ public int getActionThreshold() { return actionThreshold; }
    /** @return whether this enemy can act */ public boolean isAlive() { return hp > 0; }
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
