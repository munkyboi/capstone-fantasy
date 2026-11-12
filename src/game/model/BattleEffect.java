package game.model;

/** A change in HP shown briefly over a battle participant. */
public final class BattleEffect {
    private final Student student;
    private final Enemy enemy;
    private final int amount;
    private final String label;
    /** Creates an HP change for a student.
     * @param student affected student
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Student student, int amount) { this.student = student; this.enemy = null; this.amount = amount; this.label = null; }
    /** Creates an HP change for an enemy.
     * @param enemy affected enemy
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Enemy enemy, int amount) { this.student = null; this.enemy = enemy; this.amount = amount; this.label = null; }
    private BattleEffect(Student student, String label) { this.student = student; this.enemy = null; this.amount = 0; this.label = label; }
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
