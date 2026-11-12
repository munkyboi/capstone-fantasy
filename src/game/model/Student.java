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
    private static final int[] STARTING_HEALTH = {60, 150, 90, 125};
    private static final int[] STARTING_ATTACK = {24, 16, 11, 25};
    private static final int[] STARTING_DEFENSE = {4, 16, 5, 10};
    private static final int[] STARTING_SPEED = {17, 15, 19, 5};
    private static final int[] MAX_SP = {180, 35, 60, 30};
    private static final int[] STARTING_AGILITY = {7, 4, 13, 10};
    private static final int[] STARTING_ACCURACY = {8, 9, 12, 10};
    private static final int[] ABILITY_SP_COST = {15, 5, 6, 10};
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
    private long experienceOverflow; // Additional XP beyond the legacy serialized int field.
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
    private transient boolean etherealCharged;
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
    */ public int getMaxHp() { return statWithEquipment(Stat.HP, null, null); }
    /** Returns attack value.
     * @return attack value
     */ public int getAttack() { return statWithEquipment(Stat.ATK, null, null); }
    /** Returns attack before equipment bonuses.
     * @return base attack
     */
    public int getBaseAttack() { return attack + (getLevel() - 1) * 2 + allocatedAttack; }
    /** Returns defense value.
     * @return defense value
     */ public int getDefense() {
        int value = statWithEquipment(Stat.DEF, null, null);
        return defending ? (int) Math.round(value * 1.2) : value;
    }
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
    /** Returns speed value.
     * @return speed value
    */ public int getSpeed() { return (int) Math.round(effectiveSpeedWithEquipment(null, null)); }
    /** Agility and accuracy each increase by one per level. */
    public int getAgility() { return STARTING_AGILITY[id] + getLevel() - 1; }
    public int getAccuracy() { return STARTING_ACCURACY[id] + getLevel() - 1; }
    /** Precise equipment-adjusted combat stats, including fractional percentage bonuses. */
    public double getEffectiveAgility() { return agilityWithEquipment(null, null); }
    public double getEffectiveAccuracy() { return accuracyWithEquipment(null, null); }
    public double getEffectiveSpeed() { return effectiveSpeedWithEquipment(null, null); }
    public double agilityWithEquipment(Equipment.Slot slot, String replacement) {
        String weapon = slot == Equipment.Slot.WEAPON ? replacement : weaponId;
        String armor = slot == Equipment.Slot.ARMOR ? replacement : armorId;
        return getAgility() * (1.0 + ("hunter_crossbows".equals(weapon) ? 0.05 : 0)
                + ("training_uniform".equals(armor) ? 0.05 : 0));
    }
    public double accuracyWithEquipment(Equipment.Slot slot, String replacement) {
        String weapon = slot == Equipment.Slot.WEAPON ? replacement : weaponId;
        String accessory = slot == Equipment.Slot.ACCESSORY ? replacement : accessoryId;
        String armor = slot == Equipment.Slot.ARMOR ? replacement : armorId;
        return getAccuracy() * (1.0 + ("training_uniform".equals(armor) ? 0.05 : 0) + ("scholar_staff".equals(weapon) ? 0.05 : 0)
                + ("focus_charm".equals(accessory) ? 0.05 : 0));
    }
    public double effectiveSpeedWithEquipment(Equipment.Slot slot, String replacement) {
        String weapon = slot == Equipment.Slot.WEAPON ? replacement : weaponId;
        return statWithEquipment(Stat.SPD, slot, replacement)
                + ("champion_wraps".equals(weapon) ? getUnmodifiedSpeed() * 0.05 : 0);
    }
    /** Previews a stat if a slot were changed; pass a null slot for current equipment. */
    public int statWithEquipment(Stat stat, Equipment.Slot slotToReplace, String replacement) {
        String weapon = slotToReplace == Equipment.Slot.WEAPON ? replacement : weaponId;
        String armor = slotToReplace == Equipment.Slot.ARMOR ? replacement : armorId;
        String accessory = slotToReplace == Equipment.Slot.ACCESSORY ? replacement : accessoryId;
        int value = switch (stat) {
            case HP -> maxHp + (getLevel() - 1) * 5 + allocatedHp * 5;
            case ATK -> getBaseAttack();
            case DEF -> getBaseDefense() + (armor == null && slotToReplace != Equipment.Slot.ARMOR && vestEquipped ? Equipment.DEFENSE_BONUS : 0);
            case SPD -> speed + allocatedSpeed + (getLevel() - 1) / 5;
            default -> throw new IllegalArgumentException("Unsupported equipment stat");
        };
        for (String id : new String[]{weapon, armor, accessory}) {
            if (id != null) {
                Equipment equipment = Equipment.fromId(id);
                if (stat == Stat.ATK) value += equipment.getAttackBonus();
                if (stat == Stat.DEF) value += equipment.getDefenseBonus();
            }
        }
        double multiplier = stat != Stat.HP && "ring_of_power".equals(accessory) ? 1.5 : 1.0;
        if (stat == Stat.HP && ("campus_vest".equals(armor)
                || (armor == null && slotToReplace != Equipment.Slot.ARMOR && vestEquipped))) multiplier += 0.05;
        if (stat == Stat.HP && "blade_mail".equals(armor)) multiplier += 2.0;
        if (stat == Stat.HP && "designer_leather_vest".equals(armor)) value += 50;
        if (stat == Stat.HP && "ethereal_robe".equals(armor)) value += 100;
        if (stat == Stat.DEF && "blade_mail".equals(armor)) multiplier -= 0.5;
        if (stat == Stat.DEF && "ultra_instinct_shirt".equals(armor)) multiplier -= 0.2;
        if (stat == Stat.DEF && "designer_leather_vest".equals(armor)) multiplier += 0.1;
        if (stat == Stat.DEF && "ethereal_robe".equals(armor)) multiplier += 0.15;
        if (stat == Stat.SPD && "blade_mail".equals(armor)) multiplier -= 0.2;
        if (stat == Stat.SPD && "ultra_instinct_shirt".equals(armor)) multiplier += 0.2;
        if (stat == Stat.SPD && "designer_leather_vest".equals(armor)) multiplier += 0.2;
        if (stat == Stat.ATK && ("demon_hunter".equals(weapon) || "great_sage_staff".equals(weapon))) multiplier += 0.2;
        if (stat == Stat.ATK && "giant_slayer_gloves".equals(weapon)) multiplier += 0.5;
        if (stat == Stat.SPD && "light_saber".equals(weapon)) multiplier += 0.2;
        if (stat == Stat.SPD && "demon_hunter".equals(weapon)) multiplier += 0.75;
        if (stat == Stat.SPD && "giant_slayer_gloves".equals(weapon)) value += 15;
        return (int) Math.round(value * multiplier) + (stat == Stat.HP && "sentinel_band".equals(accessory) ? 50 : 0);
    }
    /** Preserve health proportion when equipment changes maximum HP. */
    void adjustHpForEquipment(int previousHp, int previousMax) {
        if (previousHp <= 0) { hp = 0; return; }
        hp = Math.max(1, Math.min(getMaxHp(), (int) Math.round((double) previousHp * getMaxHp() / previousMax)));
    }
    /** Speed before equipment, used to calculate equipment AP bonuses. */
    public int getUnmodifiedSpeed() { return speed + allocatedSpeed + (getLevel() - 1) / 5; }
    /** Returns current level.
     * @return current level
    */ public int getLevel() {
        int level = 1;
        while (level < 100 && getExperience() >= experienceForLevel(level + 1, experienceGrowth)) { level++; }
        return level;
    }
    private static long experienceForLevel(int level, int growth) {
        long total = 0;
        for (int current = 1; current < level; current++) {
            total += Math.round(100.0 * Math.pow(1.0 + growth / 100.0, current - 1));
        }
        return total;
    }
    /** XP needed to advance from this level; zero at or above the current cap. */
    public long getXpRequired() {
        int level = getLevel();
        return level >= getLevelCap() || level >= 100 ? 0
                : experienceForLevel(level + 1, experienceGrowth) - experienceForLevel(level, experienceGrowth);
    }
    /** XP earned toward the next level. */
    public long getXpProgress() { return getXpRequired() == 0 ? 0 : getExperience() - experienceForLevel(getLevel(), experienceGrowth); }
    public int getExperienceGrowth() { return experienceGrowth; }
    public int getLevelCap() { return configuredLevelCap == 0 ? 30 : configuredLevelCap; }
    /** Change progression while preserving level and fraction of progress toward the next level. */
    public void configureProgression(int growth, int cap) {
        if (growth < 0 || growth > 20 || (cap < 20 || cap > 100 || cap % 10 != 0)) throw new IllegalArgumentException("Invalid progression");
        int level = getLevel();
        long oldStart = experienceForLevel(level, experienceGrowth);
        long oldNext = level == 100 ? oldStart : experienceForLevel(level + 1, experienceGrowth);
        double fraction = oldNext == oldStart ? 0 : (double) (getExperience() - oldStart) / (oldNext - oldStart);
        long newStart = experienceForLevel(level, growth);
        long newNext = level == 100 ? newStart : experienceForLevel(level + 1, growth);
        setExperience(newStart + Math.min(Math.max(0, newNext - newStart - 1), Math.round(fraction * (newNext - newStart))));
        experienceGrowth = growth; configuredLevelCap = cap;
    }
    /** Restores the progression rules before restoring experience. */
    public void restoreProgression(int growth, int cap) {
        if (growth < 0 || growth > 20 || (cap < 20 || cap > 100 || cap % 10 != 0)) throw new IllegalArgumentException("Invalid progression");
        experienceGrowth = growth; configuredLevelCap = cap;
    }
    /** Returns total earned experience. */
    public long getExperience() { return (long) experience + experienceOverflow; }
    private void setExperience(long value) {
        experience = (int) Math.min(Integer.MAX_VALUE, value);
        experienceOverflow = value - experience;
    }
    /** Restores validated experience without applying healing. */
    public void restoreExperience(long value) {
        if (value < 0 || value > experienceForLevel(100, experienceGrowth)) { throw new IllegalArgumentException("Invalid experience"); }
        setExperience(value);
    }
    /** Grants XP and fully restores HP and SP when at least one level is gained. */
    public int gainExperience(long amount) {
        if (amount < 0) { throw new IllegalArgumentException("Invalid experience"); }
        int oldLevel = getLevel();
        long limit = experienceForLevel(getLevelCap(), experienceGrowth);
        if (getLevel() < getLevelCap()) { setExperience(getExperience() + Math.min(amount, limit - getExperience())); }
        int gained = getLevel() - oldLevel;
        if (gained > 0) { hp = getMaxHp(); skillPoints = getMaxSp(); skillPointsInitialized = true; }
        return gained;
    }
    /** Returns current and maximum skill points and the ability cost. */
    public int getSp() { return skillPointsInitialized ? skillPoints : getMaxSp(); }
    /** Whether a serialized student has an explicit SP balance. */
    public boolean hasInitializedSkillPoints() { return skillPointsInitialized; }
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
            case HP -> { int previousMax = getMaxHp(); allocatedHp++; if (isAlive()) { hp += getMaxHp() - previousMax; } }
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
    public int getAbilitySpCost() { return ABILITY_SP_COST[id] + getLevel() - 1; }
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
    /** Activates defensive bonuses for a guard action. */ public void defend() { defending = true; }
    public boolean isDefending() { return defending; }
    public void endDefend() { defending = false; }
    /** Clears temporary combat state between encounters. */ public void clearDefense() { defending = false; etherealCharged = false; }
    public boolean hasEtherealCharge() { return etherealCharged; }
    void chargeEthereal() { etherealCharged = true; }
    boolean consumeEtherealCharge() { boolean charged = etherealCharged; etherealCharged = false; return charged; }
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
    /** Applies damage.
     * @param amount incoming damage
     * @return actual HP lost
     */
    public int takeDamage(int amount) {
        int damage = Math.max(MINIMUM_DAMAGE, amount);
        int old = hp; hp = Math.max(0, hp - damage); return old - hp;
    }
}
