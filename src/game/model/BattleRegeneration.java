package game.model;

import java.util.List;
import java.util.Random;

/** Resolves HP and accessory SP regeneration at action-bar completion. */
final class BattleRegeneration {
    private BattleRegeneration() {}
    record Result(boolean restored, String message) {}
    static Result attempt(List<Student> students, List<Student> party, Random random,
                          List<BattleEffect> effects, boolean keepCurrentEffects) {
        boolean restored = false;
        StringBuilder text = new StringBuilder();
        for (Student student : students) {
            if (!party.contains(student)) throw new IllegalArgumentException("Unknown party member");
            int healed = HealthRegeneration.attempt(student, random);
            if (healed > 0) {
                if (!restored && !keepCurrentEffects) effects.clear();
                effects.add(new BattleEffect(student, healed));
                if (text.length() > 0) text.append("  ");
                text.append(student.getName()).append(" regenerated ").append(healed).append(" HP.");
                restored = true;
            }
            String accessory = student.getEquipmentId(Equipment.Slot.ACCESSORY);
            int spRegen = accessory == null ? 0 : Equipment.fromId(accessory).getSpRegenPercent();
            if (student.isAlive() && student.getSp() < student.getMaxSp() && spRegen > 0 && random.nextDouble() < 0.25) {
                int before = student.getSp();
                int recovery = (int)(((long)student.getMaxSp() * spRegen + 99) / 100);
                student.restoreSp((int)Math.min(student.getMaxSp(), (long)before + recovery));
                int recovered = student.getSp() - before;
                if (!restored && !keepCurrentEffects) effects.clear();
                effects.add(BattleEffect.status(student, "+" + recovered + " SP"));
                if (text.length() > 0) text.append("  ");
                text.append(student.getName()).append(" recovered ").append(recovered).append(" SP.");
                restored = true;
            }
        }
        return new Result(restored, text.toString());
    }
}
