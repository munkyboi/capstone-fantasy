package game.model;

/** A student taking part in the first battle. */
public final class Student {
    private final int id;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private int hp;
    private boolean defending;

    /** Creates a student from the character-selection index.
     * @param id character index from zero to three
     */
    public Student(int id) {
        String[] names = {"Zack", "Bogart", "Charice", "Angel"};
        int[] health = {80, 120, 90, 105};
        int[] attacks = {18, 15, 14, 10};
        int[] defenses = {6, 14, 8, 10};
        int[] speeds = {10, 7, 16, 9};
        if (id < 0 || id >= names.length) { throw new IllegalArgumentException("Unknown student"); }
        this.id = id; name = names[id]; maxHp = health[id]; hp = maxHp;
        attack = attacks[id]; defense = defenses[id]; speed = speeds[id];
    }

    /** @return character index */ public int getId() { return id; }
    /** @return student name */ public String getName() { return name; }
    /** @return current HP */ public int getHp() { return hp; }
    /** @return maximum HP */ public int getMaxHp() { return maxHp; }
    /** @return attack value */ public int getAttack() { return attack; }
    /** @return defense value */ public int getDefense() { return defense; }
    /** @return speed value */ public int getSpeed() { return speed; }
    /** @return whether the student can act */ public boolean isAlive() { return hp > 0; }
    /** Protects against the next incoming attack. */ public void defend() { defending = true; }
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
        int damage = Math.max(1, defending ? amount / 2 : amount);
        defending = false;
        int old = hp; hp = Math.max(0, hp - damage); return old - hp;
    }
}
