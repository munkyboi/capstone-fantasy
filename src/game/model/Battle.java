package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls an encounter, independent of the Swing interface. */
public final class Battle {
    private static final int KISS_HEAL = 28;
    private static final int ARCANE_BURST_BONUS = 4;
    private static final int CADET_STRIKE_BONUS = 9;
    private static final int TWIN_SHOT_BONUS = 4;
    private static final int DAMAGE_VARIATION = 5;
    private static final int MINIMUM_DAMAGE = 1;
    private final List<Student> party = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random;
    private final GameProgress progress;
    private final List<BattleEffect> effects = new ArrayList<>();
    private int turn = -1;
    private String message;

    /** Creates an encounter for exactly three different students.
     * @param ids selected student indices
     * @param random randomness used in combat
     */
    public Battle(List<Integer> ids, Random random) {
        this(new GameProgress(ids), random);
    }
    /** Creates a battle using a party's existing state.
     * @param progress party and encounter progress
     * @param random randomness used in combat
     */
    public Battle(GameProgress progress, Random random) {
        this(progress, random, false);
    }

    /** Creates a route encounter, possibly starting with an ambush.
     * @param progress party and encounter progress
     * @param random randomness used in combat
     * @param ambushed whether enemies ambush the party
     */
    public Battle(GameProgress progress, Random random, boolean ambushed) {
        if (progress.getEncounter() > 1) { throw new IllegalArgumentException("This encounter is not available yet"); }
        this.progress = progress;
        this.random = random;
        party.addAll(progress.getParty());
        for (Student student : party) { student.clearDefense(); }
        party.sort(Comparator.comparingInt(Student::getSpeed).reversed());
        if (party.stream().noneMatch(Student::isAlive)) {
            throw new IllegalArgumentException("At least one student must be able to battle");
        }
        enemies.add(new Enemy("Library Wisp A"));
        if (progress.getEncounter() == 0) {
            enemies.add(new Enemy("Library Wisp B"));
            message = "Two Library Wisps appear! Choose an action.";
        } else {
            enemies.add(new Enemy("Stone Sentinel", true));
            message = "A Stone Sentinel and a Library Wisp appear!";
        }
        if (ambushed) { message = "You have been ambushed!\n" + message; }
        enemies.sort(Comparator.comparingInt(Enemy::getSpeed).reversed());
    }
    /** Returns party in speed order.
     * @return party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns enemies in speed order.
     * @return enemies in speed order
     */ public List<Enemy> getEnemies() { return List.copyOf(enemies); }
    /** Returns selected ready student, or null while waiting.
     * @return selected ready student, or null while waiting
     */ public Student getCurrent() { return turn < 0 ? null : party.get(turn); }
    /** Returns remaining first-aid kits.
     * @return remaining first-aid kits
     */ public int getKits() { return progress.getKits(); }
    /** Returns latest combat messages.
     * @return latest combat messages
     */ public String getMessage() { return message; }
    /** Returns HP changes from the most recent action.
     * @return HP changes from the most recent action
     */ public List<BattleEffect> getEffects() { return List.copyOf(effects); }
    /** Selects a living student whose AP is ready.
     * @param student student chosen by the battle clock
     * @return true if selected
     */
    public boolean selectStudent(Student student) {
        int index = party.indexOf(student);
        if (isOver() || getCurrent() != null || index < 0 || !student.isAlive()) { return false; }
        turn = index;
        return true;
    }
    /** Returns whether either side is defeated.
     * @return whether either side is defeated
     */ public boolean isOver() { return isVictory() || isDefeat(); }
    /** Returns whether all enemies are defeated.
     * @return whether all enemies are defeated
     */ public boolean isVictory() { return enemies.stream().noneMatch(Enemy::isAlive); }
    /** Returns whether all students are knocked out.
     * @return whether all students are knocked out
     */ public boolean isDefeat() {
        List<GameCharacter> participants = new ArrayList<>(party);
        return participants.stream().noneMatch(GameCharacter::isAlive);
    }

    /** Attacks a living enemy.
     * @param target chosen enemy
     * @return whether the action was performed
     */
    public boolean attack(Enemy target) {
        if (isOver() || getCurrent() == null || !validEnemy(target)) { return false; }
        effects.clear();
        Student actor = getCurrent();
        int damage = hurt(target, actor.getAttack());
        finish(actor.getName() + " attacks " + target.getName() + " for " + damage + " damage." + defeated(target));
        return true;
    }
    /** Uses the current student's special ability.
     * @param ally healing target for Angel
     * @param enemy enemy target for single-target abilities
     * @return whether the action was performed
     */
    public boolean ability(Student ally, Enemy enemy) {
        if (isOver() || getCurrent() == null) { return false; }
        Student actor = getCurrent();
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS) {
            if (!validAlly(ally)) { return false; }
            effects.clear();
            int healed = ally.heal(KISS_HEAL);
            if (healed > 0) { effects.add(new BattleEffect(ally, healed)); }
            finish("Angel uses Healing Kiss. " + ally.getName() + " recovers " + healed + " HP.");
        } else if (actor.getAbilityType() == Student.AbilityType.ARCANE_BURST) {
            effects.clear();
            StringBuilder text = new StringBuilder("Zack uses Arcane Burst!");
            for (Enemy target : enemies) {
                if (target.isAlive()) {
                    int damage = hurt(target, actor.getAttack() + ARCANE_BURST_BONUS);
                    text.append("\n").append(target.getName()).append(" takes ").append(damage).append(" damage.").append(defeated(target));
                }
            }
            finish(text.toString());
        } else {
            if (!validEnemy(enemy)) { return false; }
            effects.clear();
            int damage = hurt(enemy, actor.getAttack() + (actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? CADET_STRIKE_BONUS : TWIN_SHOT_BONUS));
            if (actor.getAbilityType() == Student.AbilityType.TWIN_SHOT && enemy.isAlive()) { damage += hurt(enemy, actor.getAttack()); }
            String ability = actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? "Cadet Strike" : "Twin Shot";
            finish(actor.getName() + " uses " + ability + " on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy));
        }
        return true;
    }
    /** Uses a first-aid kit on a living student.
     * @param target student to heal
     * @return whether an item was used
     */
    public boolean useKit(Student target) {
        if (isOver() || getCurrent() == null || !validAlly(target)) { return false; }
        int before = target.getHp();
        if (!progress.useKit(target)) { return false; }
        effects.clear();
        int healed = target.getHp() - before;
        effects.add(new BattleEffect(target, healed));
        finish(getCurrent().getName() + " uses a First-Aid Kit. " + target.getName()
                + " recovers " + healed + " HP.");
        return true;
    }
    /** Defends the current student. */
    public void defend() { if (isOver() || getCurrent() == null) { return; } effects.clear(); Student actor = getCurrent(); actor.defend(); finish(actor.getName() + " defends."); }

    /** Lets a ready living enemy attack a random living student.
     * @param enemy ready enemy
     * @return whether the enemy acted
     */
    public boolean enemyAttack(Enemy enemy) {
        if (isOver() || getCurrent() != null || !validEnemy(enemy)) { return false; }
        effects.clear();
        List<Student> living = party.stream().filter(Student::isAlive).toList();
        Student target = living.get(random.nextInt(living.size()));
        int damage = target.takeDamage(Math.max(MINIMUM_DAMAGE, enemy.getAttack() - target.getDefense() + random.nextInt(DAMAGE_VARIATION)));
        effects.add(new BattleEffect(target, -damage));
        message = enemy.getName() + " attacks " + target.getName() + " for " + damage + " damage.";
        if (!target.isAlive()) { message += " " + target.getName() + " is knocked out."; }
        if (isDefeat()) { message += "\nThe party is defeated."; }
        return true;
    }

    private boolean validAlly(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    private boolean validEnemy(Enemy target) { return target != null && enemies.contains(target) && target.isAlive(); }
    private int hurt(Enemy target, int attack) {
        int damage = target.takeDamage(Math.max(MINIMUM_DAMAGE, attack - target.getDefense() + random.nextInt(DAMAGE_VARIATION)));
        effects.add(new BattleEffect(target, -damage));
        return damage;
    }
    private String defeated(Enemy target) { return target.isAlive() ? "" : " " + target.getName() + " is defeated."; }
    private void finish(String action) {
        message = action;
        if (isVictory()) { message += "\nAll enemies defeated! Victory!"; return; }
        turn = -1;
    }
}
