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
    private static final int[] STAGE_PETOT_BONUS = {0, 0, 3, 6, 4};
    private static final int[] STAGE_XP_BONUS = {0, 0, 3, 6, 10};
    private static final double[] STAGE_HP_MULTIPLIER = {1.0, 1.0, 1.08, 1.16, 1.25};
    private static final int[] STAGE_ATTACK_BONUS = {0, 0, 1, 2, 3};
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private int hp;
    private final boolean sentinel;
    private final boolean boss;
    private final int actionThreshold;
    private final int petotDrop;
    private final int experienceDrop;
    private final double difficultyMultiplier;
    private int actionsTaken;

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
        this(name, sentinel, boss, GameSettings.Difficulty.NORMAL);
    }
    /** Creates an enemy scaled for the selected difficulty. */
    public Enemy(String name, boolean sentinel, boolean boss, GameSettings.Difficulty difficulty) {
        this(name, sentinel, boss, difficulty, 0);
    }
    /** Creates an enemy tuned for its route stage before applying difficulty. */
    public Enemy(String name, boolean sentinel, boolean boss, GameSettings.Difficulty difficulty, int stage) {
        if (stage < 0 || stage > 4) throw new IllegalArgumentException("Invalid stage");
        this.name = name;
        this.sentinel = sentinel;
        this.boss = boss;
        difficultyMultiplier = java.util.Objects.requireNonNull(difficulty).multiplier();
        petotDrop = boss && stage == 4 ? 110 : (boss ? 150 : sentinel ? 35 : 12) + STAGE_PETOT_BONUS[stage];
        experienceDrop = boss && stage == 4 ? 80 : (boss ? 120 : sentinel ? 35 : 20) + STAGE_XP_BONUS[stage];
        maxHp = Math.max(1, (int) Math.round((boss ? 260 : sentinel ? SENTINEL_MAX_HP : WISP_MAX_HP) * STAGE_HP_MULTIPLIER[stage] * difficultyMultiplier));
        hp = maxHp;
        attack = (boss ? 24 : sentinel ? SENTINEL_ATTACK : WISP_ATTACK) + STAGE_ATTACK_BONUS[stage];
        defense = boss ? 12 : sentinel ? SENTINEL_DEFENSE : WISP_DEFENSE;
        speed = Math.max(1, (int) Math.round((sentinel ? SENTINEL_SPEED : WISP_SPEED) * difficultyMultiplier));
        char suffix = name.isEmpty() ? 'A' : name.charAt(name.length() - 1);
        int wispIndex = suffix >= 'A' && suffix <= 'F' ? suffix - 'A' : 0;
        actionThreshold = Math.max(1, (int) Math.round((boss ? 40 : sentinel ? SENTINEL_AP : WISP_A_AP + WISP_AP_STEP * wispIndex) / difficultyMultiplier));
    }
    /** Returns whether the next action uses this monster's skill. */
    boolean usesSkillNext() { return (actionsTaken + 1) % 3 == 0; }
    /** Records an action only after it is performed. */
    void recordAction() { actionsTaken++; }
    /** Returns the experience earned by each student on victory. */
    public int getExperienceDrop() { return (int) Math.round(experienceDrop * difficultyMultiplier); }
    /** Scales outgoing damage, including monster skills. */
    public int scaleDamage(int damage) { return Math.max(1, (int) Math.round(damage * difficultyMultiplier)); }
    /** Returns the Petot awarded for defeating this enemy. */
    public int getPetotDrop() { return petotDrop; }
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
