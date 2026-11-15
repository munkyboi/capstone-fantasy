package game.model;

import java.util.HashMap;
import java.util.Map;

/** Tracks Focus Charm misses and the damage penalty on the next offensive action. */
final class FocusCharmMechanic {
    private final Map<Student, Integer> penalties = new HashMap<>();
    private Student actor;
    private int activePenalty, attempts, misses;

    void begin(Student student) {
        actor = "focus_charm".equals(student.getEquipmentId(Equipment.Slot.ACCESSORY)) ? student : null;
        activePenalty = actor == null ? 0 : penalties.getOrDefault(actor, 0);
        if (actor != null) penalties.remove(actor);
        attempts = 0; misses = 0;
    }
    void hit(boolean landed) { if (actor != null) { attempts++; if (!landed) misses++; } }
    int reduce(int damage) {
        if (actor == null || activePenalty == 0) return damage;
        return (int) Math.ceil(damage * (1.0 - 0.30 * activePenalty) - 1e-9);
    }
    Student complete() {
        Student result = null;
        if (actor != null && actor.isAlive() && attempts > 0 && misses * 2 >= attempts) {
            penalties.put(actor, Math.min(3, activePenalty + 1)); result = actor;
        }
        actor = null; activePenalty = 0; attempts = 0; misses = 0;
        return result;
    }
    int stacks(Student student) { return penalties.getOrDefault(student, 0); }
}
