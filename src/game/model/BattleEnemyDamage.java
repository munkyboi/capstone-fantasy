package game.model;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Applies enemy hits, protection and defensive equipment effects. */
final class BattleEnemyDamage {
    private static final int DAMAGE_VARIATION = 5;
    private final Battle battle;
    private final List<Student> party;
    private final Map<Student, Student> guards;
    private final Random random;
    private final List<BattleEffect> effects;
    private final Set<Student> apRefills;
    private Student blockingStudent, protectedStudent, lastDamageRecipient;
    BattleEnemyDamage(Battle battle, List<Student> party, Map<Student, Student> guards, Random random,
                      List<BattleEffect> effects, Set<Student> apRefills) {
        this.battle = battle; this.party = party; this.guards = guards; this.random = random; this.effects = effects; this.apRefills = apRefills;
    }
    Student blockingStudent() { return blockingStudent; }
    Student protectedStudent() { return protectedStudent; }
    Student lastDamageRecipient() { return lastDamageRecipient; }
    int resolve(Enemy enemy, Student target, int bonus, int multiplier, boolean guaranteedHit, boolean ignoreDefense) {
        blockingStudent = null; protectedStudent = null;
        Student intended = target;
        Student protector = party.stream().filter(candidate -> candidate != intended && candidate.isDefending()
                && candidate.isAlive() && battle.getStunTurns(candidate) == 0 && guards.get(candidate) == intended).findFirst().orElse(null);
        if (protector == null) {
            Student ringBearer = party.stream().filter(candidate -> candidate != intended && candidate.isAlive() && battle.getStunTurns(candidate) == 0
                    && "heroes_ring".equals(candidate.getEquipmentId(Equipment.Slot.ACCESSORY))).findFirst().orElse(null);
            if (ringBearer != null && random.nextDouble() < 0.5) protector = ringBearer;
        }
        if (protector != null) {
            target = protector; blockingStudent = protector; protectedStudent = intended;
            effects.add(BattleEffect.status(protector, "Guard"));
        }
        lastDamageRecipient = target;
        if (!guaranteedHit && !battle.hits(enemy, target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        String armor = target.getEquipmentId(Equipment.Slot.ARMOR);
        if ("ultra_instinct_shirt".equals(armor) && random.nextDouble() < 0.5) {
            effects.add(BattleEffect.status(target, "Dodge"));
            return 0;
        }
        int power = Math.max(0, enemy.getAttack() + bonus);
        int adjusted = Battle.damageAfterDefense(power, ignoreDefense ? 0 : target.getDefense());
        double regenerationRate = "blade_mail".equals(armor) ? HealthRegeneration.rate(target) : 0;
        int incoming = enemy.scaleDamage(adjusted + (guaranteedHit ? 0 : random.nextInt(DAMAGE_VARIATION))) * multiplier;
        String accessory = target.getEquipmentId(Equipment.Slot.ACCESSORY);
        if ("guardian_band".equals(accessory) && random.nextDouble() < 0.15) {
            incoming = Math.max(1, (int)Math.ceil(incoming * 0.75));
            effects.add(BattleEffect.status(target, "Ward"));
        }
        int damage = target.takeDamage(incoming);
        effects.add(new BattleEffect(target, -damage));
        if (!target.isAlive()) { battle.clearKnockedOutHeroEffects(target); }
        if (damage > 0 && target.isAlive() && "courage_charm".equals(accessory)
                && target.getHp() * 2L <= target.getMaxHp() && random.nextDouble() < 0.20) {
            int healed = target.heal((target.getMaxHp() + 19) / 20);
            if (healed > 0) { effects.add(BattleEffect.status(target, "Courage")); effects.add(new BattleEffect(target, healed)); }
        }
        if (damage > 0 && "blade_mail".equals(armor)) {
            effects.add(BattleEffect.status(target, "Reflect"));
            int reflectedPower = (int)Math.min(Integer.MAX_VALUE, Math.ceil(damage * 0.50 * (1.0 + regenerationRate) - 1e-9));
            int reflected = enemy.takeDamage(reflectedPower);
            if (reflected > 0) { effects.add(new BattleEffect(enemy, -reflected)); battle.applyManaLeak(Battle.hasManaLeak(target), enemy, reflected); }
        }
        if (damage > 0 && target.isAlive() && "ethereal_robe".equals(armor)) target.chargeEthereal();
        if (damage > 0 && target.isAlive() && "ring_of_power".equals(target.getEquipmentId(Equipment.Slot.ACCESSORY))
                && random.nextDouble() < 0.5) { apRefills.add(target); }
        return damage;
    }

}
