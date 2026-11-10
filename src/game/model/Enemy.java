package game.model;

/** A Library Wisp in a battle. */
public final class Enemy {
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private int hp;

    /** Creates one Wisp with its own name and battle stats.
     * @param name display name
     */
    public Enemy(String name) {
        this.name = name;
        maxHp = 65;
        hp = maxHp;
        attack = 16;
        defense = 5;
        speed = 8;
    }
    /** @return enemy name */ public String getName() { return name; }
    /** @return current HP */ public int getHp() { return hp; }
    /** @return maximum HP */ public int getMaxHp() { return maxHp; }
    /** @return attack stat */ public int getAttack() { return attack; }
    /** @return defense stat */ public int getDefense() { return defense; }
    /** @return speed stat */ public int getSpeed() { return speed; }
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
