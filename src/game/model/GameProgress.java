package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Party and shared supplies carried between encounters. */
public final class GameProgress {
    private static final int STARTING_KITS = 3;
    private static final int KIT_HEAL = 30;
    private final List<Student> party = new ArrayList<>();
    private int kits = STARTING_KITS;
    private int encounter;

    /** Creates a new party of three distinct students.
     * @param ids selected character indices
     */
    public GameProgress(List<Integer> ids) {
        if (ids.size() != 3 || ids.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Choose three different students");
        }
        for (int id : ids) { party.add(new Student(id)); }
        party.sort(Comparator.comparingInt(Student::getSpeed).reversed());
    }
    /** @return the current party in speed order */ public List<Student> getParty() { return List.copyOf(party); }
    /** @return first-aid kits remaining */ public int getKits() { return kits; }
    /** @return zero-based current encounter */ public int getEncounter() { return encounter; }
    /** Advances from the first encounter to the second.
     * @return whether another encounter was available
     */
    public boolean advance() { if (encounter >= 1) { return false; } encounter++; return true; }
    /** Uses a kit on a living, injured member.
     * @param target the student to heal
     * @return true if a kit was consumed
     */
    public boolean useKit(Student target) {
        if (kits == 0 || !party.contains(target) || !target.isAlive() || target.getHp() == target.getMaxHp()) { return false; }
        target.heal(KIT_HEAL); kits--; return true;
    }
}
