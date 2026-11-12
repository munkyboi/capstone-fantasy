package game.model;

import java.util.Random;

/** Shared HP-regeneration rules for battle and camp. */
public final class HealthRegeneration {
    private static final double CHANCE = 0.25;
    private static final double[] BASE_RATES = {0.05, 0.08, 0.06, 0.05};
    private HealthRegeneration() { }

    /** Returns the amount restored by a successful attempt, before the HP cap. */
    public static int amount(Student student) {
        double bonus = switch (java.util.Objects.toString(student.getEquipmentId(Equipment.Slot.ARMOR), "")) {
            case "ethereal_robe", "designer_leather_vest" -> 0.10;
            case "blade_mail" -> 0.50;
            case "ultra_instinct_shirt" -> 0.30;
            default -> 0.0;
        };
        if ("cadet_blade".equals(student.getEquipmentId(Equipment.Slot.WEAPON))) bonus += 0.05;
        if (student.isDefending()) bonus += 0.20;
        int basis = student.getMaxHp();
        return Math.max(1, (int) Math.round(basis * (BASE_RATES[student.getId()] + bonus)));
    }

    /** Rolls once for a living injured character and returns the actual HP restored. */
    public static int attempt(Student student, Random random) {
        java.util.Objects.requireNonNull(student); java.util.Objects.requireNonNull(random);
        if (!student.isAlive() || student.getHp() >= student.getMaxHp() || random.nextDouble() >= CHANCE) return 0;
        return student.heal(amount(student));
    }
}
