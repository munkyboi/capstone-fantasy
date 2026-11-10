package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls a two-Wisp encounter, independent of the Swing interface. */
public final class Battle {
    private static final int FIRST_AID_HEAL = 30;
    private static final int KISS_HEAL = 28;
    private final List<Student> party = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random;
    private int turn;
    private int kits = 3;
    private String message = "Two Library Wisps appear! Choose an action.";

    /** Creates an encounter for exactly three different students.
     * @param ids selected student indices
     * @param random randomness used in combat
     */
    public Battle(List<Integer> ids, Random random) {
        if (ids.size() != 3 || ids.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Choose three different students");
        }
        this.random = random;
        for (int id : ids) { party.add(new Student(id)); }
        party.sort(Comparator.comparingInt(Student::getSpeed).reversed());
        enemies.add(new Enemy("Library Wisp A"));
        enemies.add(new Enemy("Library Wisp B"));
        enemies.sort(Comparator.comparingInt(Enemy::getSpeed).reversed());
    }
    /** @return party in speed order */ public List<Student> getParty() { return List.copyOf(party); }
    /** @return enemies in speed order */ public List<Enemy> getEnemies() { return List.copyOf(enemies); }
    /** @return current student */ public Student getCurrent() { return party.get(turn); }
    /** @return remaining first-aid kits */ public int getKits() { return kits; }
    /** @return latest combat messages */ public String getMessage() { return message; }
    /** @return whether either side is defeated */ public boolean isOver() { return isVictory() || isDefeat(); }
    /** @return whether all enemies are defeated */ public boolean isVictory() { return enemies.stream().noneMatch(Enemy::isAlive); }
    /** @return whether all students are knocked out */ public boolean isDefeat() { return party.stream().noneMatch(Student::isAlive); }

    /** Attacks a living enemy.
     * @param target chosen enemy
     * @return whether the action was performed
     */
    public boolean attack(Enemy target) {
        if (isOver() || !validEnemy(target)) { return false; }
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
        if (isOver()) { return false; }
        Student actor = getCurrent();
        if (actor.getId() == 3) {
            if (!validAlly(ally)) { return false; }
            int healed = ally.heal(KISS_HEAL);
            finish("Angel uses Healing Kiss. " + ally.getName() + " recovers " + healed + " HP.");
        } else if (actor.getId() == 0) {
            StringBuilder text = new StringBuilder("Zack uses Arcane Burst!");
            for (Enemy target : enemies) {
                if (target.isAlive()) {
                    int damage = hurt(target, actor.getAttack() + 4);
                    text.append("\n").append(target.getName()).append(" takes ").append(damage).append(" damage.").append(defeated(target));
                }
            }
            finish(text.toString());
        } else {
            if (!validEnemy(enemy)) { return false; }
            int damage = hurt(enemy, actor.getAttack() + (actor.getId() == 1 ? 9 : 4));
            if (actor.getId() == 2 && enemy.isAlive()) { damage += hurt(enemy, actor.getAttack()); }
            String ability = actor.getId() == 1 ? "Cadet Strike" : "Twin Shot";
            finish(actor.getName() + " uses " + ability + " on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy));
        }
        return true;
    }
    /** Uses a first-aid kit on a living student.
     * @param target student to heal
     * @return whether an item was used
     */
    public boolean useKit(Student target) {
        if (isOver() || kits == 0 || !validAlly(target)) { return false; }
        kits--; int healed = target.heal(FIRST_AID_HEAL);
        finish(getCurrent().getName() + " uses a First-Aid Kit. " + target.getName()
                + " recovers " + healed + " HP.");
        return true;
    }
    /** Defends the current student. */
    public void defend() { if (isOver()) { return; } Student actor = getCurrent(); actor.defend(); finish(actor.getName() + " defends."); }

    private boolean validAlly(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    private boolean validEnemy(Enemy target) { return target != null && enemies.contains(target) && target.isAlive(); }
    private int hurt(Enemy target, int attack) {
        return target.takeDamage(Math.max(1, attack - target.getDefense() + random.nextInt(5)));
    }
    private String defeated(Enemy target) { return target.isAlive() ? "" : " " + target.getName() + " is defeated."; }
    private void finish(String action) {
        message = action;
        if (isVictory()) { message += "\nAll Wisps defeated! Victory!"; return; }
        turn++;
        while (turn < party.size() && !party.get(turn).isAlive()) { turn++; }
        if (turn < party.size()) { return; }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || isDefeat()) { continue; }
            List<Student> living = party.stream().filter(Student::isAlive).toList();
            Student target = living.get(random.nextInt(living.size()));
            int damage = target.takeDamage(Math.max(1, enemy.getAttack() - target.getDefense() + random.nextInt(5)));
            message += "\n" + enemy.getName() + " attacks " + target.getName() + " for " + damage + " damage.";
            if (!target.isAlive()) { message += " " + target.getName() + " is knocked out."; }
        }
        if (isDefeat()) { message += "\nThe party is defeated."; return; }
        turn = 0;
        while (!party.get(turn).isAlive()) { turn++; }
    }
}
