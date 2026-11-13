package game.model;

import java.util.List;
import java.util.Map;
import java.util.Random;

/** Resolves delayed enemy attacks after their initial action. */
final class BattleEnemyFollowups {
    private BattleEnemyFollowups() {}
    record PulseResult(String message, int loops, Enemy caster) {}
    static PulseResult pulse(Battle battle, List<Student> party, List<BattleEffect> effects, Random random,
                             Map<Student, Integer> initial, Map<Student, Integer> previous, Enemy caster, int loops) {
        effects.clear(); StringBuilder text = new StringBuilder("Crystal Pulse echoes!");
        for (Student hero : party) {
            int base = initial.getOrDefault(hero, 0);
            if (!hero.isAlive() || base <= 0) continue;
            int amount = (int)Math.min(Integer.MAX_VALUE, ((long)base * 5 + (long)previous.getOrDefault(hero, 0) * 2 + 9) / 10);
            int damage = hero.takeDamage(amount); previous.put(hero, damage);
            if (damage > 0) effects.add(new BattleEffect(hero, -damage));
            if (!hero.isAlive()) battle.clearKnockedOutHeroEffects(hero);
            text.append("\n").append(hero.getName()).append(" takes ").append(damage).append(" damage.");
        }
        loops--;
        if (battle.isOver() || loops == 0 || random.nextDouble() >= Battle.GUARDIAN_PULSE_LOOP_CHANCE) {
            loops = 0; caster.finishPulse(); caster = null;
        }
        return new PulseResult(text.toString(), loops, caster);
    }
    static String spark(Battle battle, List<Student> party, List<BattleEffect> effects, Enemy caster, Student primary) {
        effects.clear(); StringBuilder text = new StringBuilder("Arcane Spark explodes!");
        for (Student target : party) if (target.isAlive()) {
            int power = caster.getAttack() * (target == primary ? 2 : 1);
            int damage = battle.enemyDamage(caster, target, power - caster.getAttack(), 1);
            text.append("\n").append(battle.lastDamageRecipient().getName()).append(" takes ").append(damage).append(" damage.");
        }
        if (battle.isDefeat()) { text.append("\nThe party is defeated."); battle.clearGuards(); }
        else if (battle.isVictory()) { text.append("\nAll enemies defeated! Victory!"); battle.clearGuards(); }
        return text.toString();
    }
}
