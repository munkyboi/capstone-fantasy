package game.model;

/** A change in HP shown briefly over a battle participant. */
public final class BattleEffect {
    private final Student student;
    private final Enemy enemy;
    private final int amount;
    /** Creates an HP change for a student.
     * @param student affected student
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Student student, int amount) { this.student = student; this.enemy = null; this.amount = amount; }
    /** Creates an HP change for an enemy.
     * @param enemy affected enemy
     * @param amount positive for healing, negative for damage
     */
    public BattleEffect(Enemy enemy, int amount) { this.student = null; this.enemy = enemy; this.amount = amount; }
    /** @return affected student, or null */ public Student getStudent() { return student; }
    /** @return affected enemy, or null */ public Enemy getEnemy() { return enemy; }
    /** @return signed HP change */ public int getAmount() { return amount; }
}
