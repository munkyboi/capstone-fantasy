package game.model;

import java.util.List;
import java.util.Random;

/** Tracks damage-over-time effects and advances their individual clocks. */
final class BattleDotEngine {
    private final Battle battle;
    private final List<BattleEffect> effects;
    BattleDotEngine(Battle battle) { this.battle = battle; this.effects = battle.dotEffects(); }
    private boolean isOver() { return battle.isOver(); }
    private boolean isVictory() { return battle.isVictory(); }
    private boolean isDefeat() { return battle.isDefeat(); }
    private final java.util.List<DamageOverTime> burns = new java.util.ArrayList<>();
    private static final class DamageOverTime {
        private final GameCharacter source, target;
        private final int total, duration, spRecovery;
        private final boolean manaLeak;
        private int ticks;
        private double elapsedSeconds;
        DamageOverTime(GameCharacter source, GameCharacter target, int damage) { this(source, target, damage, false); }
        DamageOverTime(GameCharacter source, GameCharacter target, int damage, boolean manaLeak) { this(source, target, damage, manaLeak, 5); }
        DamageOverTime(GameCharacter source, GameCharacter target, int damage, boolean manaLeak, int duration) { this(source, target, damage, manaLeak, duration, 0); }
        DamageOverTime(GameCharacter source, GameCharacter target, int damage, boolean manaLeak, int duration, int spRecovery) { this.source = source; this.target = target; total = damage; this.manaLeak = manaLeak; this.duration = duration; this.spRecovery = spRecovery; }
    }
    boolean has(GameCharacter target) { return target != null && target.isAlive() && !isOver() && burns.stream().anyMatch(b -> b.target == target && b.ticks < b.duration); }
    void remove(GameCharacter target) { burns.removeIf(b -> b.target == target); }
    void removeMirrors() { burns.removeIf(b -> b.target instanceof Enemy e && e.isMirror()); }
    void add(GameCharacter source, GameCharacter target, int damage, boolean manaLeak, int duration, int recovery) { burns.add(new DamageOverTime(source, target, damage, manaLeak, duration, recovery)); }
    void replaceRecovery(Student source, Enemy target, int damage, boolean manaLeak) { burns.removeIf(b -> b.source == source && b.target == target && b.spRecovery > 0); add(source, target, damage, manaLeak, 5, 5); }
    boolean tick(GameCharacter target) { return tickDamageOverTime(target, null); }
    private boolean tickDamageOverTime(GameCharacter participant, DamageOverTime only) {
        if (participant == null || isOver() || !participant.isAlive() || burns.isEmpty()) return false;
        boolean changed = false, damaged = false;
        for (DamageOverTime burn : new java.util.ArrayList<>(burns)) {
            if (!burns.contains(burn) || (only != null && burn != only)) continue;
            GameCharacter target = burn.target;
            if (target != participant || !target.isAlive() || burn.ticks >= burn.duration) continue;
                int before = (int) ((long) burn.total * burn.ticks / burn.duration);
                burn.ticks++;
                int damage = (int) ((long) burn.total * burn.ticks / burn.duration) - before;
                if (damage > 0 && (!battle.hits(burn.source, target)
                        || (target instanceof Student student && "ultra_instinct_shirt".equals(student.getEquipmentId(Equipment.Slot.ARMOR)) && battle.dotRandom().nextDouble() < 0.5))) {
                    if (!changed) effects.clear();
                    if (target instanceof Enemy enemy) effects.add(BattleEffect.status(enemy, "Dodge"));
                    else if (target instanceof Student student) effects.add(BattleEffect.status(student, "Dodge"));
                    changed = true;
                    continue;
                }
                boolean shielded = target instanceof Enemy enemy && enemy.getStoneSkinHits() > 0;
                int taken = target.takeDamage(damage);
                if (taken > 0) {
                    if (!changed) effects.clear();
                    if (target instanceof Enemy enemy) { effects.add(new BattleEffect(enemy, -taken)); battle.reflectStoneSkin(shielded, burn.source, taken); battle.applyManaLeak(burn.manaLeak, enemy, taken); }
                    else if (target instanceof Student student) {
                        effects.add(new BattleEffect(student, -taken));
                        if (!student.isAlive()) battle.dotKnockout(student);
                    }
                    if (burn.spRecovery > 0 && burn.source instanceof Student hero && hero.isAlive()) {
                        int recovered = Math.min(burn.spRecovery, hero.getMaxSp() - hero.getSp());
                        if (recovered > 0) { hero.restoreSp(hero.getSp() + recovered); effects.add(BattleEffect.status(hero, "+" + recovered + " SP")); }
                    }
                    changed = true; damaged = true;
                }
        }
        burns.removeIf(burn -> !burn.target.isAlive() || burn.ticks >= burn.duration);
        if (changed) {
            battle.dotMessage(isVictory() ? "Damage over time defeats the last enemy! Victory!" : isDefeat() ? "Damage over time defeats the party!" : damaged ? "Damage over time strikes!" : "Damage over time was dodged!");
            if (isOver()) battle.clearGuards();
        }
        return changed;
    }
    boolean advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (isOver() || burns.isEmpty()) return false;
        java.util.List<BattleEffect> combined = new java.util.ArrayList<>();
        boolean changed = false;
        for (DamageOverTime burn : new java.util.ArrayList<>(burns)) {
            if (!burns.contains(burn) || !burn.target.isAlive() || isOver()) continue;
            burn.elapsedSeconds += seconds;
            while (burn.elapsedSeconds >= 1.0 && burns.contains(burn) && burn.target.isAlive() && !isOver()) {
                burn.elapsedSeconds -= 1.0;
                if (tickDamageOverTime(burn.target, burn)) { combined.addAll(battle.getEffects()); changed = true; }
            }
        }
        if (changed) battle.replaceEffects(combined);
        return changed;
    }
}
