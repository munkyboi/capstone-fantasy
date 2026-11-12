package game.model;

/** A catalogued weapon, armor or accessory that can be equipped once. */
public final class Equipment extends Item {
    private static final long serialVersionUID = 1L;
    /** Supported equipment slots. */
    public enum Slot { /** Class weapon. */ WEAPON, /** Shared defensive gear. */ ARMOR, /** Shared charm. */ ACCESSORY }
    /** Defense provided by the original campus vest. */
    public static final int DEFENSE_BONUS = 4;
    private final String id;
    private final Slot slot;
    private final int attackBonus;
    private final int defenseBonus;
    private final int allowedStudent;

    /** Creates the original campus vest. */
    public Equipment() { this("campus_vest", "Campus Vest", Slot.ARMOR, 0, DEFENSE_BONUS, -1); }
    private Equipment(String id, String name, Slot slot, int attack, int defense, int allowedStudent) {
        super(name); this.id = id; this.slot = slot; this.attackBonus = attack;
        this.defenseBonus = defense; this.allowedStudent = allowedStudent;
    }
    /** Looks up an equipment item by its stable save ID.
     * @param id equipment ID
     * @return equipment definition
     */
    public static Equipment fromId(String id) {
        if (id == null) { throw new IllegalArgumentException("Missing equipment"); }
        return switch (id) {
            case "student_staff" -> new Equipment(id, "Student Staff", Slot.WEAPON, 0, 0, Student.ZACK);
            case "cadet_sword" -> new Equipment(id, "Cadet Sword", Slot.WEAPON, 0, 0, Student.BOGART);
            case "hand_crossbows" -> new Equipment(id, "Hand Crossbows", Slot.WEAPON, 0, 0, Student.CHARICE);
            case "monk_wraps" -> new Equipment(id, "Monk Wraps", Slot.WEAPON, 0, 0, Student.ANGEL);
            case "scholar_staff" -> new Equipment(id, "Scholar Staff", Slot.WEAPON, 3, 0, Student.ZACK);
            case "cadet_blade" -> new Equipment(id, "Cadet Blade", Slot.WEAPON, 3, 0, Student.BOGART);
            case "hunter_crossbows" -> new Equipment(id, "Hunter Crossbows", Slot.WEAPON, 3, 0, Student.CHARICE);
            case "champion_wraps" -> new Equipment(id, "Champion Wraps", Slot.WEAPON, 3, 0, Student.ANGEL);
            case "campus_vest" -> new Equipment();
            case "focus_charm" -> new Equipment(id, "Focus Charm", Slot.ACCESSORY, 2, 0, -1);
            case "training_uniform" -> new Equipment(id, "Training Uniform", Slot.ARMOR, 0, 6, -1);
            case "market_charm" -> new Equipment(id, "Market Charm", Slot.ACCESSORY, 1, 1, -1);
            case "sentinel_band" -> new Equipment(id, "Sentinel Band", Slot.ACCESSORY, 0, 2, -1);
            case "guardian_band" -> new Equipment(id, "Guardian Band", Slot.ACCESSORY, 0, 3, -1);
            case "courage_charm" -> new Equipment(id, "Courage Charm", Slot.ACCESSORY, 3, 1, -1);
            case "heroes_ring" -> new Equipment(id, "Hero’s Ring", Slot.ACCESSORY, 0, 0, -1);
            case "looper_ring" -> new Equipment(id, "Looper Ring", Slot.ACCESSORY, 0, 0, -1);
            case "ring_of_power" -> new Equipment(id, "Ring of Power", Slot.ACCESSORY, 0, 0, -1);
            case "light_saber" -> new Equipment(id, "Light Saber", Slot.WEAPON, 100, 0, Student.BOGART);
            case "demon_hunter" -> new Equipment(id, "Demon Hunter", Slot.WEAPON, 0, 0, Student.CHARICE);
            case "great_sage_staff" -> new Equipment(id, "Great Sage Staff", Slot.WEAPON, 0, 0, Student.ZACK);
            case "giant_slayer_gloves" -> new Equipment(id, "Giant Slayer Gloves", Slot.WEAPON, 0, 0, Student.ANGEL);
            case "blade_mail" -> new Equipment(id, "Blade Mail", Slot.ARMOR, 0, 0, -1);
            case "ultra_instinct_shirt" -> new Equipment(id, "Ultra Instinct Shirt", Slot.ARMOR, 0, 0, -1);
            case "designer_leather_vest" -> new Equipment(id, "Designer Leather Vest", Slot.ARMOR, 0, 0, -1);
            case "ethereal_robe" -> new Equipment(id, "Ethereal Robe", Slot.ARMOR, 0, 0, -1);
            case "campus_medal" -> new Equipment(id, "Campus Medal", Slot.ACCESSORY, 5, 3, -1);
            default -> throw new IllegalArgumentException("Unknown equipment: " + id);
        };
    }
    /** Returns the starter weapon ID for a class.
     * @param studentId character ID
     * @return starter weapon ID
     */
    public static String starterWeapon(int studentId) {
        return switch (studentId) {
            case Student.ZACK -> "student_staff";
            case Student.BOGART -> "cadet_sword";
            case Student.CHARICE -> "hand_crossbows";
            case Student.ANGEL -> "monk_wraps";
            default -> throw new IllegalArgumentException("Unknown student");
        };
    }
    /** Returns the first-stage weapon upgrade for a class.
     * @param studentId character ID
     * @return compatible weapon ID
     */
    public static String upgradedWeapon(int studentId) {
        return switch (studentId) {
            case Student.ZACK -> "scholar_staff";
            case Student.BOGART -> "cadet_blade";
            case Student.CHARICE -> "hunter_crossbows";
            case Student.ANGEL -> "champion_wraps";
            default -> throw new IllegalArgumentException("Unknown student");
        };
    }
    @Override public String getCategory() { return "Equipment"; }
    /** Returns the persistent ID.
     * @return equipment ID
     */
    public String getId() { return id; }
    /** Returns the slot.
     * @return equipment slot
     */
    public Slot getSlot() { return slot; }
    /** Returns the attack bonus.
     * @return attack bonus
     */
    public int getAttackBonus() { return attackBonus; }
    /** Returns the defense bonus.
     * @return defense bonus
     */
    public int getDefenseBonus() { return defenseBonus; }
    /** Describes special legendary effects in the equipment screen. */
    public String getSpecialDescription() {
        return switch (id) {
            case "campus_vest" -> "+5% HP; +5% HP regeneration";
            case "training_uniform" -> "+5% ACC; +5% AGI";
            case "sentinel_band" -> "+50 HP; +8% HP regeneration";
            case "focus_charm", "scholar_staff" -> "+5% ACC";
            case "hunter_crossbows" -> "+5% AGI";
            case "cadet_blade" -> "+5% HP regeneration";
            case "champion_wraps" -> "+5% SPD";
            case "heroes_ring" -> "+10% HP/DEF; +10% regen; -30% SPD; 50% auto defend";
            case "looper_ring" -> "+100% SP; +10% ability cost; -20% DEF; +10% SPD; 50% chance of 5 random hits";
            case "ring_of_power" -> "+50% ATK/DEF/SPD; 50% AP refill when hit";
            case "light_saber" -> "+20% SPD; 100% hit damage over 5 turns";
            case "demon_hunter" -> "+20% ATK; +75% SPD";
            case "great_sage_staff" -> "+20% ATK; 50% chance to cast twice";
            case "giant_slayer_gloves" -> "+50% ATK; +15 SPD; extra basic hit";
            case "blade_mail" -> "-50% DEF, -20% SPD; +200% HP; 50% reflect; +50% regen";
            case "ultra_instinct_shirt" -> "-20% DEF; +20% SPD; 50% dodge; +30% regen";
            case "designer_leather_vest" -> "+10% DEF; +20% SPD; +50 HP; 2x XP; +10% regen";
            case "ethereal_robe" -> "+15% DEF; +100 HP; +10% regen; damage empowers next attack";
            default -> "";
        };
    }
    /** Checks whether a student can equip the item.
     * @param studentId character ID
     * @return true if compatible
     */
    public boolean canEquip(int studentId) { return allowedStudent == -1 || allowedStudent == studentId; }
}
