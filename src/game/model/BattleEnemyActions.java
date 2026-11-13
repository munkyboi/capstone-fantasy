package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Resolves a ready enemy's action and species-specific skills. */
final class BattleEnemyActions {
    private final Battle battle;
    private final List<Student> party;
    private final List<Enemy> enemies, defeatedEarlier;
    private final List<BattleEffect> effects;
    private final Random random;
    private final GameSettings.Difficulty encounterDifficulty;
    private final double encounterPartyLevel;
    private final Map<Student, Integer> pulseInitial, pulsePrevious;
    private Enemy pulseCaster, sparkCaster;
    private Student sparkTarget;
    private int pulseLoops;
    private String message;
    private Battle.AbilityAnnouncement lastAbility;

    BattleEnemyActions(Battle battle, List<Student> party, List<Enemy> enemies, List<Enemy> defeatedEarlier,
            List<BattleEffect> effects, Random random, GameSettings.Difficulty encounterDifficulty,
            double encounterPartyLevel, Map<Student, Integer> pulseInitial, Map<Student, Integer> pulsePrevious,
            Enemy pulseCaster, int pulseLoops, Enemy sparkCaster, Student sparkTarget, String message) {
        this.battle = battle; this.party = party; this.enemies = enemies; this.defeatedEarlier = defeatedEarlier;
        this.effects = effects; this.random = random; this.encounterDifficulty = encounterDifficulty;
        this.encounterPartyLevel = encounterPartyLevel; this.pulseInitial = pulseInitial; this.pulsePrevious = pulsePrevious;
        this.pulseCaster = pulseCaster; this.pulseLoops = pulseLoops; this.sparkCaster = sparkCaster;
        this.sparkTarget = sparkTarget; this.message = message;
    }
    String message() { return message; }
    Battle.AbilityAnnouncement announcement() { return lastAbility; }
    Enemy pulseCaster() { return pulseCaster; }
    int pulseLoops() { return pulseLoops; }
    Enemy sparkCaster() { return sparkCaster; }
    Student sparkTarget() { return sparkTarget; }

    boolean act(Enemy enemy) {
        List<Student> living = party.stream().filter(Student::isAlive).toList();
        if (enemy.isBoss()) {
            long defeatedCompanions = enemies.stream().filter(other -> other != enemy && other.isDefeated()).count();
            boolean missingSupport = enemies.stream().anyMatch(other -> other != enemy && other.isDefeated() && other.isSentinel() && !other.isAttackSentinel());
            int action = enemy.guardianAction(defeatedCompanions >= 3 && missingSupport); enemy.recordAction();
            if (action == 1) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Crystal Pulse");
                message = enemy.getName() + " begins charging Crystal Pulse!";
            } else if (action == 2) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Crystal Pulse");
                pulseCaster = enemy; pulseInitial.clear(); pulsePrevious.clear();
                StringBuilder text = new StringBuilder(enemy.getName() + " unleashes Crystal Pulse!");
                for (Student target : living) {
                    if (!target.isAlive() || !enemy.isAlive()) continue;
                    int power = (int)Math.ceil(enemy.getAttack() * (180 + random.nextInt(21)) / 100.0);
                    int damage = battle.enemyDamage(enemy, target, power - enemy.getAttack(), 1);
                    Student recipient = battle.lastDamageRecipient();
                    pulseInitial.merge(recipient, damage, Integer::sum);
                    if (damage > 0 && recipient.isAlive() && random.nextDouble() < Battle.GUARDIAN_PULSE_STUN_CHANCE) {
                        battle.applyStun(recipient, 1);
                    }
                    text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                }
                message = text.toString();
                if (!battle.isOver() && enemy.isAlive() && random.nextDouble() < Battle.GUARDIAN_PULSE_LOOP_CHANCE) pulseLoops = 5;
                else { enemy.finishPulse(); pulseCaster = null; }
            } else if (action == 4) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Crystal Mesh");
                List<Integer> missing = new ArrayList<>();
                for (int i = 0; i < enemies.size(); i++) if (enemies.get(i) != enemy && enemies.get(i).isDefeated()) missing.add(i);
                double roll = random.nextDouble();
                int count = Math.min(missing.size(), roll < .15 ? 3 : roll < .40 ? 2 : 1);
                List<Integer> supports = missing.stream().filter(slot -> !enemies.get(slot).isAttackSentinel()).toList();
                for (int i = 0; i < count; i++) {
                    int slot = i == 0 ? supports.get(random.nextInt(supports.size())) : missing.get(random.nextInt(missing.size()));
                    missing.remove(Integer.valueOf(slot));
                    Enemy old = enemies.get(slot); defeatedEarlier.add(old);
                    battle.removeDamageOverTime(old);
                    enemies.set(slot, Enemy.sentinel(old.getName(), old.isAttackSentinel(), encounterDifficulty, 4, encounterPartyLevel, party.size()));
                }
                message = enemy.getName() + " uses Crystal Mesh! " + count + " Sentinel" + (count == 1 ? "" : "s") + " appear!";
            } else if (action == 3) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Crushing Grip");
                Student target = living.get(random.nextInt(living.size()));
                int power = (int)Math.ceil(enemy.getAttack() * (120 + random.nextInt(21)) / 100.0);
                int damage = battle.enemyDamage(enemy, target, power - enemy.getAttack(), 1);
                Student recipient = battle.lastDamageRecipient();
                if (damage > 0 && recipient.isAlive() && random.nextDouble() < .5) {
                    int total = enemy.scaleDamage(enemy.getAttack());
                    battle.addEnemyDamageOverTime(enemy, recipient, total, 3);
                    effects.add(BattleEffect.status(recipient, "DOT"));
                }
                message = enemy.getName() + " uses Crushing Grip on " + recipient.getName() + " for " + damage + " damage.";
            } else {
                Student target = living.get(random.nextInt(living.size()));
                int damage = battle.enemyDamage(enemy, target, 0, 1);
                message = enemy.getName() + " attacks " + battle.lastDamageRecipient().getName() + " for " + damage + " damage.";
            }
            if (battle.isDefeat()) { message += "\nThe party is defeated."; battle.clearGuards(); }
            else if (battle.isVictory()) { message += "\nAll enemies defeated! Victory!"; battle.clearGuards(); }
            return true;
        }
        if (enemy.isMerchant() || enemy.isMirror()) {
            int skill = enemy.merchantSkill(enemies.stream().anyMatch(e -> e.isMirror() && e.isAlive()));
            enemy.recordAction();
            if (skill == 1) {
                battle.removeMirrorDamageOverTime();
                enemies.removeIf(Enemy::isMirror);
                enemies.add(enemy.mirror(1)); enemies.add(enemy.mirror(2));
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Mirror Image");
                message = enemy.getName() + " uses Mirror Image!";
            } else if (skill == 5) {
                List<Enemy> copies = enemies.stream().filter(e -> e.isMirror() && e.isAlive()).toList();
                int hpTotal = copies.stream().mapToInt(Enemy::getHp).sum();
                int spTotal = copies.stream().mapToInt(Enemy::getSp).sum();
                int percent = 10 + random.nextInt(6);
                int hpRestored = enemy.heal((int) Math.ceil(hpTotal * percent / 100.0));
                int spRestored = enemy.absorbSp((int) Math.ceil(spTotal * percent / 100.0));
                battle.removeMirrorDamageOverTime();
                enemies.removeIf(Enemy::isMirror);
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Recall");
                if (hpRestored > 0) effects.add(new BattleEffect(enemy, hpRestored));
                if (spRestored > 0) effects.add(BattleEffect.status(enemy, "+" + spRestored + " SP"));
                message = enemy.getName() + " uses Recall, absorbing " + hpRestored + " HP and " + spRestored + " SP.";
            } else if (skill == 2) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Magic Missiles");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Magic Missiles!");
                List<Student> unselected = new ArrayList<>();
                for (int i = 0; i < (enemy.isMirror() ? 1 : 3); i++) {
                    List<Student> targets = party.stream().filter(Student::isAlive).toList(); if (targets.isEmpty()) break;
                    unselected.removeIf(target -> !target.isAlive());
                    if (unselected.isEmpty()) unselected.addAll(targets);
                    Student target = unselected.remove(random.nextInt(unselected.size()));
                    if (random.nextDouble() >= (enemy.isMirror() ? 0.5 : 0.6)) { effects.add(BattleEffect.status(target, "Miss")); text.append("\nMissed ").append(target.getName()).append("."); continue; }
                    int power = (int)Math.ceil(enemy.getAttack() * (enemy.isMirror() ? 80 + random.nextInt(21) : 100 + random.nextInt(41)) / 100.0);
                    int damage = battle.enemyDamage(enemy, target, power - enemy.getAttack(), 1, true);
                    Student recipient = battle.lastDamageRecipient();
                    if (damage > 0 && recipient.isAlive()) { battle.applyStun(recipient, enemy.isMirror() ? 1 : 2); }
                    text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                }
                message = text.toString();
            } else if (skill == 3) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Mana Burn");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Mana Burn!");
                Student target = living.get(random.nextInt(living.size()));
                if (random.nextDouble() >= 0.6) {
                    effects.add(BattleEffect.status(target, "Miss"));
                    text.append("\nMissed ").append(target.getName()).append(".");
                } else {
                    int damage = (int)Math.ceil((target.getMaxSp() - target.getSp()) * (50 + random.nextInt(11)) / 100.0);
                    int taken = target.takeDamage(damage); effects.add(new BattleEffect(target, -taken));
                    if (!target.isAlive()) { battle.clearKnockedOutHeroEffects(target); }
                    text.append("\n").append(target.getName()).append(" takes ").append(taken).append(" damage.");
                }
                message = text.toString();
            } else if (skill == 4) {
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Heat Stroke");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Heat Stroke!");
                for (int i = 0; i < 2; i++) {
                    List<Student> targets = party.stream().filter(Student::isAlive).toList(); if (targets.isEmpty()) break;
                    Student target = targets.get(random.nextInt(targets.size()));
                    int damage = enemy.scaleDamage((int)Math.ceil(enemy.getAttack() * (140 + random.nextInt(61)) / 100.0));
                    battle.addEnemyDamageOverTime(enemy, target, damage, 5);
                    effects.add(BattleEffect.status(target, "Heat Stroke"));
                    text.append("\n").append(target.getName()).append(" is afflicted with Heat Stroke.");
                }
                message = text.toString();
            } else {
                Student target = living.get(random.nextInt(living.size()));
                int damage = battle.enemyDamage(enemy, target, 0, 1);
                message = enemy.getName() + " attacks " + battle.lastDamageRecipient().getName() + " for " + damage + " damage.";
            }
            if (battle.isDefeat()) { message += "\nThe party is defeated."; battle.clearGuards(); }
            return true;
        }
        if (enemy.isLootGoblin() && enemy.getEscapeCountdown() >= 0) {
            enemy.advanceEscapeCountdown();
            if (enemy.getEscapeCountdown() == 0 && enemy.escape()) {
                enemy.recordAction();
                lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Escape");
                effects.add(BattleEffect.status(enemy, "Escape"));
                message = enemy.getName() + " uses Escape and flees!";
                if (battle.isVictory()) { message += "\nThe remaining enemies are gone!"; battle.clearGuards(); }
                return true;
            }
        }
        if (enemy.isLootGoblin()) {
            enemy.recordAction();
            message = enemy.getName() + " watches the battle.";
            return true;
        }
        if (enemy.isSentinel() && !enemy.isBoss()) {
            enemy.sentinelCooldowns();
            if (enemy.isAttackSentinel()) {
                int attackSkill = enemy.attackSkill();
                enemy.recordAction();
                if (attackSkill != 0) {
                    String name = attackSkill == 1 ? "Stone Sweep" : "Stone Smash";
                    lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), name);
                    StringBuilder text = new StringBuilder(enemy.getName() + " uses " + name + "!");
                    List<Student> targets = attackSkill == 1 ? living : List.of(living.get(random.nextInt(living.size())));
                    for (Student target : targets) {
                        if (!enemy.isAlive()) break;
                        if (!target.isAlive()) continue;
                        int percent = attackSkill == 1 ? 60 + random.nextInt(21) : 80 + random.nextInt(11);
                        int power = (int)Math.ceil(enemy.getAttack() * percent / 100.0);
                        boolean pierce = attackSkill == 1 && random.nextDouble() < .30;
                        int damage = battle.enemyDamage(enemy, target, power - enemy.getAttack(), 1, false, pierce);
                        Student recipient = battle.lastDamageRecipient();
                        text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                        if (attackSkill == 2 && damage > 0 && recipient.isAlive() && random.nextDouble() < .30) {
                            battle.applyStun(recipient, 3);
                        }
                    }
                    message = text.toString();
                } else {
                    Student target = living.get(random.nextInt(living.size()));
                    int damage = battle.enemyDamage(enemy, target, 0, 1);
                    message = enemy.getName() + " attacks " + battle.lastDamageRecipient().getName() + " for " + damage + " damage.";
                }
                if (battle.isDefeat()) { message += "\nThe party is defeated."; battle.clearGuards(); }
                else if (battle.isVictory()) { message += "\nAll enemies defeated! Victory!"; battle.clearGuards(); }
                return true;
            }
        }
        boolean skill = !enemy.isLootGoblin() && !enemy.isMirror() && (enemy.isSentinel() ? enemy.isBoss() && enemy.usesSkillNext() : enemy.useArcaneSpark());
        enemy.recordAction();
        Enemy healTarget = enemy.isSentinel() && !enemy.isBoss() && enemy.canUseStoneMend()
                ? enemies.stream().filter(other -> other.isAlive() && !other.isLootGoblin()
                        && other.getHp() * 2 <= other.getMaxHp())
                    .min(java.util.Comparator.comparingDouble(other -> (double) other.getHp() / other.getMaxHp())).orElse(null)
                : null;
        Enemy skinTarget = enemy.isSentinel() && !enemy.isBoss() ? enemies.stream().filter(Enemy::isAlive)
                .filter(other -> other.getStoneSkinHits() == 0)
                .max(java.util.Comparator.comparingInt(Enemy::getAttack)).orElse(null) : null;
        if (healTarget != null && enemy.spendHealSp()) {
            lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Stone Mend");
            int healed = healTarget.heal(enemy.healingPower(healTarget));
            effects.add(new BattleEffect(healTarget, healed));
            message = enemy.getName() + " uses Stone Mend and heals " + healTarget.getName() + " for " + healed + " HP.";
        } else if (skinTarget != null && enemy.castStoneSkin(skinTarget)) {
            lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Stone Skin");
            effects.add(BattleEffect.status(skinTarget, "Stone Skin"));
            message = enemy.getName() + " uses Stone Skin on " + skinTarget.getName() + ".";
        } else if (skill && enemy.isBoss()) {
            lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), enemy.isBoss() ? "Crystal Pulse" : "Stone Sweep");
            StringBuilder text = new StringBuilder(enemy.getName() + (enemy.isBoss() ? " uses Crystal Pulse!" : " uses Stone Sweep!"));
            for (Student target : living) {
                if (!target.isAlive()) continue;
                int damage = battle.enemyDamage(enemy, target, enemy.isBoss() ? 0 : -5, enemy.isBoss() ? 2 : 1);
                Student recipient = battle.lastDamageRecipient();
                text.append("\n");
                if (recipient != target) text.append(recipient.getName()).append(" protects ").append(target.getName()).append(". ");
                text.append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                if (!recipient.isAlive()) text.append(" ").append(recipient.getName()).append(" is knocked out.");
            }
            message = text.toString();
        } else {
            if (skill) lastAbility = new Battle.AbilityAnnouncement(enemy.getName(), "Arcane Spark");
            Student target = living.get(random.nextInt(living.size()));
            int power = skill ? (int) Math.ceil(enemy.getAttack() * (100 + random.nextInt(21)) / 100.0) : enemy.getAttack();
            int damage = battle.enemyDamage(enemy, target, power - enemy.getAttack(), 1);
            Student recipient = battle.lastDamageRecipient();
            message = skill ? enemy.getName() + " uses Arcane Spark on " + target.getName() + "."
                    : enemy.getName() + " attacks " + target.getName() + ".";
            if (recipient != target) message += " " + recipient.getName() + " protects " + target.getName() + ".";
            message += " " + recipient.getName() + " takes " + damage + " damage.";
            if (!recipient.isAlive()) { message += " " + recipient.getName() + " is knocked out."; }
            if (skill && !battle.isOver() && random.nextDouble() < 0.20) { sparkCaster = enemy; sparkTarget = target; }
        }
        if (battle.isDefeat()) { message += "\nThe party is defeated."; battle.clearGuards(); }
        else if (battle.isVictory()) { message += "\nAll enemies defeated! Victory!"; battle.clearGuards(); }
        return true;
    }
}
