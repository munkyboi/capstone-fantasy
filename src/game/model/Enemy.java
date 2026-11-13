package game.model;

/** An opponent in a battle. */
public final class Enemy extends GameCharacter {
    private static final long serialVersionUID = 1L;
    private static final int WISP_MAX_HP = 65, SENTINEL_MAX_HP = 250;
    private static final int WISP_ATTACK = 16, SENTINEL_ATTACK = 13;
    private static final int WISP_DEFENSE = 5, SENTINEL_DEFENSE = 12;
    private static final int WISP_SPEED = 3, SENTINEL_SPEED = 2;
    private static final int WISP_A_AP = 36;
    private static final int WISP_AP_STEP = 2;
    private static final int SENTINEL_AP = 32;
    private static final int SENTINEL_BASE_SP = 900;
    private static final int[] STAGE_PETOT_BONUS = {0, 0, 3, 6, 4};
    private static final int[] STAGE_XP_BONUS = {0, 0, 3, 6, 10};
    private static final double[] STAGE_HP_MULTIPLIER = {1.0, 1.0, 1.08, 1.16, 1.25};
    private static final int[] STAGE_ATTACK_BONUS = {0, 0, 1, 2, 3};
    // Final-stage multipliers indexed by active party size; difficulty still applies independently.
    private static final double[] FINAL_HP = {1, .35, .595, .7225, .80};
    private static final double[] FINAL_DAMAGE = {1, .20, .595, .7225, .75};
    private static final double[] FINAL_AP_THRESHOLD = {1, 1.8, 1.62, 1.44, 1.35};
    private static final int STONE_MEND_COOLDOWN = 3;
    /** Serialized state: name.
     * @serial
     */
    private final String name;
    /** Serialized state: max hp.
     * @serial
     */
    private final int maxHp;
    /** Serialized state: attack.
     * @serial
     */
    private final int attack;
    /** Serialized state: defense.
     * @serial
     */
    private final int defense;
    /** Serialized state: speed.
     * @serial
     */
    private final int speed;
    /** Serialized state: accuracy, agility.
     * @serial
     */
    private final int agility, accuracy;
    /** Serialized state: hp.
     * @serial
     */
    private int hp;
    /** Serialized state: sentinel.
     * @serial
     */
    private final boolean sentinel;
    /** Serialized state: attack sentinel.
     * @serial
     */
    private boolean attackSentinel;
    /** Serialized state: boss.
     * @serial
     */
    private final boolean boss;
    /** Serialized state: loot goblin.
     * @serial
     */
    private final boolean lootGoblin;
    /** Serialized state: merchant, mirror.
     * @serial
     */
    private final boolean merchant, mirror;
    /** Serialized state: escaped.
     * @serial
     */
    private boolean escaped;
    /** Serialized state: escape countdown.
     * @serial
     */
    private int escapeCountdown = -1;
    /** Serialized state: action threshold.
     * @serial
     */
    private final int actionThreshold;
    /** Serialized state: petot drop.
     * @serial
     */
    private final int petotDrop;
    /** Serialized state: experience drop.
     * @serial
     */
    private final int experienceDrop;
    /** Serialized state: difficulty multiplier.
     * @serial
     */
    private final double difficultyMultiplier;
    /** Serialized state: final damage scale.
     * @serial
     */
    private double finalDamageScale = 1.0;
    /** Serialized state: actions taken.
     * @serial
     */
    private int actionsTaken;
    /** Serialized state: skill points.
     * @serial
     */
    private int skillPoints;
    /** Serialized state: drain cooldown, life drain cooldown, mirror cooldown, missile cooldown, recall cooldown, spark cooldown, stone mend cooldown.
     * @serial
     */
    private int mirrorCooldown, missileCooldown, drainCooldown, lifeDrainCooldown, recallCooldown, sparkCooldown, stoneMendCooldown;

    /** Serialized state: stone skin cooldown, stone skin defense, stone skin hits, stone smash cooldown, stone sweep cooldown, twin defense penalty.
     * @serial
     */
    private int stoneSkinCooldown, stoneSweepCooldown, stoneSmashCooldown, stoneSkinDefense, stoneSkinHits, twinDefensePenalty;
    /** Serialized state: level curve.
     * @serial
     */
    private double levelCurve;
    /** Serialized state: grip cooldown, mesh cooldown, pulse cooldown, pulse countdown.
     * @serial
     */
    private int pulseCountdown, pulseCooldown, gripCooldown, meshCooldown;

    private Enemy(Enemy original, GameSettings.Difficulty difficulty, double partyLevel, boolean copy, int number) {
        double levels = partyLevel - 1, p = levels / 99.0, curve = p * p * (3.0 - 2.0 * p);
        levelCurve = curve;
        difficultyMultiplier = copy ? original.difficultyMultiplier : difficulty.multiplier();
        merchant = !copy; mirror = copy; sentinel = false; boss = false; lootGoblin = false;
        name = copy ? "Mirror Image " + number : "Mysterious Merchant";
        maxHp = copy ? Math.max(1, (int) Math.ceil(original.maxHp * 0.75)) : (int)Math.round(350 * difficultyMultiplier * (1 + 7.92 * curve));
        attack = copy ? (int)Math.ceil(original.attack / 2.0) : (int)Math.round(40 * (1 + 3.96 * curve));
        defense = copy ? (int)Math.ceil(original.defense / 2.0) : (int)Math.round(10 * (1 + 2.97 * curve));
        agility = copy ? (int)Math.ceil(original.agility / 2.0) : (int)Math.round(10 + levels * 0.5);
        accuracy = copy ? (int)Math.ceil(original.accuracy / 2.0) : (int)Math.round(8 + levels);
        speed = copy ? original.speed : Math.max(1,(int)Math.round(4 * difficultyMultiplier * (1 + curve)));
        actionThreshold = copy ? original.actionThreshold : Math.max(1,(int)Math.round(40 / (difficultyMultiplier * (1 + curve))));
        hp = maxHp; skillPoints = copy ? (int)Math.ceil(original.getMaxSp() * 0.75) : 800; petotDrop = 0; experienceDrop = 0;
    }
    /** Creates a merchant enemy scaled for the encounter.
     * @param difficulty selected difficulty
     * @param partyLevel average party level
     * @return the result of merchant
     */
    public static Enemy merchant(GameSettings.Difficulty difficulty, double partyLevel) { return new Enemy(null, difficulty, partyLevel, false, 0); }
    Enemy mirror(int number) { return new Enemy(this, GameSettings.Difficulty.NORMAL, 1, true, number); }
    /** Returns whether this enemy is the merchant.
     * @return whether this enemy is the merchant
     */
    public boolean isMerchant() { return merchant; }
    /** Returns whether this enemy is a merchant copy.
     * @return whether this enemy is a merchant copy
     */
    public boolean isMirror() { return mirror; }
    int merchantSkill(boolean mirrorsActive) {
        if (mirrorCooldown > 0) mirrorCooldown--; if (missileCooldown > 0) missileCooldown--; if (drainCooldown > 0) drainCooldown--; if (lifeDrainCooldown > 0) lifeDrainCooldown--; if (recallCooldown > 0) recallCooldown--;
        if (merchant && !mirrorsActive && mirrorCooldown == 0 && skillPoints >= 90) { skillPoints -= 90; mirrorCooldown = 5; return 1; }
        if (merchant && mirrorsActive && recallCooldown == 0 && skillPoints >= 30
                && (hp * 2 <= maxHp || skillPoints * 4 <= getMaxSp())) { skillPoints -= 30; recallCooldown = 5; return 5; }
        if ((mirror || !mirrorsActive) && missileCooldown == 0 && skillPoints >= 55) { skillPoints -= 55; missileCooldown = 3; return 2; }
        if (mirror) return 0;
        if (drainCooldown == 0 && skillPoints >= 15) { skillPoints -= 15; drainCooldown = 3; return 3; }
        if (lifeDrainCooldown == 0 && skillPoints >= 20) { skillPoints -= 20; lifeDrainCooldown = 3; return 4; }
        return 0;
    }
    int absorbSp(int amount) { int actual = Math.min(Math.max(0, amount), getMaxSp() - skillPoints); skillPoints += actual; return actual; }

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
    /** Creates an enemy scaled for the selected difficulty.
     * @param name display name
     * @param sentinel the sentinel
     * @param boss the boss
     * @param difficulty selected difficulty
     */
    public Enemy(String name, boolean sentinel, boolean boss, GameSettings.Difficulty difficulty) {
        this(name, sentinel, boss, difficulty, 0);
    }
    /** Creates an enemy tuned for its route stage before applying difficulty.
     * @param name display name
     * @param sentinel the sentinel
     * @param boss the boss
     * @param difficulty selected difficulty
     * @param stage stage index
     */
    public Enemy(String name, boolean sentinel, boolean boss, GameSettings.Difficulty difficulty, int stage) {
        this(name, sentinel, boss, difficulty, stage, 1.0);
    }
    /** Creates an enemy scaled to the active party's average level.
     * @param name display name
     * @param sentinel the sentinel
     * @param boss the boss
     * @param difficulty selected difficulty
     * @param stage stage index
     * @param partyLevel average party level
     */
    public Enemy(String name, boolean sentinel, boolean boss, GameSettings.Difficulty difficulty, int stage, double partyLevel) {
        this(name, sentinel, boss, false, difficulty, stage, partyLevel);
    }
    /** Creates the rare loot goblin, using the same scaling rules as other monsters.
     * @param difficulty selected difficulty
     * @param stage stage index
     * @param partyLevel average party level
     * @return the result of itmigims
     */
    public static Enemy itmigims(GameSettings.Difficulty difficulty, int stage, double partyLevel) {
        return new Enemy("Itmigims", false, false, true, difficulty, stage, partyLevel);
    }
    private Enemy(String name, boolean sentinel, boolean boss, boolean lootGoblin,
            GameSettings.Difficulty difficulty, int stage, double partyLevel) {
        this(name, sentinel, boss, lootGoblin, difficulty, stage, partyLevel, 0);
    }
    /** Creates a final-stage opponent adjusted for the active party size.
     * @param name display name
     * @param boss the boss
     * @param difficulty selected difficulty
     * @param partyLevel average party level
     * @param partySize the party size
     * @return the result of final opponent
     */
    public static Enemy finalOpponent(String name, boolean boss, GameSettings.Difficulty difficulty, double partyLevel, int partySize) {
        if (partySize < 1 || partySize > 4) throw new IllegalArgumentException("Invalid party size");
        return new Enemy(name, true, boss, false, difficulty, 4, partyLevel, partySize);
    }
    /** Creates a support or attack Sentinel for the encounter.
     * @param name display name
     * @param attack the attack
     * @param difficulty selected difficulty
     * @param stage stage index
     * @param partyLevel average party level
     * @param partySize the party size
     * @return the result of sentinel
     */
    public static Enemy sentinel(String name, boolean attack, GameSettings.Difficulty difficulty, int stage, double partyLevel, int partySize) {
        if (partySize < 0 || partySize > 4) throw new IllegalArgumentException("Invalid party size");
        return new Enemy(name, true, false, false, difficulty, stage, partyLevel, partySize, false, attack);
    }
    /** Opening-stage Wisp adjusted for a party of one.
     * @param name display name
     * @param difficulty selected difficulty
     * @param partyLevel average party level
     * @return the result of solo opening wisp
     */
    public static Enemy soloOpeningWisp(String name, GameSettings.Difficulty difficulty, double partyLevel) {
        return new Enemy(name, false, false, false, difficulty, 0, partyLevel, 0, true);
    }
    private Enemy(String name, boolean sentinel, boolean boss, boolean lootGoblin,
            GameSettings.Difficulty difficulty, int stage, double partyLevel, int finalPartySize) {
        this(name, sentinel, boss, lootGoblin, difficulty, stage, partyLevel, finalPartySize, false);
    }
    private Enemy(String name, boolean sentinel, boolean boss, boolean lootGoblin,
            GameSettings.Difficulty difficulty, int stage, double partyLevel, int finalPartySize, boolean soloOpening) {
        this(name, sentinel, boss, lootGoblin, difficulty, stage, partyLevel, finalPartySize, soloOpening, false);
    }
    private Enemy(String name, boolean sentinel, boolean boss, boolean lootGoblin,
            GameSettings.Difficulty difficulty, int stage, double partyLevel, int finalPartySize, boolean soloOpening, boolean attackVariant) {
        if (stage < 0 || stage > 4) throw new IllegalArgumentException("Invalid stage");
        if (!Double.isFinite(partyLevel) || partyLevel < 1 || partyLevel > 100) throw new IllegalArgumentException("Invalid party level");
        double levels = partyLevel - 1;
        // Smoothstep is a bounded S-curve: gentle at both ends, steepest at the midpoint.
        double progress = levels / 99.0;
        double curve = progress * progress * (3.0 - 2.0 * progress);
        levelCurve = curve; attackSentinel = attackVariant;
        double healthScale = 1.0 + 7.92 * curve;
        double attackScale = 1.0 + 3.96 * curve;
        double defenseScale = 1.0 + 2.97 * curve;
        double speedScale = 1.0 + curve;
        double xpScale = 1.0 + levels * 0.05;
        this.name = name;
        this.sentinel = sentinel;
        this.boss = boss;
        this.lootGoblin = lootGoblin;
        this.merchant = false; this.mirror = false;
        difficultyMultiplier = java.util.Objects.requireNonNull(difficulty).multiplier();
        skillPoints = lootGoblin ? 100 : boss ? 2000 : sentinel ? SENTINEL_BASE_SP : 80;
        petotDrop = lootGoblin ? 5000 : boss && stage == 4 ? 50000 : (boss ? 150 : sentinel ? 35 : 12) + STAGE_PETOT_BONUS[stage];
        experienceDrop = lootGoblin ? 0 : (int) Math.round((boss && stage == 4 ? 10000 : (boss ? 120 : sentinel ? 35 : 20) + STAGE_XP_BONUS[stage]) * xpScale);
        double partyHealth = FINAL_HP[finalPartySize] * (soloOpening ? 0.50 : 1.0);
        finalDamageScale = FINAL_DAMAGE[finalPartySize] * (soloOpening ? 0.50 : 1.0);
        double partyThreshold = FINAL_AP_THRESHOLD[finalPartySize] * (soloOpening ? 1.6 : 1.0);
        maxHp = Math.max(1, (int) Math.round((lootGoblin ? 450 : boss ? 1000 : sentinel ? SENTINEL_MAX_HP : WISP_MAX_HP) * STAGE_HP_MULTIPLIER[stage] * difficultyMultiplier * healthScale * partyHealth));
        hp = maxHp;
        attack = (int) Math.round(((lootGoblin ? 1 : boss ? 32 : sentinel ? attackSentinel ? 20 : SENTINEL_ATTACK : WISP_ATTACK) + STAGE_ATTACK_BONUS[stage]) * attackScale);
        defense = (int) Math.round((lootGoblin ? 18 : boss ? 24 : sentinel ? attackSentinel ? 9 : SENTINEL_DEFENSE : WISP_DEFENSE) * defenseScale);
        agility = Math.max(1, (int) Math.round((lootGoblin ? 18 : boss ? 14 : sentinel ? 5 : 9) + levels * 0.5));
        accuracy = Math.max(1, (int) Math.round((lootGoblin ? 18 : boss ? 20 : sentinel ? 9 : 8) + levels));
        speed = Math.max(1, (int) Math.round((lootGoblin ? 15 : boss ? 1 : sentinel ? SENTINEL_SPEED : WISP_SPEED) * difficultyMultiplier * speedScale));
        char suffix = name.isEmpty() ? 'A' : name.charAt(name.length() - 1);
        int wispIndex = suffix >= 'A' && suffix <= 'F' ? suffix - 'A' : 0;
        actionThreshold = Math.max(1, (int) Math.round((lootGoblin ? 30 : boss ? 40 : sentinel ? SENTINEL_AP : WISP_A_AP + WISP_AP_STEP * wispIndex) * partyThreshold / (difficultyMultiplier * speedScale)));
    }
    /** Sentinel healing resources are renewed for each battle.
     * @return the sp
     */
    public int getSp() { return skillPoints; }
    /** Removes up to the supplied amount of current SP. */
    int loseSp(int amount) { int lost = Math.min(skillPoints, Math.max(0, amount)); skillPoints -= lost; return lost; }
    /** Returns the enemy’s maximum skill points.
     * @return the enemy\u2019s maximum skill points
     */
    public int getMaxSp() { return merchant ? 800 : mirror ? 600 : lootGoblin ? 100 : boss ? 2000 : sentinel ? SENTINEL_BASE_SP : 80; }
    int guardianAction(boolean canSummonCompanions) {
        if (gripCooldown > 0) gripCooldown--;
        if (meshCooldown > 0) meshCooldown--;
        if (pulseCountdown > 0 && --pulseCountdown == 0) return 2;
        if (pulseCooldown > 0) pulseCooldown--;
        if (canSummonCompanions && meshCooldown == 0 && skillPoints >= 100) { skillPoints -= 100; meshCooldown = 6; return 4; }
        if (pulseCountdown == 0 && pulseCooldown == 0 && skillPoints >= 50) { skillPoints -= 50; pulseCountdown = 5; return 1; }
        if (gripCooldown == 0 && skillPoints >= 15) { skillPoints -= 15; gripCooldown = 4; return 3; }
        return 0;
    }
    void finishPulse() { pulseCooldown = 6; }
    boolean useArcaneSpark() {
        if (sparkCooldown > 0) { sparkCooldown--; return false; }
        if (actionsTaken < 2 || skillPoints < 12) return false;
        skillPoints -= 12; sparkCooldown = 3; return true;
    }
    /** Stone Mend costs 15 SP.
     * @return the heal sp cost
     */
    public int getHealSpCost() { return 15; }
    boolean canUseStoneMend() { if (stoneMendCooldown > 0) { stoneMendCooldown--; return false; } return skillPoints >= getHealSpCost(); }
    boolean spendHealSp() { int cost = getHealSpCost(); if (skillPoints < cost) return false; skillPoints -= cost; stoneMendCooldown = STONE_MEND_COOLDOWN; return true; }
    void sentinelCooldowns() { if(stoneSkinCooldown>0) stoneSkinCooldown--; if(stoneSweepCooldown>0) stoneSweepCooldown--; if(stoneSmashCooldown>0) stoneSmashCooldown--; }
    boolean castStoneSkin(Enemy target) { if(stoneSkinCooldown>0 || skillPoints<20 || target.stoneSkinHits>0) return false; skillPoints-=20; stoneSkinCooldown=4; target.stoneSkinDefense=(int)Math.ceil(10*(1+2.97*levelCurve)*difficultyMultiplier); target.stoneSkinHits=5; return true; }
    int attackSkill() { if(stoneSweepCooldown==0 && skillPoints>=15){skillPoints-=15;stoneSweepCooldown=6;return 1;} if(stoneSmashCooldown==0 && skillPoints>=10){skillPoints-=10;stoneSmashCooldown=4;return 2;} return 0; }
    /** Returns whether this Sentinel is the attack variant.
     * @return whether this Sentinel is the attack variant
     */
    public boolean isAttackSentinel() { return sentinel && !boss && attackSentinel; }
    /** Returns the remaining hits protected by Stone Skin.
     * @return the remaining hits protected by Stone Skin
     */
    public int getStoneSkinHits() { return stoneSkinHits; }
    int applyTwinDefensePenalty() { twinDefensePenalty = (int)Math.ceil(10 * (1 + 2.97 * levelCurve) * difficultyMultiplier); return twinDefensePenalty; }
    void clearTwinDefensePenalty() { twinDefensePenalty = 0; }
    int healingPower(Enemy target) { return (int)(((long)target.getMaxHp() + 4) / 5); }
    int heal(int amount) {
        if (lootGoblin) return 0;
        int cap = boss ? (int)Math.ceil(200 * (1 + 2.97 * levelCurve) * difficultyMultiplier) : Integer.MAX_VALUE;
        int healed = Math.min(maxHp - hp, Math.max(0, Math.min(cap, amount))); hp += healed; return healed;
    }
    /** Returns whether the next action uses this monster's skill. */
    boolean usesSkillNext() { return (actionsTaken + 1) % (boss ? 4 : 3) == 0; }
    /** Countdown displayed before the Guardian's next fourth-action skill.
     * @return the boss countdown
     */
    public int getBossCountdown() { return boss ? pulseCountdown : 0; }
    /** Records an action only after it is performed. */
    void recordAction() { actionsTaken++; }
    /** Returns the experience earned by each student on victory.
     * @return the experience earned by each student on victory
     */
    public int getExperienceDrop() { return (int) Math.round(experienceDrop * difficultyMultiplier); }
    /** Scales outgoing damage, including monster skills.
     * @param damage damage amount
     * @return the result of scale damage
     */
    public int scaleDamage(int damage) { return Math.max(1, (int) Math.round(damage * difficultyMultiplier * finalDamageScale)); }
    /** Returns the Petot awarded for defeating this enemy.
     * @return the Petot awarded for defeating this enemy
     */
    public int getPetotDrop() { return (int) Math.round(petotDrop * difficultyMultiplier); }
    /** Returns whether this enemy is Itmigims.
     * @return whether this enemy is Itmigims
     */
    public boolean isLootGoblin() { return lootGoblin; }
    /** Returns whether this enemy escaped the battle.
     * @return whether this enemy escaped the battle
     */
    public boolean hasEscaped() { return escaped; }
    /** Returns whether this enemy was defeated rather than escaping.
     * @return whether this enemy was defeated rather than escaping
     */
    public boolean isDefeated() { return hp <= 0 && !escaped; }
    /** Thirteen actions remain from the first successful damaging hit; -1 means untriggered.
     * @return the escape countdown
     */
    public int getEscapeCountdown() { return escapeCountdown; }
    void advanceEscapeCountdown() { if (lootGoblin && escapeCountdown > 0) escapeCountdown--; }
    boolean escape() {
        if (!lootGoblin || !isAlive() || escapeCountdown != 0 || skillPoints < 10) return false;
        skillPoints -= 10; escaped = true; return true;
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
     */ public int getDefense() { return Math.max(-20, defense + (stoneSkinHits>0 ? stoneSkinDefense : 0) - twinDefensePenalty); }
    /** Returns speed stat.
     * @return speed stat
     */ public int getSpeed() { return speed; }
    /** Returns the enemy’s agility.
     * @return the enemy\u2019s agility
     */
    public int getAgility() { return agility + (stoneSkinHits > 0 ? 5 : 0); }
    /** Returns the enemy’s accuracy.
     * @return the enemy\u2019s accuracy
     */
    public int getAccuracy() { return accuracy; }
    /** Returns AP required before this enemy can attack.
     * @return AP required before this enemy can attack
     */ public int getActionThreshold() { return actionThreshold; }
    /** Returns whether this enemy can act.
     * @return whether this enemy can act
     */ public boolean isAlive() { return hp > 0 && !escaped; }
    /** Applies damage and returns the actual HP lost.
     * @param amount incoming damage
     * @return HP lost
     */
    public int takeDamage(int amount) {
        if (escaped) return 0;
        int actual = Math.min(hp, Math.max(0, amount));
        if (actual > 0 && lootGoblin && escapeCountdown < 0) escapeCountdown = 13;
        hp -= actual;
        if (actual > 0 && stoneSkinHits > 0) stoneSkinHits--;
        return actual;
    }
}
