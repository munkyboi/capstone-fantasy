package game.model;

/** A student taking part in the first battle. */
public class Student extends GameCharacter {
    /** Character selection index for Zack. */
    public static final int ZACK = 0;
    /** Character selection index for Bogart. */
    public static final int BOGART = 1;
    /** Character selection index for Charice. */
    public static final int CHARICE = 2;
    /** Character selection index for Angel. */
    public static final int ANGEL = 3;
    /** The available student abilities. */
    public enum AbilityType {
        /** Mage ability. */ ARCANE_BURST,
        /** Warrior ability. */ CADET_STRIKE,
        /** Hunter ability. */ TWIN_SHOT,
        /** Monk ability. */ HEALING_KISS
    }
    /** Stats that can receive level-up points. */
    public enum Stat { HP, SP, ATK, DEF, SPD }
    /** Creates the appropriate playable class for a selection index.
     * @param id character index
     * @return corresponding hero
     */
    public static Student create(int id) {
        return switch (id) {
            case ZACK -> new Hero.Mage();
            case BOGART -> new Hero.Warrior();
            case CHARICE -> new Hero.Hunter();
            case ANGEL -> new Hero.Monk();
            default -> throw new IllegalArgumentException("Unknown student");
        };
    }
    /** Returns the class-specific ability.
     * @return ability type
     */
    public AbilityType getAbilityType() { return AbilityType.values()[id]; }
    private static final int STARTING_LEVEL = 1;
    private static final int MINIMUM_DAMAGE = 1;
    private static final long serialVersionUID = 1L;
    private static final int[] STARTING_HEALTH = {80, 120, 90, 105};
    private static final int[] STARTING_ATTACK = {22, 15, 14, 25};
    private static final int[] STARTING_DEFENSE = {6, 13, 8, 10};
    private static final int[] STARTING_SPEED = {12, 12, 18, 5};
    private static final int[] MAX_SP = {150, 50, 45, 30};
    private static final int[] ABILITY_SP_COST = {15, 5, 6, 10};
    private static final int DEFENSE_DIVISOR = 2;
    private static final int[] ACTION_THRESHOLDS = {35, 30, 28, 50};
    private final int id;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private final int level = STARTING_LEVEL;
    private int hp;
    private int experience;
    // Zero denotes the original fixed-100-XP curve in older saves.
    private int experienceGrowth;
    private int configuredLevelCap;
    private int allocatedHp, allocatedSp, allocatedAttack, allocatedDefense, allocatedSpeed;
    /** Maximum experience: level 30. */
    public static final int MAX_EXPERIENCE = 2900;
    // False identifies a serialized save from before SP existed.
    private int skillPoints;
    private boolean skillPointsInitialized;
    private boolean defending;
    private boolean vestEquipped;
    private String weaponId;
    private String armorId;
    private String accessoryId;

    /** Creates a student from the character-selection index.
     * @param id character index from zero to three
     */
    public Student(int id) {
        String[] names = {"Zack", "Bogart", "Charice", "Angel"};
        if (id < 0 || id >= names.length) { throw new IllegalArgumentException("Unknown student"); }
        this.id = id; name = names[id]; maxHp = STARTING_HEALTH[id]; hp = maxHp;
        attack = STARTING_ATTACK[id]; defense = STARTING_DEFENSE[id]; speed = STARTING_SPEED[id];
        skillPoints = MAX_SP[id]; skillPointsInitialized = true;
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
    */ public int getMaxHp() { return maxHp + (getLevel() - 1) * 5 + allocatedHp * 5; }
    /** Returns attack value.
     * @return attack value
     */ public int getAttack() { return getBaseAttack() + equipmentAttack(); }
    /** Returns attack before equipment bonuses.
     * @return base attack
     */
    public int getBaseAttack() { return attack + (getLevel() - 1) * 2 + allocatedAttack; }
    /** Returns defense value.
     * @return defense value
     */ public int getDefense() { return getBaseDefense() + equipmentDefense(); }
    /** Returns defense before equipment bonuses.
     * @return base defense
     */
    public int getBaseDefense() { return defense + (getLevel() - 1) + allocatedDefense; }
    /** Checks whether the campus vest is equipped.
     * @return true if equipped
     */
    public boolean hasVest() { return "campus_vest".equals(armorId) || (armorId == null && vestEquipped); }
    void setVestEquipped(boolean equipped) { vestEquipped = equipped; armorId = equipped ? "campus_vest" : null; }
    /** Returns the item ID equipped in a slot.
     * @param slot equipment slot
     * @return item ID, or null
     */
    public String getEquipmentId(Equipment.Slot slot) {
        return switch (slot) { case WEAPON -> weaponId; case ARMOR -> armorId; case ACCESSORY -> accessoryId; };
    }
    void setEquipmentId(Equipment.Slot slot, String item) {
        switch (slot) { case WEAPON -> weaponId = item; case ARMOR -> { armorId = item; vestEquipped = "campus_vest".equals(item); }
            case ACCESSORY -> accessoryId = item; }
    }
    private int equipmentAttack() {
        int total = 0;
        for (Equipment.Slot slot : Equipment.Slot.values()) {
            String item = getEquipmentId(slot); if (item != null) { total += Equipment.fromId(item).getAttackBonus(); }
        }
        return total;
    }
    private int equipmentDefense() {
        int total = armorId == null && vestEquipped ? Equipment.DEFENSE_BONUS : 0;
        for (Equipment.Slot slot : Equipment.Slot.values()) {
            String item = getEquipmentId(slot); if (item != null) { total += Equipment.fromId(item).getDefenseBonus(); }
        }
        return total;
    }
    /** Returns speed value.
     * @return speed value
    */ public int getSpeed() { return speed + allocatedSpeed; }
    /** Returns current level.
     * @return current level
    */ public int getLevel() {
        int level = 1;
        while (level < 50 && experience >= experienceForLevel(level + 1, experienceGrowth)) { level++; }
        return level;
    }
    private static int experienceForLevel(int level, int growth) {
        long total = 0;
        for (int current = 1; current < level; current++) {
            total += Math.round(100.0 * Math.pow(1.0 + growth / 100.0, current - 1));
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
    /** XP needed to advance from this level; zero at or above the current cap. */
    public int getXpRequired() {
        int level = getLevel();
        return level >= getLevelCap() || level >= 50 ? 0
                : experienceForLevel(level + 1, experienceGrowth) - experienceForLevel(level, experienceGrowth);
    }
    /** XP earned toward the next level. */
    public int getXpProgress() { return getXpRequired() == 0 ? 0 : experience - experienceForLevel(getLevel(), experienceGrowth); }
    public int getExperienceGrowth() { return experienceGrowth; }
    public int getLevelCap() { return configuredLevelCap == 0 ? 30 : configuredLevelCap; }
    /** Change progression while preserving level and fraction of progress toward the next level. */
    public void configureProgression(int growth, int cap) {
        if (growth < 0 || growth > 20 || (cap != 20 && cap != 30 && cap != 40 && cap != 50)) throw new IllegalArgumentException("Invalid progression");
        int level = getLevel();
        int oldStart = experienceForLevel(level, experienceGrowth);
        int oldNext = level == 50 ? oldStart : experienceForLevel(level + 1, experienceGrowth);
        double fraction = oldNext == oldStart ? 0 : (double) (experience - oldStart) / (oldNext - oldStart);
        int newStart = experienceForLevel(level, growth);
        int newNext = level == 50 ? newStart : experienceForLevel(level + 1, growth);
        experience = newStart + Math.min(Math.max(0, newNext - newStart - 1), (int) Math.round(fraction * (newNext - newStart)));
        experienceGrowth = growth; configuredLevelCap = cap;
    }
    /** Restores the progression rules before restoring experience. */
    public void restoreProgression(int growth, int cap) {
        if (growth < 0 || growth > 20 || (cap != 20 && cap != 30 && cap != 40 && cap != 50)) throw new IllegalArgumentException("Invalid progression");
        experienceGrowth = growth; configuredLevelCap = cap;
    }
    /** Returns total earned experience. */
    public int getExperience() { return experience; }
    /** Restores validated experience without applying healing. */
    public void restoreExperience(int value) {
        if (value < 0 || value > experienceForLevel(50, experienceGrowth)) { throw new IllegalArgumentException("Invalid experience"); }
        experience = value;
    }
    /** Grants XP and fully restores HP and SP when at least one level is gained. */
    public int gainExperience(int amount) {
        if (amount < 0) { throw new IllegalArgumentException("Invalid experience"); }
        int oldLevel = getLevel();
        int limit = experienceForLevel(getLevelCap(), experienceGrowth);
        if (getLevel() < getLevelCap()) { experience = (int) Math.min(limit, (long) experience + amount); }
        int gained = getLevel() - oldLevel;
        if (gained > 0) { hp = getMaxHp(); skillPoints = getMaxSp(); skillPointsInitialized = true; }
        return gained;
    }
    /** Returns current and maximum skill points and the ability cost. */
    public int getSp() { return skillPointsInitialized ? skillPoints : getMaxSp(); }
    /** Returns the student's maximum skill points. */
    public int getMaxSp() { return MAX_SP[id] + (getLevel() - 1) * 3 + allocatedSp * 3; }
    /** Returns points earned but not spent. */
    public int getUnspentStatPoints() {
        return getLevel() - 1 - allocatedHp - allocatedSp - allocatedAttack - allocatedDefense - allocatedSpeed;
    }
    /** Returns the number of points assigned to one stat. */
    public int getAllocatedStatPoints(Stat stat) {
        return switch (stat) { case HP -> allocatedHp; case SP -> allocatedSp; case ATK -> allocatedAttack;
            case DEF -> allocatedDefense; case SPD -> allocatedSpeed; };
    }
    /** Spends one point and applies the stat increase. KO students stay KO. */
    public boolean allocateStatPoint(Stat stat) {
        if (stat == null || getUnspentStatPoints() <= 0) { return false; }
        switch (stat) {
            case HP -> { allocatedHp++; if (isAlive()) { hp += 5; } }
            case SP -> { int oldSp = getSp(); allocatedSp++; skillPoints = oldSp + 3; skillPointsInitialized = true; }
            case ATK -> allocatedAttack++;
            case DEF -> allocatedDefense++;
            case SPD -> allocatedSpeed++;
        }
        return true;
    }
    /** Restores validated allocations without changing current HP or SP. */
    public void restoreStatAllocations(int hpPoints, int spPoints, int atkPoints, int defPoints, int spdPoints) {
        if (hpPoints < 0 || spPoints < 0 || atkPoints < 0 || defPoints < 0 || spdPoints < 0
                || (long) hpPoints + spPoints + atkPoints + defPoints + spdPoints > getLevel() - 1) {
            throw new IllegalArgumentException("Invalid stat allocation");
        }
        allocatedHp = hpPoints; allocatedSp = spPoints; allocatedAttack = atkPoints;
        allocatedDefense = defPoints; allocatedSpeed = spdPoints;
    }
    /** Returns the skill-point cost of the student's ability. */
    public int getAbilitySpCost() { return ABILITY_SP_COST[id]; }
    /** Uses skill points if enough are available. */
    public boolean spendAbilitySp() {
        if (getSp() < getAbilitySpCost()) { return false; }
        skillPoints = getSp() - getAbilitySpCost(); skillPointsInitialized = true; return true;
    }
    /** Restores validated skill points from a save. */
    public void restoreSp(int value) {
        if (value < 0 || value > getMaxSp()) { throw new IllegalArgumentException("Invalid SP"); }
        skillPoints = value; skillPointsInitialized = true;
    }
    /** Fully restores the student's SP. */
    public void restoreAllSp() { skillPoints = getMaxSp(); skillPointsInitialized = true; }
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
        if (savedHp < 0 || savedHp > getMaxHp()) { throw new IllegalArgumentException("Invalid HP"); }
        hp = savedHp;
        defending = false;
    }
    /** Restores HP up to the maximum.
     * @param amount amount to restore
     * @return actual HP restored
     */
    public int heal(int amount) { int old = hp; hp = Math.min(getMaxHp(), hp + Math.max(0, amount)); return hp - old; }
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
