package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls the first encounter, independent of the Swing interface. */
public final class Battle {
    private static final int WISP_MAX_HP = 125;
    private static final int WISP_ATTACK = 24;
    private static final int WISP_DEFENSE = 5;
    private static final int FIRST_AID_HEAL = 30;
    private static final int KISS_HEAL = 28;
    private final List<Student> party = new ArrayList<>();
    private final Random random;
    private int wispHp = WISP_MAX_HP;
    private int turn;
    private int kits = 3;
    private String message = "A Library Wisp appears! Choose an action.";

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
    }
    /** @return party in speed order */ public List<Student> getParty() { return List.copyOf(party); }
    /** @return current student */ public Student getCurrent() { return party.get(turn); }
    /** @return remaining wisp HP */ public int getWispHp() { return wispHp; }
    /** @return maximum wisp HP */ public int getWispMaxHp() { return WISP_MAX_HP; }
    /** @return remaining first-aid kits */ public int getKits() { return kits; }
    /** @return latest combat messages */ public String getMessage() { return message; }
    /** @return whether either side is defeated */ public boolean isOver() { return isVictory() || isDefeat(); }
    /** @return whether the wisp is defeated */ public boolean isVictory() { return wispHp == 0; }
    /** @return whether all students are knocked out */ public boolean isDefeat() { return party.stream().noneMatch(Student::isAlive); }

    /** Performs a normal attack, including Angel's attack. */
    public void attack() { if (isOver()) { return; } Student actor = getCurrent();
        int damage = hurtWisp(actor.getAttack()); finish(actor.getName() + " attacks for " + damage + " damage."); }
    /** Uses the current student's special ability.
     * @param target healing target for Angel; ignored for other students
     * @return true if the action was performed
     */
    public boolean ability(Student target) {
        if (isOver()) { return false; }
        Student actor = getCurrent();
        if (actor.getId() == 3) {
            if (!validTarget(target)) { return false; }
            int healed = target.heal(KISS_HEAL);
            finish("Angel uses Healing Kiss. " + target.getName() + " recovers " + healed + " HP.");
        } else {
            int damage = hurtWisp(actor.getAttack() + (actor.getId() == 1 ? 9 : 4));
            if (actor.getId() == 2 && wispHp > 0) { damage += hurtWisp(actor.getAttack()); }
            String[] names = {"Arcane Burst", "Cadet Strike", "Twin Shot"};
            finish(actor.getName() + " uses " + names[actor.getId()] + " for " + damage + " damage.");
        }
        return true;
    }
    /** Uses a first-aid kit on a living student.
     * @param target student to heal
     * @return true if an item was used
     */
    public boolean useKit(Student target) {
        if (isOver() || kits == 0 || !validTarget(target)) { return false; }
        kits--; int healed = target.heal(FIRST_AID_HEAL);
        finish(getCurrent().getName() + " uses a First-Aid Kit. " + target.getName()
                + " recovers " + healed + " HP.");
        return true;
    }
    /** Defends the current student. */
    public void defend() { if (isOver()) { return; } Student actor = getCurrent(); actor.defend(); finish(actor.getName() + " defends."); }

    private boolean validTarget(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    private int hurtWisp(int attack) {
        int damage = Math.max(1, attack - WISP_DEFENSE + random.nextInt(5));
        int actual = Math.min(wispHp, damage); wispHp -= actual; return actual;
    }
    private void finish(String action) {
        message = action;
        if (isVictory()) { message += "\nThe Library Wisp is defeated! Victory!"; return; }
        turn++;
        while (turn < party.size() && !party.get(turn).isAlive()) { turn++; }
        if (turn < party.size()) { return; }
        List<Student> living = party.stream().filter(Student::isAlive).toList();
        Student target = living.get(random.nextInt(living.size()));
        int damage = target.takeDamage(Math.max(1, WISP_ATTACK - target.getDefense() + random.nextInt(5)));
        message += "\nLibrary Wisp attacks " + target.getName() + " for " + damage + " damage.";
        if (!target.isAlive()) { message += " " + target.getName() + " is knocked out."; }
        if (isDefeat()) { message += "\nThe party is defeated."; return; }
        turn = 0;
        while (!party.get(turn).isAlive()) { turn++; }
    }
}
