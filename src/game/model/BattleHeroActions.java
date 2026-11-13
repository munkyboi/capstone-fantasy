package game.model;

import java.util.List;
import java.util.Random;

/** Resolves hero abilities and their delayed follow-up attacks. */
final class BattleHeroActions {
    private final Battle battle;
    private final List<Student> party;
    private final List<Enemy> enemies;
    private final Random random;
    private final List<BattleEffect> effects;
    private final BattleDotEngine dots;
    private final List<Enemy> rainArrowTargets = new java.util.ArrayList<>();
    private String message;
    private boolean pendingSecondCast;
    private int pendingLooperHits;
    private static final double LOOPER_LOOP_CHANCE = 0.70;
    private int twinStage;
    private Enemy twinTarget;
    private Student twinActor;
    private int twinAttackMultiplier = 1;
    private Student looperActor;
    private Enemy lastHeroHitTarget, looperTarget;
    private int lastHeroHitDamage, looperBaseDamage, looperPreviousDamage;
    private int angelHitsRemaining, angelDamage, angelAttackPower;
    private boolean angelExplosion;
    private Student angelActor;
    private Enemy angelTarget;

    BattleHeroActions(Battle battle, List<Student> party, List<Enemy> enemies, Random random, List<BattleEffect> effects, BattleDotEngine dots) {
        this.battle = battle; this.party = party; this.enemies = enemies; this.random = random; this.effects = effects; this.dots = dots;
    }
    List<Enemy> getRainArrowTargets() { return List.copyOf(rainArrowTargets); }
    boolean hasPendingLooperHits() { return pendingLooperHits > 0; }
    boolean hasPendingAngelAttack() { return angelHitsRemaining > 0 || angelExplosion; }
    boolean isAngelExplosionPending() { return angelExplosion; }
    boolean hasPendingTwinShot() { return twinStage != 0; }
    void recordHit(Enemy target, int damage) { lastHeroHitTarget = target; lastHeroHitDamage = damage; }
    private boolean run(java.util.function.BooleanSupplier action) { message = battle.getMessage(); boolean result = action.getAsBoolean(); battle.heroMessage(message); return result; }
    boolean attack(Enemy target) { return run(() -> attackInternal(target)); }
    boolean ability(Student ally, Enemy target) { return run(() -> abilityInternal(ally, target)); }
    boolean advanceTwinShot() { return run(this::advanceTwinShotInternal); }
    boolean advanceAngelAttack() { return run(this::advanceAngelAttackInternal); }
    boolean advanceLooperHit() { return run(this::advanceLooperHitInternal); }
    boolean completeSecondCast() { return run(this::completeSecondCastInternal); }
    private Student getCurrent() { return battle.getCurrent(); }
    private boolean isOver() { return battle.isOver(); }
    private boolean isVictory() { return battle.isVictory(); }
    private boolean isActionAnimating() { return battle.isActionAnimating(); }
    private boolean validEnemy(Enemy target) { return battle.validEnemy(target); }
    private int hurt(Enemy target, int power) { return battle.hurt(target, power); }
    private int hurt(Enemy target, int power, boolean guaranteed) { return battle.hurt(target, power, guaranteed); }
    private void applyLightSaber(Student s, Enemy e, int damage) { battle.applyLightSaber(s, e, damage); }
    private void reflectStoneSkin(boolean shielded, GameCharacter source, int damage) { battle.reflectStoneSkin(shielded, source, damage); }
    private void applyManaLeak(boolean enabled, Enemy target, int damage) { battle.applyManaLeak(enabled, target, damage); }
    private static boolean hasManaLeak(Student s) { return Battle.hasManaLeak(s); }
    private String defeated(Enemy e) { return battle.defeated(e); }
    private void clearKnockedOutHeroEffects(Student s) { battle.clearKnockedOutHeroEffects(s); }
    private void finish(String text) { battle.finish(text); message = battle.getMessage(); }
    private boolean attackInternal(Enemy target) {
        if (isActionAnimating() || isOver() || getCurrent() == null || !validEnemy(target)) { return false; }
        effects.clear();
        Student actor = getCurrent();
        if (actor.getId() == Student.CHARICE && actor.getSp() < 2) return false;
        battle.heroAnnouncement(null); rainArrowTargets.clear();
        lastHeroHitDamage = 0; lastHeroHitTarget = null;
        int multiplier = actor.consumeEtherealCharge() ? 4 : 1;
        int attackPower = actor.getAttack() * multiplier;
        if (actor.getId() == Student.CHARICE) {
            actor.restoreSp(actor.getSp() - 2);
            twinAttackMultiplier = multiplier;
            beginTwinShot(actor, target);
            return true;
        }
        if (actor.getId() == Student.ANGEL) {
            angelActor = actor; angelAttackPower = attackPower; angelTarget = target;
            angelHitsRemaining = 3 + ("giant_slayer_gloves".equals(actor.getEquipmentId(Equipment.Slot.WEAPON)) ? 2 : 0);
            angelDamage = 0;
            advanceAngelAttack();
            return true;
        }
        int damage = hurt(target, attackPower);
        applyLightSaber(actor, target, damage);
        finish(looperHits(actor, actor.getName() + " attacks " + target.getName() + " for " + damage + " damage." + defeated(target)));
        return true;
    }
    /** Uses the current student's special ability.
     * @param ally healing target for Angel
     * @param enemy enemy target for single-target abilities
     * @return whether the action was performed
     */
    private boolean abilityInternal(Student ally, Enemy enemy) {
        if (isActionAnimating() || isOver() || getCurrent() == null) { return false; }
        Student actor = getCurrent();
        rainArrowTargets.clear();
        lastHeroHitDamage = 0; lastHeroHitTarget = null;
        if (actor.getSp() < actor.getAbilitySpCost()) { return false; }
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS && (ally == null || !party.contains(ally))) { return false; }
        if (actor.getAbilityType() != Student.AbilityType.HEALING_KISS
                && actor.getAbilityType() != Student.AbilityType.ARCANE_BURST && actor.getAbilityType() != Student.AbilityType.RAIN_OF_ARROWS && !validEnemy(enemy)) { return false; }
        if (!actor.spendAbilitySp()) { return false; }
        String skillName = switch (actor.getAbilityType()) {
            case ARCANE_BURST -> "Arcane Burst"; case CADET_STRIKE -> "Cadet Strike";
            case RAIN_OF_ARROWS -> "Rain of Arrows"; case HEALING_KISS -> "Angel Hug";
        };
        battle.heroAnnouncement(new Battle.AbilityAnnouncement(actor.getName(), skillName));
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS) {
            effects.clear();
            boolean revived = !ally.isAlive();
            if (revived) clearKnockedOutHeroEffects(ally);
            int healed = ally.heal((int) Math.ceil(revived ? ally.getMaxHp() * 0.3 : (ally.getMaxHp() - ally.getHp()) * 0.5));
            if (revived) ally.restoreSp((int) Math.ceil(ally.getMaxSp() * 0.3));
            if (healed > 0) { effects.add(new BattleEffect(ally, healed)); }
            finish("Angel uses Angel Hug. " + ally.getName() + (revived ? " is revived and" : "") + " recovers " + healed + " HP.");
        } else if (actor.getAbilityType() == Student.AbilityType.ARCANE_BURST) {
            boolean doubleCast = "great_sage_staff".equals(actor.getEquipmentId(Equipment.Slot.WEAPON)) && random.nextDouble() < 0.5;
            String text = arcaneBurst(actor, "Zack uses Arcane Burst!");
            if (doubleCast && !isVictory()) {
                pendingSecondCast = true;
                message = text;
            } else {
                finish(text);
            }
        } else if (actor.getAbilityType() == Student.AbilityType.RAIN_OF_ARROWS) {
            effects.clear();
            java.util.Set<Enemy> struck = new java.util.LinkedHashSet<>();
            StringBuilder text = new StringBuilder(actor.getName() + " uses Rain of Arrows!");
            for (int arrow = 0; arrow < 12 && actor.isAlive(); arrow++) {
                List<Enemy> living = enemies.stream().filter(Enemy::isAlive).toList();
                if (living.isEmpty()) break;
                Enemy target = living.get(random.nextInt(living.size()));
                rainArrowTargets.add(target);
                int power = (int)Math.ceil(actor.getAttack() * (40 + random.nextInt(21)) / 100.0);
                int damage = hurt(target, power);
                if (damage > 0) struck.add(target);
                text.append("\n").append(target.getName()).append(" takes ").append(damage).append(" damage.").append(defeated(target));
            }
            for (Enemy target : struck) if (target.isAlive() && random.nextDouble() < .5) {
                dots.replaceRecovery(actor, target, actor.getAttack(), hasManaLeak(actor));
                effects.add(BattleEffect.status(target, "DOT"));
            }
            finish(looperHits(actor, text.toString()));
        } else {
            if (!validEnemy(enemy)) { return false; }
            effects.clear();
            double bonusRate = 0.60;
            int damage = hurt(enemy, actor.getAttack() + (int) Math.ceil(actor.getBaseAttack() * bonusRate));
            applyLightSaber(actor, enemy, damage);
            String ability = "Cadet Strike";
            finish(looperHits(actor, actor.getName() + " uses " + ability + " on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy)));
        }
        return true;
    }
    private void beginTwinShot(Student actor, Enemy enemy) {
        int damage = hurt(enemy, (int)Math.ceil(actor.getAttack() * twinAttackMultiplier * (80 + random.nextInt(21)) / 100.0));
        boolean reducedDefense = damage > 0 && enemy.isAlive() && random.nextDouble() < 0.5;
        int defenseBefore = enemy.getDefense();
        if (reducedDefense) enemy.applyTwinDefensePenalty();
        int defensePenalty = defenseBefore - enemy.getDefense();
        message = actor.getName() + " uses Twin Shot on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy);
        if (reducedDefense && defensePenalty > 0) {
            effects.add(BattleEffect.status(enemy, "DEF -" + defensePenalty));
            message += "\n" + enemy.getName() + " loses " + defensePenalty + " DEF for Twin Shot.";
        }
        if (enemy.isAlive() && actor.isAlive()) { twinActor = actor; twinTarget = enemy; twinStage = 1; }
        else { enemy.clearTwinDefensePenalty(); finish(looperHits(actor, message)); }
    }
    /** Resolves the second shot, then the optional third shot, without another action or SP cost.
     * @return the result of advance twin shot
     */
    private boolean advanceTwinShotInternal() {
        if (!hasPendingTwinShot() || twinActor == null || twinTarget == null) return false;
        Student actor = twinActor; Enemy target = twinTarget;
        if (!target.isAlive() || !actor.isAlive() || isOver()) { target.clearTwinDefensePenalty(); twinStage = 0; twinTarget = null; twinActor = null; finish(message); return false; }
        effects.clear();
        int damage = hurt(target, (int)Math.ceil(actor.getAttack() * twinAttackMultiplier * 0.8), true);
        message = actor.getName() + " hits " + target.getName() + " for " + damage + " damage." + defeated(target);
        if (twinStage == 1 && target.isAlive() && actor.isAlive() && random.nextDouble() < .7) { twinStage = 2; }
        else {
            target.clearTwinDefensePenalty(); twinStage = 0; twinTarget = null; twinActor = null;
            finish(looperHits(actor, message));
        }
        return true;
    }
    private String arcaneBurst(Student actor, String heading) {
        effects.clear();
        StringBuilder text = new StringBuilder(heading);
        for (Enemy target : enemies) {
            if (!actor.isAlive()) break;
            if (target.isAlive()) {
                int damage = hurt(target, actor.getAttack() + (int) Math.ceil(actor.getBaseAttack() * 0.40));
                text.append("\n").append(target.getName()).append(" takes ").append(damage).append(" damage.").append(defeated(target));
            }
        }
        return looperHits(actor, text.toString());
    }
    /** Resolves one punch or the final combo explosion.
     * @return the result of advance angel attack
     */
    private boolean advanceAngelAttackInternal() {
        if (!hasPendingAngelAttack() || hasPendingLooperHits()) return false;
        if (!angelActor.isAlive()) { angelHitsRemaining = 0; angelExplosion = false; finish(message); return false; }
        List<Enemy> living = enemies.stream().filter(Enemy::isAlive).toList();
        if (living.isEmpty()) { angelHitsRemaining = 0; angelExplosion = false; finish(message); return false; }
        effects.clear();
        if (angelExplosion) {
            Enemy target = angelTarget.isAlive() ? angelTarget : living.get(random.nextInt(living.size()));
            int power = (int) Math.ceil(angelDamage * (80 + random.nextInt(21)) / 100.0);
            boolean shielded = target.getStoneSkinHits() > 0;
            int damage = target.takeDamage(power);
            reflectStoneSkin(shielded, angelActor, damage);
            if (damage > 0) { effects.add(new BattleEffect(target, -damage)); applyManaLeak(hasManaLeak(angelActor), target, damage); }
            angelExplosion = false;
            finish("Combo explosion hits " + target.getName() + " for " + damage + " damage." + defeated(target));
        } else {
            if (angelHitsRemaining < ("giant_slayer_gloves".equals(angelActor.getEquipmentId(Equipment.Slot.WEAPON)) ? 5 : 3))
                angelTarget = living.get(random.nextInt(living.size()));
            int damage = hurt(angelTarget, angelAttackPower);
            angelDamage += damage; angelHitsRemaining--;
            message = "Angel attacks " + angelTarget.getName() + " for " + damage + " damage." + defeated(angelTarget);
            if (isVictory() || !angelActor.isAlive()) { angelHitsRemaining = 0; angelExplosion = false; finish(message); }
            else if (angelHitsRemaining == 0) { angelExplosion = true; message = looperHits(angelActor, message); }
        }
        return true;
    }
    /** Starts a chain based on the last hit of an attack or damaging cast. */
    private String looperHits(Student actor, String message) {
        if (!"looper_ring".equals(actor.getEquipmentId(Equipment.Slot.ACCESSORY))
                || !actor.isAlive() || lastHeroHitDamage <= 0 || isVictory() || random.nextDouble() >= LOOPER_LOOP_CHANCE) return message;
        pendingLooperHits = 5; looperActor = actor; looperTarget = lastHeroHitTarget;
        looperBaseDamage = lastHeroHitDamage; looperPreviousDamage = 0;
        return message + "\nLooper Ring activates!";
    }
    /** Resolves one queued Looper Ring hit. The UI schedules these at 300 ms intervals.
     * @return the result of advance looper hit
     */
    private boolean advanceLooperHitInternal() {
        if (!hasPendingLooperHits()) return false;
        List<Enemy> living = enemies.stream().filter(Enemy::isAlive).toList();
        if (living.isEmpty() || isOver()) { pendingLooperHits = 0; pendingSecondCast = false; finish(message); return false; }
        Enemy target = looperTarget != null && looperTarget.isAlive() ? looperTarget : living.get(random.nextInt(living.size()));
        looperTarget = target;
        int power = (int) Math.min(Integer.MAX_VALUE, ((long) looperBaseDamage * 5 + (long) looperPreviousDamage * 2 + 9) / 10);
        effects.clear();
        boolean shielded = target.getStoneSkinHits() > 0;
        int damage = target.takeDamage(power);
        reflectStoneSkin(shielded, looperActor, damage);
        looperPreviousDamage = damage;
        if (angelExplosion && looperActor == angelActor) { angelDamage += damage; angelTarget = target; }
        effects.add(new BattleEffect(target, -damage)); applyManaLeak(true, target, damage);
        message = "Looper Ring hits " + target.getName() + " for " + damage + " damage." + defeated(target);
        pendingLooperHits--;
        if (!looperActor.isAlive()) { pendingLooperHits = 0; pendingSecondCast = false; angelExplosion = false; }
        if (pendingLooperHits > 0 && random.nextDouble() >= LOOPER_LOOP_CHANCE) pendingLooperHits = 0;
        if (isVictory()) { pendingLooperHits = 0; pendingSecondCast = false; angelExplosion = false; finish(message); }
        else if (pendingLooperHits == 0 && !pendingSecondCast && !angelExplosion) finish(message);
        return true;
    }
    /** Whether the Great Sage Staff has a follow-up cast waiting to resolve.
     * @return whether the Great Sage Staff has a follow-up cast waiting to resolve
     */
    boolean hasPendingSecondCast() { return pendingSecondCast; }
    /** Applies the delayed follow-up without spending extra SP or an extra action.
     * @return the result of complete second cast
     */
    private boolean completeSecondCastInternal() {
        if (!pendingSecondCast || hasPendingLooperHits()) return false;
        pendingSecondCast = false;
        if (isOver() || getCurrent() == null || !getCurrent().isAlive()) return false;
        battle.heroAnnouncement(new Battle.AbilityAnnouncement(getCurrent().getName(), "Arcane Burst"));
        finish(arcaneBurst(getCurrent(), "The staff casts Arcane Burst again!"));
        return true;
    }
}
