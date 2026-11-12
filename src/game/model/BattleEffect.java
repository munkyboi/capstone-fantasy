package game.model;

/** A change in HP shown briefly over a battle participant. */
public final class BattleEffect {
    private final Student student;
    private final Enemy enemy;
    private final int amount;
    private final String label;
    private final boolean manaLeak;
    /** Creates an HP change for a student.
     * @param student affected student
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Student student, int amount) { this.student = student; this.enemy = null; this.amount = amount; this.label = null; this.manaLeak = false; }
    /** Creates an HP change for an enemy.
     * @param enemy affected enemy
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Enemy enemy, int amount) { this.student = null; this.enemy = enemy; this.amount = amount; this.label = null; this.manaLeak = false; }
    private BattleEffect(Student student, String label) { this.student = student; this.enemy = null; this.amount = 0; this.label = label; this.manaLeak = false; }
    private BattleEffect(Enemy enemy, String label) { this.student = null; this.enemy = enemy; this.amount = 0; this.label = label; this.manaLeak = false; }
    private BattleEffect(Enemy enemy, int amount, boolean manaLeak) { this.student = null; this.enemy = enemy; this.amount = amount; this.label = null; this.manaLeak = manaLeak; }
    /** Creates a distinct SP-loss indicator for Mana Leak. */
    public static BattleEffect manaLeak(Enemy enemy, int amount) { return new BattleEffect(enemy, -amount, true); }
    public boolean isManaLeak() { return manaLeak; }
    /** Creates a textual combat indicator for a monster. */
    public static BattleEffect status(Enemy enemy, String label) { return new BattleEffect(enemy, label); }
    /** Creates a textual combat indicator for a character. */
    public static BattleEffect status(Student student, String label) { return new BattleEffect(student, label); }
    public String getLabel() { return label; }
    /** Returns affected student, or null.
     * @return affected student, or null
     */ public Student getStudent() { return student; }
    /** Returns affected enemy, or null.
     * @return affected enemy, or null
     */ public Enemy getEnemy() { return enemy; }
    /** Returns signed HP change.
     * @return signed HP change
     */ public int getAmount() { return amount; }
}
