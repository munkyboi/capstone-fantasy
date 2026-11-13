package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls an encounter, independent of the Swing interface. */
public final class Battle {
    private static final int DAMAGE_VARIATION = 5;
    private static final int MINIMUM_DAMAGE = 1;
    private static final double GUARDIAN_PULSE_STUN_CHANCE = 0.40;
    private static final double GUARDIAN_PULSE_LOOP_CHANCE = 0.45;
    private final List<Student> party = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> defeatedEarlier = new ArrayList<>();
    private final GameSettings.Difficulty encounterDifficulty;
    private final double encounterPartyLevel;
    private final Random random;
    private final java.util.Map<Student, Student> guards = new java.util.HashMap<>();
    private Student blockingStudent, protectedStudent, lastDamageRecipient;
    private final double accuracyScalingFactor;
    private final GameProgress progress;
    private final List<BattleEffect> effects = new ArrayList<>();
    private final java.util.Map<Student, Integer> stuns = new java.util.HashMap<>();
    private final boolean merchantEncounter;
    private final java.util.Set<Student> apRefills = new java.util.HashSet<>();
    private final java.util.List<DamageOverTime> burns = new java.util.ArrayList<>();
    private static final class DamageOverTime {
        final GameCharacter source, target; final int total, duration; final boolean manaLeak; int ticks;
        DamageOverTime(GameCharacter source, GameCharacter target, int damage) { this(source, target, damage, false); }
        DamageOverTime(GameCharacter source, GameCharacter target, int damage, boolean manaLeak) { this(source, target, damage, manaLeak, 5); }
        DamageOverTime(GameCharacter source, GameCharacter target, int damage, boolean manaLeak, int duration) { this.source = source; this.target = target; total = damage; this.manaLeak = manaLeak; this.duration = duration; }
    }
    private int turn = -1;
    private boolean pendingSecondCast;
    private int pendingLooperHits;
    private static final double LOOPER_LOOP_CHANCE = 0.70;
    private int twinStage;
    private Enemy twinTarget;
    private Student twinActor;
    private Student looperActor;
    private Enemy lastHeroHitTarget, looperTarget;
    private int lastHeroHitDamage, looperBaseDamage, looperPreviousDamage;
    private int angelHitsRemaining, angelDamage, angelAttackPower;
    private boolean angelExplosion;
    private Student angelActor;
    private Enemy angelTarget;
    private Enemy sparkCaster;
    private Student sparkTarget;
    private Enemy pulseCaster;
    private int pulseLoops;
    private final java.util.Map<Student, Integer> pulseInitial = new java.util.HashMap<>(), pulsePrevious = new java.util.HashMap<>();
    private Student escapeInitiator;
    private int escapeTurnsRemaining;
    public boolean isEscaping() { return escapeInitiator != null; }
    public Student getEscapeInitiator() { return escapeInitiator; }
    public int getEscapeTurnsRemaining() { return escapeTurnsRemaining; }
    /** Starts a three-turn escape; selecting the command consumes the first turn. */
    public boolean beginEscape() {
        if (isOver() || isActionAnimating() || isEscaping() || !canUseDefendOrEscape()) return false;
        escapeInitiator = getCurrent(); escapeTurnsRemaining = 2;
        finish(escapeInitiator.getName() + " leads the party's escape! Turns remaining: 2.");
        return true;
    }
    /** Cancels escape if its initiator is no longer able to lead. */
    public boolean cancelEscapeIfKnockedOut() {
        if (escapeInitiator == null || escapeInitiator.isAlive()) return false;
        message = escapeInitiator.getName() + " was knocked out. Escape failed.";
        escapeInitiator = null; escapeTurnsRemaining = 0; return true;
    }
    /** Consumes an initiator's ready turn and returns true when escape completes. */
    public boolean advanceEscape(Student student) {
        if (student == null || student != escapeInitiator || !student.isAlive() || escapeTurnsRemaining <= 0) return false;
        escapeTurnsRemaining--;
        if (escapeTurnsRemaining > 0) { message = "Escaping... Turns remaining: " + escapeTurnsRemaining + "."; return false; }
        escapeInitiator = null;
        for (Student hero : party) hero.applyEscapeXpPenalty();
        progress.spendPetot((progress.getPetot() + 9) / 10);
        message = "The party escaped!";
        return true;
    }
    public boolean hasPendingSparkExplosion() { return sparkCaster != null; }
    public boolean hasPendingLooperHits() { return pendingLooperHits > 0; }
    public boolean hasPendingAngelAttack() { return angelHitsRemaining > 0 || angelExplosion; }
    public boolean isAngelExplosionPending() { return angelExplosion; }
    public boolean hasPendingGuardianLoop() { return pulseLoops > 0; }
    public boolean isActionAnimating() { return pendingSecondCast || hasPendingLooperHits() || hasPendingAngelAttack() || hasPendingSparkExplosion() || hasPendingTwinShot() || hasPendingGuardianLoop(); }
    public boolean hasPendingTwinShot() { return twinStage != 0; }
    public int twinShotDelayMillis() { return 300; }
    public record AbilityAnnouncement(String caster, String skill) { }
    private AbilityAnnouncement lastAbility;
    public AbilityAnnouncement getLastAbility() { return lastAbility; }
    private String message;

    /** Creates an encounter for exactly three different students.
     * @param ids selected student indices
     * @param random randomness used in combat
     */
    public Battle(List<Integer> ids, Random random) {
        this(new GameProgress(ids), random);
    }
    /** Creates a battle using a party's existing state.
     * @param progress party and encounter progress
     * @param random randomness used in combat
     */
    public Battle(GameProgress progress, Random random) {
        this(progress, random, false);
    }

    /** Creates a route encounter, possibly starting with an ambush.
     * @param progress party and encounter progress
     * @param random randomness used in combat
     * @param ambushed whether enemies ambush the party
     */
    public Battle(GameProgress progress, Random random, boolean ambushed) {
        this(progress, random, ambushed, progress.getEncounter());
    }

    /** Starts a specific unlocked route encounter, including a replay. */
    public Battle(GameProgress progress, Random random, boolean ambushed, int encounter) {
        this(progress, random, ambushed, encounter, GameSettings.Difficulty.NORMAL);
    }
    /** Starts an encounter using the selected monster difficulty. */
    public Battle(GameProgress progress, Random random, boolean ambushed, int encounter, GameSettings.Difficulty difficulty) {
        this(progress, random, ambushed, encounter, difficulty, false);
    }
    public static Battle merchantBattle(GameProgress progress, Random random, GameSettings.Difficulty difficulty) {
        return new Battle(progress, random, false, 2, difficulty, true);
    }
    private Battle(GameProgress progress, Random random, boolean ambushed, int encounter, GameSettings.Difficulty difficulty, boolean merchantEncounter) {
        if (encounter < 0 || encounter > 4 || encounter > progress.getEncounter()) {
            throw new IllegalArgumentException("This encounter is not available yet");
        }
        this.progress = progress;
        this.merchantEncounter = merchantEncounter;
        if (merchantEncounter && (progress.getEncounter() < 3 || progress.isMerchantDefeated())) throw new IllegalArgumentException("Merchant challenge unavailable");
        this.random = random;
        party.addAll(progress.getParty());
        double partyMultiplier = switch (party.size()) {
            case 1 -> 3.0; case 2 -> 2.0; case 3 -> 1.5; case 4 -> 1.0;
            default -> throw new IllegalArgumentException("Invalid party size");
        };
        this.accuracyScalingFactor = 100.0 * java.util.Objects.requireNonNull(difficulty).multiplier() * partyMultiplier;
        for (Student student : party) { student.clearDefense(); }
        party.sort(Comparator.comparingDouble(Student::getEffectiveSpeed).reversed());
        if (party.stream().noneMatch(Student::isAlive)) {
            throw new IllegalArgumentException("At least one student must be able to battle");
        }
        double averageLevel = party.stream().mapToInt(Student::getLevel).average().orElse(1.0);
        encounterDifficulty = difficulty; encounterPartyLevel = averageLevel;
        if (merchantEncounter) {
            enemies.add(Enemy.merchant(difficulty, averageLevel));
            message = "The Mysterious Merchant challenges the party!";
            return;
        }
        int count = EncounterRules.rollMonsterCount(encounter, random, difficulty);
        boolean finalEncounter = encounter == GameProgress.FINAL_BOSS_ENCOUNTER;
        ambushed = ambushed && !finalEncounter;
        int sentinels = finalEncounter ? 5 : encounter == 3 ? 1 + random.nextInt(3) : encounter >= 1 ? 1 : 0;
        int wisps = count - sentinels - (finalEncounter ? 1 : 0);
        for (int index = 0; index < wisps; index++) {
            String name = "Library Wisp " + (char) ('A' + index);
            enemies.add(encounter == 0 && party.size() == 1 ? Enemy.soloOpeningWisp(name, difficulty, averageLevel)
                    : new Enemy(name, false, false, difficulty, encounter, averageLevel));
        }
        for (int index = 0; index < sentinels; index++) {
            boolean attack = finalEncounter ? index >= 2 : index % 2 == 1;
            int variantIndex = finalEncounter ? (attack ? index - 2 : index) : index / 2;
            String name = (attack ? "Attack Sentinel " : "Support Sentinel ") + (char) ('A' + variantIndex);
            enemies.add(Enemy.sentinel(name, attack, difficulty, encounter, averageLevel, finalEncounter ? party.size() : 0));
        }
        if (finalEncounter) { enemies.add(Enemy.finalOpponent("Crystal Guardian", true, difficulty, averageLevel, party.size())); }
        boolean goblinSpawned = !finalEncounter && random.nextInt(100) >= 95;
        if (goblinSpawned) {
            for (int index = enemies.size() - 1; index >= 0; index--) {
                if (!enemies.get(index).isSentinel()) { enemies.remove(index); wisps--; break; }
            }
            enemies.add(Enemy.itmigims(difficulty, encounter, averageLevel));
        }
        message = finalEncounter ? "The Crystal Guardian and five Sentinels appear!"
                : sentinels > 0 ? sentinels + " Stone Sentinel(s)" + (wisps > 0 ? " and " + wisps + " Library Wisp(s) appear!" : " appear!")
                : wisps > 0 ? wisps + " Library Wisp(s) appear! Choose an action."
                : "Itmigims appears! Choose an action.";
        if (goblinSpawned && (wisps > 0 || sentinels > 0)) { message += "\nItmigims appears!"; }
        if (ambushed) { message = "You have been ambushed!\n" + message; }
        enemies.sort(Comparator.comparingInt(Enemy::getSpeed).reversed());
    }
    /** The shared hit/dodge scaling factor, fixed from the party size at battle start. */
    public double getAccuracyScalingFactor() { return accuracyScalingFactor; }
    public boolean isMerchantEncounter() { return merchantEncounter; }
    public int getStunTurns(Student student) { return stuns.getOrDefault(student, 0); }
    public boolean consumeStunTurn(Student student) {
        int remaining = getStunTurns(student);
        if (remaining <= 0) return false;
        if (remaining == 1) stuns.remove(student); else stuns.put(student, remaining - 1);
        effects.clear(); effects.add(BattleEffect.status(student, "Stunned"));
        message = student.getName() + " is stunned and misses a turn.";
        return true;
    }
    /** Returns party in speed order.
     * @return party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns enemies in speed order.
     * @return enemies in speed order
     */ public List<Enemy> getEnemies() { return List.copyOf(enemies); }
    /** All defeated waves remain eligible for victory rewards. */
    public List<Enemy> getRewardEnemies() {
        List<Enemy> result = new ArrayList<>(defeatedEarlier); result.addAll(enemies); return List.copyOf(result);
    }
    /** Returns selected ready student, or null while waiting.
     * @return selected ready student, or null while waiting
     */ public Student getCurrent() { return turn < 0 ? null : party.get(turn); }
    /** Returns remaining first-aid kits.
     * @return remaining first-aid kits
     */ public int getKits() { return progress.getKits(); }
    /** Returns latest combat messages.
     * @return latest combat messages
     */ public String getMessage() { return message; }
    /** Returns HP changes from the most recent action.
     * @return HP changes from the most recent action
     */ public List<BattleEffect> getEffects() { return List.copyOf(effects); }
    /** Returns and clears equipment-triggered AP refills. */
    public java.util.Set<Student> consumeApRefills() {
        java.util.Set<Student> result = java.util.Set.copyOf(apRefills); apRefills.clear(); return result;
    }
    /** Whether a living participant has any active damage-over-time effect. */
    public boolean hasDamageOverTime(GameCharacter target) {
        return target != null && target.isAlive() && !isOver() && burns.stream().anyMatch(burn -> burn.target == target && burn.ticks < burn.duration);
    }
    private void clearKnockedOutHeroEffects(Student hero) {
        burns.removeIf(burn -> burn.target == hero);
        guards.remove(hero); hero.endDefend();
    }
    /** Applies one tick to effects on a participant whose action bar has filled. */
    public boolean tickDamageOverTime(GameCharacter participant) {
        if (participant == null || isOver() || !participant.isAlive() || burns.isEmpty()) return false;
        boolean changed = false, damaged = false;
        for (DamageOverTime burn : new java.util.ArrayList<>(burns)) {
            if (!burns.contains(burn)) continue;
            GameCharacter target = burn.target;
            if (target != participant || !target.isAlive() || burn.ticks >= burn.duration) continue;
                int before = (int) ((long) burn.total * burn.ticks / burn.duration);
                burn.ticks++;
                int damage = (int) ((long) burn.total * burn.ticks / burn.duration) - before;
                if (damage > 0 && (!hits(burn.source, target)
                        || (target instanceof Student student && "ultra_instinct_shirt".equals(student.getEquipmentId(Equipment.Slot.ARMOR)) && random.nextDouble() < 0.5))) {
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
                    if (target instanceof Enemy enemy) { effects.add(new BattleEffect(enemy, -taken)); reflectStoneSkin(shielded, burn.source, taken); applyManaLeak(burn.manaLeak, enemy, taken); }
                    else if (target instanceof Student student) {
                        effects.add(new BattleEffect(student, -taken));
                        if (!student.isAlive()) { guards.remove(student); student.endDefend(); if (getCurrent() == student) turn = -1; }
                    }
                    changed = true; damaged = true;
                }
        }
        burns.removeIf(burn -> !burn.target.isAlive() || burn.ticks >= burn.duration);
        if (changed) {
            message = isVictory() ? "Damage over time defeats the last enemy! Victory!" : isDefeat() ? "Damage over time defeats the party!" : damaged ? "Damage over time strikes!" : "Damage over time was dodged!";
            if (isOver()) clearGuards();
        }
        return changed;
    }
    /** Keeps DOT indicators when the participant also performs an action. */
    public void prependEffects(List<BattleEffect> earlier) { if (earlier != null && !earlier.isEmpty()) effects.addAll(0, earlier); }
    /** Displays all effects produced by simultaneous AP fills. */
    public void replaceEffects(List<BattleEffect> combined) { effects.clear(); effects.addAll(combined); }
    private void applyLightSaber(Student actor, Enemy target, int damage) {
        if (damage > 0 && target.isAlive() && "light_saber".equals(actor.getEquipmentId(Equipment.Slot.WEAPON))) {
            burns.add(new DamageOverTime(actor, target, damage, hasManaLeak(actor)));
        }
    }
    /** Rolls once for each character whose AP has just filled. */
    public boolean regenerate(java.util.List<Student> students, boolean keepCurrentEffects) {
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
        if (restored) message = keepCurrentEffects ? message + "\n" + text : text.toString();
        return restored;
    }
    /** Selects a living student whose AP is ready.
     * @param student student chosen by the battle clock
     * @return true if selected
     */
    public boolean selectStudent(Student student) {
        int index = party.indexOf(student);
        if (isOver() || isEscaping() || isActionAnimating() || getCurrent() != null || index < 0 || !student.isAlive()) { return false; }
        guards.remove(student);
        student.endDefend();
        turn = index;
        return true;
    }
    /** Returns whether either side is defeated.
     * @return whether either side is defeated
     */ public boolean isOver() { return isVictory() || isDefeat(); }
    /** Returns whether all enemies are defeated.
     * @return whether all enemies are defeated
     */ public boolean isVictory() { return merchantEncounter ? enemies.stream().filter(Enemy::isMerchant).noneMatch(Enemy::isAlive) : enemies.stream().noneMatch(Enemy::isAlive); }
    /** Returns whether all students are knocked out.
     * @return whether all students are knocked out
     */ public boolean isDefeat() {
        List<GameCharacter> participants = new ArrayList<>(party);
        return participants.stream().noneMatch(GameCharacter::isAlive);
    }

    /** Attacks a living enemy.
     * @param target chosen enemy
     * @return whether the action was performed
     */
    public boolean attack(Enemy target) {
        if (isActionAnimating() || isOver() || getCurrent() == null || !validEnemy(target)) { return false; }
        effects.clear();
        Student actor = getCurrent();
        lastHeroHitDamage = 0; lastHeroHitTarget = null;
        int attackPower = actor.getAttack() * (actor.consumeEtherealCharge() ? 4 : 1);
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
    public boolean ability(Student ally, Enemy enemy) {
        if (isActionAnimating() || isOver() || getCurrent() == null) { return false; }
        Student actor = getCurrent();
        lastHeroHitDamage = 0; lastHeroHitTarget = null;
        if (actor.getSp() < actor.getAbilitySpCost()) { return false; }
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS && (ally == null || !party.contains(ally))) { return false; }
        if (actor.getAbilityType() != Student.AbilityType.HEALING_KISS
                && actor.getAbilityType() != Student.AbilityType.ARCANE_BURST && !validEnemy(enemy)) { return false; }
        if (!actor.spendAbilitySp()) { return false; }
        String skillName = switch (actor.getAbilityType()) {
            case ARCANE_BURST -> "Arcane Burst"; case CADET_STRIKE -> "Cadet Strike";
            case TWIN_SHOT -> "Twin Shot"; case HEALING_KISS -> "Angel Hug";
        };
        lastAbility = new AbilityAnnouncement(actor.getName(), skillName);
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
        } else if (actor.getAbilityType() == Student.AbilityType.TWIN_SHOT) {
            effects.clear();
            int damage = hurt(enemy, (int)Math.ceil(actor.getAttack() * 1.5));
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
        } else {
            if (!validEnemy(enemy)) { return false; }
            effects.clear();
            double bonusRate = actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? 0.60 : 0.50;
            int damage = hurt(enemy, actor.getAttack() + (int) Math.ceil(actor.getBaseAttack() * bonusRate));
            applyLightSaber(actor, enemy, damage);
            String ability = actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? "Cadet Strike" : "Twin Shot";
            finish(looperHits(actor, actor.getName() + " uses " + ability + " on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy)));
        }
        return true;
    }
    /** Resolves the second shot, then the optional third shot, without another action or SP cost. */
    public boolean advanceTwinShot() {
        if (!hasPendingTwinShot() || twinActor == null || twinTarget == null) return false;
        Student actor = twinActor; Enemy target = twinTarget;
        if (!target.isAlive() || !actor.isAlive() || isOver()) { target.clearTwinDefensePenalty(); twinStage = 0; twinTarget = null; twinActor = null; finish(message); return false; }
        effects.clear();
        int damage = hurt(target, actor.getAttack(), true);
        message = actor.getName() + " hits " + target.getName() + " for " + damage + " damage." + defeated(target);
        if (twinStage == 1 && target.isAlive() && actor.isAlive() && random.nextDouble() < .5) { twinStage = 2; }
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
    /** Resolves one punch or the final combo explosion. */
    public boolean advanceAngelAttack() {
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
    /** Resolves one queued Looper Ring hit. The UI schedules these at 300 ms intervals. */
    public boolean advanceLooperHit() {
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
    /** Whether the Great Sage Staff has a follow-up cast waiting to resolve. */
    public boolean hasPendingSecondCast() { return pendingSecondCast; }
    /** Applies the delayed follow-up without spending extra SP or an extra action. */
    public boolean completeSecondCast() {
        if (!pendingSecondCast || hasPendingLooperHits()) return false;
        pendingSecondCast = false;
        if (isOver() || getCurrent() == null || !getCurrent().isAlive()) return false;
        lastAbility = new AbilityAnnouncement(getCurrent().getName(), "Arcane Burst");
        finish(arcaneBurst(getCurrent(), "The staff casts Arcane Burst again!"));
        return true;
    }
    /** Uses a first-aid kit on a living student.
     * @param target student to heal
     * @return whether an item was used
     */
    public boolean useKit(Student target) {
        if (isActionAnimating() || isOver() || getCurrent() == null || !validAlly(target)) { return false; }
        int before = target.getHp();
        if (!progress.useKit(target)) { return false; }
        effects.clear();
        int healed = target.getHp() - before;
        effects.add(new BattleEffect(target, healed));
        finish(getCurrent().getName() + " uses a First-Aid Kit. " + target.getName()
                + " recovers " + healed + " HP.");
        return true;
    }
    /** Uses a White Flower on a fallen party member. */
    public boolean useWhiteFlower(Student target) {
        if (isActionAnimating() || isOver() || getCurrent() == null || target == null || !party.contains(target) || target.isAlive()) return false;
        int restored = progress.getInventory().useWhiteFlower(target);
        if (restored <= 0) return false;
        clearKnockedOutHeroEffects(target);
        effects.clear(); effects.add(new BattleEffect(target, restored));
        finish(getCurrent().getName() + " uses White Flower. " + target.getName() + " returns with " + restored + " HP.");
        return true;
    }
    /** Whether a living party member can be protected by the current actor. */
    public boolean canUseDefendOrEscape() {
        Student actor = getCurrent();
        return actor != null && !"heroes_ring".equals(actor.getEquipmentId(Equipment.Slot.ACCESSORY));
    }
    /** Victory Petot, including difficulty, optional greed, and Market Charm. */
    public int victoryPetot(boolean greed) {
        long amount = getRewardEnemies().stream().filter(Enemy::isDefeated).mapToLong(Enemy::getPetotDrop).sum();
        if (greed) amount *= 10;
        if (party.stream().anyMatch(s -> s.isAlive() && "market_charm".equals(s.getEquipmentId(Equipment.Slot.ACCESSORY)))) {
            amount = (amount * 11 + 9) / 10;
        }
        return (int)Math.min(GameProgress.MAX_PETOT, amount);
    }
    public boolean canDefend(Student target) {
        Student actor = getCurrent();
        return canUseDefendOrEscape() && validAlly(target) && (target != actor || party.stream().filter(Student::isAlive).count() == 1);
    }
    /** Protects one ally until the defender is selected for their next action. */
    public boolean defend(Student target) {
        if (isActionAnimating() || isOver() || !canDefend(target)) return false;
        effects.clear(); Student actor = getCurrent(); actor.defend(); guards.put(actor, target);
        blockingStudent = null; protectedStudent = null;
        finish(actor.getName() + " protects " + target.getName() + ".");
        return true;
    }
    /** Legacy self-defense entry point: protects the first other living party member. */
    public void defend() {
        Student actor = getCurrent();
        if (actor == null) return;
        Student target = party.stream().filter(this::canDefend).findFirst().orElse(null);
        if (target != null) defend(target);
    }
    public Student getBlockingStudent() { return blockingStudent; }
    public Student getProtectedStudent() { return protectedStudent; }
    public Student getGuardedAlly(Student defender) {
        Student ally = guards.get(defender);
        return defender != null && defender.isAlive() && ally != null && ally.isAlive() ? ally : null;
    }

    /** Lets a ready enemy act. Every third action uses its species-specific skill.
     * @param enemy ready enemy
     * @return whether the enemy acted
     */
    public boolean enemyAttack(Enemy enemy) {
        if (isOver() || isActionAnimating() || getCurrent() != null || !validEnemy(enemy)) { return false; }
        effects.clear(); blockingStudent = null; protectedStudent = null; lastAbility = null;
        List<Student> living = party.stream().filter(Student::isAlive).toList();
        if (enemy.isBoss()) {
            boolean companionsAlive = enemies.stream().anyMatch(other -> other != enemy && other.isAlive());
            int action = enemy.guardianAction(companionsAlive); enemy.recordAction();
            if (action == 1) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Crystal Pulse");
                message = enemy.getName() + " begins charging Crystal Pulse!";
            } else if (action == 2) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Crystal Pulse");
                pulseCaster = enemy; pulseInitial.clear(); pulsePrevious.clear();
                StringBuilder text = new StringBuilder(enemy.getName() + " unleashes Crystal Pulse!");
                for (Student target : living) {
                    if (!target.isAlive() || !enemy.isAlive()) continue;
                    int power = (int)Math.ceil(enemy.getAttack() * (180 + random.nextInt(21)) / 100.0);
                    int damage = enemyDamage(enemy, target, power - enemy.getAttack(), 1);
                    Student recipient = lastDamageRecipient;
                    pulseInitial.merge(recipient, damage, Integer::sum);
                    if (damage > 0 && recipient.isAlive() && random.nextDouble() < GUARDIAN_PULSE_STUN_CHANCE) {
                        stuns.merge(recipient, 1, Math::max); effects.add(BattleEffect.status(recipient, "Stunned"));
                    }
                    text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                }
                message = text.toString();
                if (!isOver() && enemy.isAlive() && random.nextDouble() < GUARDIAN_PULSE_LOOP_CHANCE) pulseLoops = 5;
                else { enemy.finishPulse(); pulseCaster = null; }
            } else if (action == 4) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Crystal Mesh");
                for (Enemy other : enemies) if (other != enemy && other.isDefeated()) defeatedEarlier.add(other);
                burns.removeIf(burn -> burn.target instanceof Enemy other && other != enemy && !other.isAlive());
                enemies.removeIf(other -> other != enemy && !other.isAlive());
                for (int index = 0; index < 5; index++) {
                    boolean attack = index >= 2;
                    String name = (attack ? "Attack Sentinel " : "Support Sentinel ") + (char)('A' + (attack ? index - 2 : index));
                    enemies.add(Enemy.sentinel(name, attack, encounterDifficulty, 4, encounterPartyLevel, party.size()));
                }
                message = enemy.getName() + " uses Crystal Mesh! Five Sentinels appear!";
            } else if (action == 3) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Crushing Grip");
                Student target = living.get(random.nextInt(living.size()));
                int power = (int)Math.ceil(enemy.getAttack() * (120 + random.nextInt(21)) / 100.0);
                int damage = enemyDamage(enemy, target, power - enemy.getAttack(), 1);
                Student recipient = lastDamageRecipient;
                if (damage > 0 && recipient.isAlive() && random.nextDouble() < .5) {
                    int total = enemy.scaleDamage(enemy.getAttack());
                    burns.add(new DamageOverTime(enemy, recipient, total, false, 3));
                    effects.add(BattleEffect.status(recipient, "DOT"));
                }
                message = enemy.getName() + " uses Crushing Grip on " + recipient.getName() + " for " + damage + " damage.";
            } else {
                Student target = living.get(random.nextInt(living.size()));
                int damage = enemyDamage(enemy, target, 0, 1);
                message = enemy.getName() + " attacks " + lastDamageRecipient.getName() + " for " + damage + " damage.";
            }
            if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
            else if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); }
            return true;
        }
        if (enemy.isMerchant() || enemy.isMirror()) {
            int skill = enemy.merchantSkill(enemies.stream().anyMatch(e -> e.isMirror() && e.isAlive()));
            enemy.recordAction();
            if (skill == 1) {
                burns.removeIf(burn -> burn.target instanceof Enemy other && other.isMirror());
                enemies.removeIf(Enemy::isMirror);
                enemies.add(enemy.mirror(1)); enemies.add(enemy.mirror(2));
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Mirror Image");
                message = enemy.getName() + " uses Mirror Image!";
            } else if (skill == 5) {
                List<Enemy> copies = enemies.stream().filter(e -> e.isMirror() && e.isAlive()).toList();
                int hpTotal = copies.stream().mapToInt(Enemy::getHp).sum();
                int spTotal = copies.stream().mapToInt(Enemy::getSp).sum();
                int percent = 10 + random.nextInt(6);
                int hpRestored = enemy.heal((int) Math.ceil(hpTotal * percent / 100.0));
                int spRestored = enemy.absorbSp((int) Math.ceil(spTotal * percent / 100.0));
                burns.removeIf(burn -> burn.target instanceof Enemy other && other.isMirror());
                enemies.removeIf(Enemy::isMirror);
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Recall");
                if (hpRestored > 0) effects.add(new BattleEffect(enemy, hpRestored));
                if (spRestored > 0) effects.add(BattleEffect.status(enemy, "+" + spRestored + " SP"));
                message = enemy.getName() + " uses Recall, absorbing " + hpRestored + " HP and " + spRestored + " SP.";
            } else if (skill == 2) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Magic Missiles");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Magic Missiles!");
                List<Student> unselected = new ArrayList<>();
                for (int i = 0; i < (enemy.isMirror() ? 1 : 3); i++) {
                    List<Student> targets = party.stream().filter(Student::isAlive).toList(); if (targets.isEmpty()) break;
                    unselected.removeIf(target -> !target.isAlive());
                    if (unselected.isEmpty()) unselected.addAll(targets);
                    Student target = unselected.remove(random.nextInt(unselected.size()));
                    if (random.nextDouble() >= (enemy.isMirror() ? 0.5 : 0.6)) { effects.add(BattleEffect.status(target, "Miss")); text.append("\nMissed ").append(target.getName()).append("."); continue; }
                    int power = (int)Math.ceil(enemy.getAttack() * (enemy.isMirror() ? 80 + random.nextInt(21) : 100 + random.nextInt(41)) / 100.0);
                    int damage = enemyDamage(enemy, target, power - enemy.getAttack(), 1, true);
                    Student recipient = lastDamageRecipient;
                    if (damage > 0 && recipient.isAlive()) { stuns.merge(recipient, enemy.isMirror() ? 1 : 2, Math::max); effects.add(BattleEffect.status(recipient, "Stunned")); }
                    text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                }
                message = text.toString();
            } else if (skill == 3) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Mana Burn");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Mana Burn!");
                Student target = living.get(random.nextInt(living.size()));
                if (random.nextDouble() >= 0.6) {
                    effects.add(BattleEffect.status(target, "Miss"));
                    text.append("\nMissed ").append(target.getName()).append(".");
                } else {
                    int damage = (int)Math.ceil((target.getMaxSp() - target.getSp()) * (50 + random.nextInt(11)) / 100.0);
                    int taken = target.takeDamage(damage); effects.add(new BattleEffect(target, -taken));
                    if (!target.isAlive()) { clearKnockedOutHeroEffects(target); }
                    text.append("\n").append(target.getName()).append(" takes ").append(taken).append(" damage.");
                }
                message = text.toString();
            } else if (skill == 4) {
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Heat Stroke");
                StringBuilder text = new StringBuilder(enemy.getName() + " uses Heat Stroke!");
                for (int i = 0; i < 2; i++) {
                    List<Student> targets = party.stream().filter(Student::isAlive).toList(); if (targets.isEmpty()) break;
                    Student target = targets.get(random.nextInt(targets.size()));
                    int damage = enemy.scaleDamage((int)Math.ceil(enemy.getAttack() * (140 + random.nextInt(61)) / 100.0));
                    burns.add(new DamageOverTime(enemy, target, damage));
                    effects.add(BattleEffect.status(target, "Heat Stroke"));
                    text.append("\n").append(target.getName()).append(" is afflicted with Heat Stroke.");
                }
                message = text.toString();
            } else {
                Student target = living.get(random.nextInt(living.size()));
                int damage = enemyDamage(enemy, target, 0, 1);
                message = enemy.getName() + " attacks " + lastDamageRecipient.getName() + " for " + damage + " damage.";
            }
            if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
            return true;
        }
        if (enemy.isLootGoblin() && enemy.getEscapeCountdown() >= 0) {
            enemy.advanceEscapeCountdown();
            if (enemy.getEscapeCountdown() == 0 && enemy.escape()) {
                enemy.recordAction();
                lastAbility = new AbilityAnnouncement(enemy.getName(), "Escape");
                effects.add(BattleEffect.status(enemy, "Escape"));
                message = enemy.getName() + " uses Escape and flees!";
                if (isVictory()) { message += "\nThe remaining enemies are gone!"; clearGuards(); }
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
                    lastAbility = new AbilityAnnouncement(enemy.getName(), name);
                    StringBuilder text = new StringBuilder(enemy.getName() + " uses " + name + "!");
                    List<Student> targets = attackSkill == 1 ? living : List.of(living.get(random.nextInt(living.size())));
                    for (Student target : targets) {
                        if (!enemy.isAlive()) break;
                        if (!target.isAlive()) continue;
                        int percent = attackSkill == 1 ? 60 + random.nextInt(21) : 80 + random.nextInt(11);
                        int power = (int)Math.ceil(enemy.getAttack() * percent / 100.0);
                        boolean pierce = attackSkill == 1 && random.nextDouble() < .30;
                        int damage = enemyDamage(enemy, target, power - enemy.getAttack(), 1, false, pierce);
                        Student recipient = lastDamageRecipient;
                        text.append("\n").append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                        if (attackSkill == 2 && damage > 0 && recipient.isAlive() && random.nextDouble() < .30) {
                            stuns.merge(recipient, 3, Math::max); effects.add(BattleEffect.status(recipient, "Stunned"));
                        }
                    }
                    message = text.toString();
                } else {
                    Student target = living.get(random.nextInt(living.size()));
                    int damage = enemyDamage(enemy, target, 0, 1);
                    message = enemy.getName() + " attacks " + lastDamageRecipient.getName() + " for " + damage + " damage.";
                }
                if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
                else if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); }
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
            lastAbility = new AbilityAnnouncement(enemy.getName(), "Stone Mend");
            int healed = healTarget.heal(enemy.healingPower(healTarget));
            effects.add(new BattleEffect(healTarget, healed));
            message = enemy.getName() + " uses Stone Mend and heals " + healTarget.getName() + " for " + healed + " HP.";
        } else if (skinTarget != null && enemy.castStoneSkin(skinTarget)) {
            lastAbility = new AbilityAnnouncement(enemy.getName(), "Stone Skin");
            effects.add(BattleEffect.status(skinTarget, "Stone Skin"));
            message = enemy.getName() + " uses Stone Skin on " + skinTarget.getName() + ".";
        } else if (skill && enemy.isBoss()) {
            lastAbility = new AbilityAnnouncement(enemy.getName(), enemy.isBoss() ? "Crystal Pulse" : "Stone Sweep");
            StringBuilder text = new StringBuilder(enemy.getName() + (enemy.isBoss() ? " uses Crystal Pulse!" : " uses Stone Sweep!"));
            for (Student target : living) {
                if (!target.isAlive()) continue;
                int damage = enemyDamage(enemy, target, enemy.isBoss() ? 0 : -5, enemy.isBoss() ? 2 : 1);
                Student recipient = lastDamageRecipient;
                text.append("\n");
                if (recipient != target) text.append(recipient.getName()).append(" protects ").append(target.getName()).append(". ");
                text.append(recipient.getName()).append(" takes ").append(damage).append(" damage.");
                if (!recipient.isAlive()) text.append(" ").append(recipient.getName()).append(" is knocked out.");
            }
            message = text.toString();
        } else {
            if (skill) lastAbility = new AbilityAnnouncement(enemy.getName(), "Arcane Spark");
            Student target = living.get(random.nextInt(living.size()));
            int power = skill ? (int) Math.ceil(enemy.getAttack() * (100 + random.nextInt(21)) / 100.0) : enemy.getAttack();
            int damage = enemyDamage(enemy, target, power - enemy.getAttack(), 1);
            Student recipient = lastDamageRecipient;
            message = skill ? enemy.getName() + " uses Arcane Spark on " + target.getName() + "."
                    : enemy.getName() + " attacks " + target.getName() + ".";
            if (recipient != target) message += " " + recipient.getName() + " protects " + target.getName() + ".";
            message += " " + recipient.getName() + " takes " + damage + " damage.";
            if (!recipient.isAlive()) { message += " " + recipient.getName() + " is knocked out."; }
            if (skill && !isOver() && random.nextDouble() < 0.20) { sparkCaster = enemy; sparkTarget = target; }
        }
        if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
        else if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); }
        return true;
    }

    /** Resolves one delayed Crystal Pulse loop against surviving original targets. */
    public boolean resolveGuardianLoop() {
        if (!hasPendingGuardianLoop()) return false;
        effects.clear(); StringBuilder text = new StringBuilder("Crystal Pulse echoes!");
        for (Student hero : party) {
            int base = pulseInitial.getOrDefault(hero, 0);
            if (!hero.isAlive() || base <= 0) continue;
            int amount = (int)Math.min(Integer.MAX_VALUE, ((long)base * 5 + (long)pulsePrevious.getOrDefault(hero, 0) * 2 + 9) / 10);
            int damage = hero.takeDamage(amount); pulsePrevious.put(hero, damage);
            if (damage > 0) effects.add(new BattleEffect(hero, -damage));
            if (!hero.isAlive()) clearKnockedOutHeroEffects(hero);
            text.append("\n").append(hero.getName()).append(" takes ").append(damage).append(" damage.");
        }
        message = text.toString(); pulseLoops--;
        if (isOver() || pulseLoops == 0 || random.nextDouble() >= GUARDIAN_PULSE_LOOP_CHANCE) {
            pulseLoops = 0; pulseCaster.finishPulse(); pulseCaster = null;
        }
        return true;
    }

    /** Resolves the delayed Arcane Spark explosion against living heroes. */
    public boolean resolveSparkExplosion() {
        if (!hasPendingSparkExplosion()) return false;
        Enemy caster = sparkCaster; Student primary = sparkTarget; sparkCaster = null; sparkTarget = null;
        effects.clear(); StringBuilder text = new StringBuilder("Arcane Spark explodes!");
        for (Student target : party) if (target.isAlive()) {
            int power = caster.getAttack() * (target == primary ? 2 : 1);
            int damage = enemyDamage(caster, target, power - caster.getAttack(), 1);
            text.append("\n").append(lastDamageRecipient.getName()).append(" takes ").append(damage).append(" damage.");
        }
        message = text.toString();
        if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
        else if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); }
        return true;
    }

    /** Chance to hit; difficulty and party size scale accuracy, with dodge capped at 60%. */
    public static double hitChance(double agility, double accuracy, double scalingFactor) {
        if (!Double.isFinite(agility) || !Double.isFinite(accuracy) || agility < 0 || accuracy <= 0 || !Double.isFinite(scalingFactor) || scalingFactor <= 0)
            throw new IllegalArgumentException("Invalid accuracy parameters");
        double dodge = agility / (agility + accuracy * scalingFactor / 100.0);
        return 1.0 - Math.min(0.60, dodge);
    }
    private boolean hits(GameCharacter attacker, GameCharacter defender) {
        return random.nextDouble() < hitChance(defender.getEffectiveAgility(), attacker.getEffectiveAccuracy(), accuracyScalingFactor);
    }
    private int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier) {
        return enemyDamage(enemy, target, bonus, multiplier, false);
    }
    private int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier, boolean guaranteedHit) {
        return enemyDamage(enemy, target, bonus, multiplier, guaranteedHit, false);
    }
    private int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier, boolean guaranteedHit, boolean ignoreDefense) {
        Student intended = target;
        Student protector = party.stream().filter(candidate -> candidate != intended && candidate.isDefending()
                && candidate.isAlive() && guards.get(candidate) == intended).findFirst().orElse(null);
        if (protector == null) {
            Student ringBearer = party.stream().filter(candidate -> candidate != intended && candidate.isAlive()
                    && "heroes_ring".equals(candidate.getEquipmentId(Equipment.Slot.ACCESSORY))).findFirst().orElse(null);
            if (ringBearer != null && random.nextDouble() < 0.5) protector = ringBearer;
        }
        if (protector != null) {
            target = protector; blockingStudent = protector; protectedStudent = intended;
            effects.add(BattleEffect.status(protector, "Guard"));
        }
        lastDamageRecipient = target;
        if (!guaranteedHit && !hits(enemy, target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        String armor = target.getEquipmentId(Equipment.Slot.ARMOR);
        if ("ultra_instinct_shirt".equals(armor) && random.nextDouble() < 0.5) {
            effects.add(BattleEffect.status(target, "Dodge"));
            return 0;
        }
        int power = Math.max(0, enemy.getAttack() + bonus);
        int adjusted = damageAfterDefense(power, ignoreDefense ? 0 : target.getDefense());
        double regenerationRate = "blade_mail".equals(armor) ? HealthRegeneration.rate(target) : 0;
        int incoming = enemy.scaleDamage(adjusted + (guaranteedHit ? 0 : random.nextInt(DAMAGE_VARIATION))) * multiplier;
        String accessory = target.getEquipmentId(Equipment.Slot.ACCESSORY);
        if ("guardian_band".equals(accessory) && random.nextDouble() < 0.15) {
            incoming = Math.max(1, (int)Math.ceil(incoming * 0.75));
            effects.add(BattleEffect.status(target, "Ward"));
        }
        int damage = target.takeDamage(incoming);
        effects.add(new BattleEffect(target, -damage));
        if (!target.isAlive()) { clearKnockedOutHeroEffects(target); }
        if (damage > 0 && target.isAlive() && "courage_charm".equals(accessory)
                && target.getHp() * 2L <= target.getMaxHp() && random.nextDouble() < 0.20) {
            int healed = target.heal((target.getMaxHp() + 19) / 20);
            if (healed > 0) { effects.add(BattleEffect.status(target, "Courage")); effects.add(new BattleEffect(target, healed)); }
        }
        if (damage > 0 && "blade_mail".equals(armor)) {
            effects.add(BattleEffect.status(target, "Reflect"));
            int reflectedPower = (int)Math.min(Integer.MAX_VALUE, Math.ceil(damage * 0.50 * (1.0 + regenerationRate) - 1e-9));
            int reflected = enemy.takeDamage(reflectedPower);
            if (reflected > 0) { effects.add(new BattleEffect(enemy, -reflected)); applyManaLeak(hasManaLeak(target), enemy, reflected); }
        }
        if (damage > 0 && target.isAlive() && "ethereal_robe".equals(armor)) target.chargeEthereal();
        if (damage > 0 && target.isAlive() && "ring_of_power".equals(target.getEquipmentId(Equipment.Slot.ACCESSORY))
                && random.nextDouble() < 0.5) { apRefills.add(target); }
        return damage;
    }

    private boolean validAlly(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    private boolean validEnemy(Enemy target) { return target != null && enemies.contains(target) && target.isAlive(); }
    /** Applies proportional defense; negative defense increases damage, capped at -20. */
    public static int damageAfterDefense(int power, int defense) {
        return Math.max(MINIMUM_DAMAGE, (int)Math.ceil(Math.max(0, power) * 100.0 / (100 + Math.max(-20, defense))));
    }
    private int hurt(Enemy target, int attack) {
        return hurt(target, attack, false);
    }
    private int hurt(Enemy target, int attack, boolean guaranteedHit) {
        lastHeroHitTarget = target; lastHeroHitDamage = 0;
        if (!guaranteedHit && !hits(getCurrent(), target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        int defense = target.getDefense();
        Student actor = getCurrent();
        if (defense > 0 && actor != null && "demon_hunter".equals(actor.getEquipmentId(Equipment.Slot.WEAPON))) {
            defense -= (int)Math.ceil(defense * 0.25);
        }
        boolean shielded = target.getStoneSkinHits() > 0;
        int damage = target.takeDamage(damageAfterDefense(attack, defense) + random.nextInt(DAMAGE_VARIATION));
        reflectStoneSkin(shielded, actor, damage);
        lastHeroHitDamage = damage;
        effects.add(new BattleEffect(target, -damage));
        applyManaLeak(hasManaLeak(getCurrent()), target, damage);
        return damage;
    }
    /** Stone Skin returns 10% of actual damage without triggering further reflection. */
    private void reflectStoneSkin(boolean shielded, GameCharacter source, int damage) {
        if (!shielded || damage <= 0 || !(source instanceof Student hero) || !hero.isAlive()) return;
        int reflected = hero.takeDamage((int)(((long)damage + 9) / 10));
        if (reflected > 0) {
            effects.add(BattleEffect.status(hero, "Reflect"));
            effects.add(new BattleEffect(hero, -reflected));
            if (!hero.isAlive()) { clearKnockedOutHeroEffects(hero); if (getCurrent() == hero) turn = -1; }
        }
    }
    private static boolean hasManaLeak(Student student) {
        return student != null && "looper_ring".equals(student.getEquipmentId(Equipment.Slot.ACCESSORY));
    }
    private void applyManaLeak(boolean enabled, Enemy target, int damage) {
        if (!enabled || damage <= 0 || target.getSp() <= 0) return;
        int amount = (int) Math.ceil(target.getMaxSp() * (5 + random.nextInt(6)) / 100.0);
        int drained = target.loseSp(amount);
        if (drained > 0) effects.add(BattleEffect.manaLeak(target, drained));
    }
    private String defeated(Enemy target) { return target.isAlive() ? "" : " " + target.getName() + " is defeated."; }
    private void clearGuards() {
        guards.clear();
        for (Student student : party) student.endDefend();
    }
    private void finish(String action) {
        message = action;
        if (hasPendingLooperHits()) return;
        if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); return; }
        turn = -1;
    }
}
