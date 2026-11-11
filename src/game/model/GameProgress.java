package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Party and shared supplies carried between encounters. */
public final class GameProgress implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
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
    /** Unlocks the next route location after an encounter.
     * @return whether the route position advanced
     */
    public boolean advance() { if (encounter >= 2) { return false; } encounter++; return true; }
    /** Reconstructs and validates a saved camp state.
     * @param ids student indices
     * @param hitPoints corresponding current HP values
     * @param kits remaining first-aid kits
     * @param encounter next encounter index
     * @return reconstructed progress
     */
    public static GameProgress restore(List<Integer> ids, List<Integer> hitPoints, int kits, int encounter) {
        if (ids == null || hitPoints == null || ids.size() != 3 || hitPoints.size() != 3
                || kits < 0 || kits > STARTING_KITS || encounter < 1 || encounter > 2) {
            throw new IllegalArgumentException("Invalid saved progress");
        }
        GameProgress result = new GameProgress(ids);
        for (int i = 0; i < ids.size(); i++) {
            final int id = ids.get(i);
            Student student = result.party.stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
            student.restoreHp(hitPoints.get(i));
        }
        if (result.party.stream().noneMatch(Student::isAlive)) { throw new IllegalArgumentException("Party is defeated"); }
        result.kits = kits;
        result.encounter = encounter;
        return result;
    }
    /** Uses a kit on a living, injured member.
     * @param target the student to heal
     * @return true if a kit was consumed
     */
    public boolean useKit(Student target) {
        if (kits == 0 || !party.contains(target) || !target.isAlive() || target.getHp() == target.getMaxHp()) { return false; }
        target.heal(KIT_HEAL); kits--; return true;
    }
}
