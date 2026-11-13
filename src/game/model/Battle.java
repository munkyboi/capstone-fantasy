package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls an encounter, independent of the Swing interface. */
public final class Battle {
    private static final int DAMAGE_VARIATION = 5;
    private static final int MINIMUM_DAMAGE = 1;
    static final double GUARDIAN_PULSE_STUN_CHANCE = 0.40;
    static final double GUARDIAN_PULSE_LOOP_CHANCE = 0.45;
    private final List<Student> party = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> defeatedEarlier = new ArrayList<>();
    private final GameSettings.Difficulty encounterDifficulty;
    private final double encounterPartyLevel;
    private final Random random;
    private final java.util.Map<Student, Student> guards = new java.util.HashMap<>();
    private Student blockingStudent, protectedStudent, lastDamageRecipient;
    private final double accuracyScalingFactor;
    private final double heroAccuracyScalingFactor;
    private final GameProgress progress;
    private final List<BattleEffect> effects = new ArrayList<>();
    private final java.util.Map<Student, Integer> stuns = new java.util.HashMap<>();
    private final boolean merchantEncounter;
    private final java.util.Set<Student> apRefills = new java.util.HashSet<>();
    private final BattleDotEngine dots = new BattleDotEngine(this);
    private int turn = -1;
    private BattleHeroActions heroActions;
    private BattleEnemyDamage enemyDamageResolver;
    private Enemy sparkCaster;
    private Student sparkTarget;
    /** Returns the targets selected for the latest Rain of Arrows animation.
     * @return the targets selected for the latest Rain of Arrows animation
     */
    public List<Enemy> getRainArrowTargets() { return heroActions.getRainArrowTargets(); }
    private Enemy pulseCaster;
    private int pulseLoops;
    private final java.util.Map<Student, Integer> pulseInitial = new java.util.HashMap<>(), pulsePrevious = new java.util.HashMap<>();
    private BattleEscape escape;
    private BattleItems items;
    /** Returns whether the party is attempting to escape.
     * @return whether the party is attempting to escape
     */
    public boolean isEscaping() { return escape.isEscaping(); }
    /** Returns the student leading the escape, if any.
     * @return the student leading the escape, if any
     */
    public Student getEscapeInitiator() { return escape.initiator(); }
    /** Returns the remaining action cycles before escape.
     * @return the remaining action cycles before escape
     */
    public int getEscapeTurnsRemaining() { return escape.turnsRemaining(); }
    /** Starts a three-turn escape; selecting the command consumes the first turn.
     * @return the result of begin escape
     */
    public boolean beginEscape() { return escape.begin(); }
    /** Cancels escape after the initiator is knocked out.
     * @return whether cancelled
     */
    public boolean cancelEscapeIfKnockedOut() { return escape.cancelIfKnockedOut(); }
    /** Advances an escape cycle.
     * @param student acting hero
     * @return whether escaped
     */
    public boolean advanceEscape(Student student) { return escape.advance(student); }
    /** Returns whether an Arcane Spark explosion is waiting.
     * @return whether an Arcane Spark explosion is waiting
     */
    public boolean hasPendingSparkExplosion() { return sparkCaster != null; }
    /** Returns whether Looper Ring hits are waiting.
     * @return whether Looper Ring hits are waiting
     */
    public boolean hasPendingLooperHits() { return heroActions.hasPendingLooperHits(); }
    /** Returns whether Angel has combo hits or an explosion waiting.
     * @return whether Angel has combo hits or an explosion waiting
     */
    public boolean hasPendingAngelAttack() { return heroActions.hasPendingAngelAttack(); }
    /** Returns whether Angel’s combo explosion is waiting.
     * @return whether Angel\u2019s combo explosion is waiting
     */
    public boolean isAngelExplosionPending() { return heroActions.isAngelExplosionPending(); }
    /** Returns whether a Crystal Pulse loop is waiting.
     * @return whether a Crystal Pulse loop is waiting
     */
    public boolean hasPendingGuardianLoop() { return pulseLoops > 0; }
    /** Returns whether a delayed combat action is still resolving.
     * @return whether a delayed combat action is still resolving
     */
    public boolean isActionAnimating() { return heroActions.hasPendingSecondCast() || hasPendingLooperHits() || hasPendingAngelAttack() || hasPendingSparkExplosion() || hasPendingTwinShot() || hasPendingGuardianLoop(); }
    /** Returns whether Charice has a follow-up shot waiting.
     * @return whether Charice has a follow-up shot waiting
     */
    public boolean hasPendingTwinShot() { return heroActions.hasPendingTwinShot(); }
    /** Returns the delay between Twin Shot hits in milliseconds.
     * @return the delay between Twin Shot hits in milliseconds
     */
    public int twinShotDelayMillis() { return 300; }
    /** Text identifying an ability as it is cast.
     * @param caster name of the caster
     * @param skill name of the ability
     */
    public record AbilityAnnouncement(String caster, String skill) { }
    private AbilityAnnouncement lastAbility;
    /** Returns the latest ability announcement, if any.
     * @return the latest ability announcement, if any
     */
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

    /** Starts a specific unlocked route encounter, including a replay.
     * @param progress current game progress
     * @param random random source
     * @param ambushed the ambushed
     * @param encounter encounter index
     */
    public Battle(GameProgress progress, Random random, boolean ambushed, int encounter) {
        this(progress, random, ambushed, encounter, GameSettings.Difficulty.NORMAL);
    }
    /** Starts an encounter using the selected monster difficulty.
     * @param progress current game progress
     * @param random random source
     * @param ambushed the ambushed
     * @param encounter encounter index
     * @param difficulty selected difficulty
     */
    public Battle(GameProgress progress, Random random, boolean ambushed, int encounter, GameSettings.Difficulty difficulty) {
        this(progress, random, ambushed, encounter, difficulty, false);
    }
    /** Creates the merchant encounter using the supplied progress and difficulty.
     * @param progress current game progress
     * @param random random source
     * @param difficulty selected difficulty
     * @return the result of merchant battle
     */
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
        this.heroActions = new BattleHeroActions(this, party, enemies, random, effects, dots);
        this.escape = new BattleEscape(this, party, progress);
        this.items = new BattleItems(this, progress, party, effects);
        this.enemyDamageResolver = new BattleEnemyDamage(this, party, guards, random, effects, apRefills);
        party.addAll(progress.getParty());
        double partyMultiplier = switch (party.size()) {
            case 1 -> 3.0; case 2 -> 2.0; case 3 -> 1.5; case 4 -> 1.0;
            default -> throw new IllegalArgumentException("Invalid party size");
        };
        this.accuracyScalingFactor = 100.0 * java.util.Objects.requireNonNull(difficulty).multiplier() * partyMultiplier;
        this.heroAccuracyScalingFactor = 100.0 * partyMultiplier / difficulty.multiplier();
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
        EncounterFactory.Encounter created = EncounterFactory.create(encounter, random, difficulty, averageLevel, party.size(), ambushed);
        enemies.addAll(created.enemies());
        message = created.message();
    }
    /** Returns the monster accuracy factor, fixed from difficulty and starting party size.
     * @return the monster accuracy factor, fixed from difficulty and starting party size
     */
    public double getAccuracyScalingFactor() { return accuracyScalingFactor; }
    /** Returns the difficulty and party-size accuracy factor for heroes.
     * @return the difficulty and party-size accuracy factor for heroes
     */
    public double getHeroAccuracyScalingFactor() { return heroAccuracyScalingFactor; }
    /** Returns whether this battle is against the merchant.
     * @return whether this battle is against the merchant
     */
    public boolean isMerchantEncounter() { return merchantEncounter; }
    /** Returns the remaining stun cycles for a student.
     * @param student student involved in the action
     * @return the remaining stun cycles for a student
     */
    public int getStunTurns(Student student) { return stuns.getOrDefault(student, 0); }
    /** Consumes one stun cycle when the student is ready.
     * @param student student involved in the action
     * @return the result of consume stun turn
     */
    public boolean consumeStunTurn(Student student) {
        int remaining = getStunTurns(student);
        if (remaining <= 0) return false;
        if (remaining == 1) stuns.remove(student); else stuns.put(student, remaining - 1);
        effects.clear(); effects.add(BattleEffect.status(student, "Stunned"));
        message = student.getName() + " is stunned and misses a turn.";
        return true;
    }
    void applyStun(Student student, int turns) {
        stuns.merge(student, turns, Math::max);
        guards.remove(student); student.endDefend();
        effects.add(BattleEffect.status(student, "Stunned"));
    }
    /** Returns party in speed order.
     * @return party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns enemies in speed order.
     * @return enemies in speed order
     */ public List<Enemy> getEnemies() { return List.copyOf(enemies); }
    /** All defeated waves remain eligible for victory rewards.
     * @return the reward enemies
     */
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
    /** Returns and clears equipment-triggered AP refills.
     * @return and clears equipment-triggered AP refills
     */
    public java.util.Set<Student> consumeApRefills() {
        java.util.Set<Student> result = java.util.Set.copyOf(apRefills); apRefills.clear(); return result;
    }
    /** Whether a living participant has any active damage-over-time effect.
     * @param target target of the action
     * @return whether a living participant has any active damage-over-time effect
     */
    public boolean hasDamageOverTime(GameCharacter target) { return dots.has(target); }
    void clearKnockedOutHeroEffects(Student hero) {
        dots.remove(hero); guards.remove(hero); hero.endDefend();
    }
    /** Applies one tick to the effects on a participant.
     * @param participant affected participant
     * @return whether an effect changed
     */
    public boolean tickDamageOverTime(GameCharacter participant) { return dots.tick(participant); }
    /** Advances damage-over-time effects by elapsed battle time.
     * @param seconds elapsed seconds
     * @return whether an effect changed
     */
    public boolean advanceDamageOverTime(double seconds) { return dots.advance(seconds); }
    /** Keeps DOT indicators when the participant also performs an action.
     * @param earlier the earlier
     */
    public void prependEffects(List<BattleEffect> earlier) { if (earlier != null && !earlier.isEmpty()) effects.addAll(0, earlier); }
    /** Displays all effects produced by simultaneous AP fills.
     * @param combined the combined
     */
    public void replaceEffects(List<BattleEffect> combined) { effects.clear(); effects.addAll(combined); }
    void applyLightSaber(Student actor, Enemy target, int damage) {
        if (damage > 0 && target.isAlive() && "light_saber".equals(actor.getEquipmentId(Equipment.Slot.WEAPON))) {
            dots.add(actor, target, damage, hasManaLeak(actor), 5, 0);
        }
    }
    /** Rolls once for each character whose AP has just filled.
     * @param students the students
     * @param keepCurrentEffects the keep current effects
     * @return the result of regenerate
     */
    public boolean regenerate(java.util.List<Student> students, boolean keepCurrentEffects) {
        BattleRegeneration.Result result = BattleRegeneration.attempt(students, party, random, effects, keepCurrentEffects);
        if (result.restored()) message = keepCurrentEffects ? message + "\n" + result.message() : result.message();
        return result.restored();
    }
    /** Selects a living student whose AP is ready.
     * @param student student chosen by the battle clock
     * @return true if selected
     */
    public boolean selectStudent(Student student) {
        int index = party.indexOf(student);
        if (isOver() || isEscaping() || isActionAnimating() || getCurrent() != null || index < 0 || !student.isAlive() || getStunTurns(student) > 0) { return false; }
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
    public boolean attack(Enemy target) { return heroActions.attack(target); }
    /** Uses a hero ability.
     * @param ally chosen ally
     * @param enemy chosen enemy
     * @return whether used
     */
    public boolean ability(Student ally, Enemy enemy) { return heroActions.ability(ally, enemy); }
    /** Advances Twin Shot.
     * @return whether advanced
     */
    public boolean advanceTwinShot() { return heroActions.advanceTwinShot(); }
    /** Advances Angel's combo.
     * @return whether advanced
     */
    public boolean advanceAngelAttack() { return heroActions.advanceAngelAttack(); }
    /** Advances a Looper Ring hit.
     * @return whether advanced
     */
    public boolean advanceLooperHit() { return heroActions.advanceLooperHit(); }
    /** Indicates a pending second cast.
     * @return whether pending
     */
    public boolean hasPendingSecondCast() { return heroActions.hasPendingSecondCast(); }
    /** Completes a second cast.
     * @return whether completed
     */
    public boolean completeSecondCast() { return heroActions.completeSecondCast(); }
    /** Uses a first-aid kit on a living student.
     * @param target student to heal
     * @return whether an item was used
     */
    public boolean useKit(Student target) { return items.useKit(target); }
    /** Uses a White Flower on a fallen hero.
     * @param target fallen hero
     * @return whether used
     */
    public boolean useWhiteFlower(Student target) { return items.useWhiteFlower(target); }
    /** Whether a living party member can be protected by the current actor.
     * @return whether a living party member can be protected by the current actor
     */
    public boolean canUseDefendOrEscape() {
        Student actor = getCurrent();
        return actor != null && !"heroes_ring".equals(actor.getEquipmentId(Equipment.Slot.ACCESSORY));
    }
    /** Victory Petot, including difficulty, optional greed, and Market Charm.
     * @param greed the greed
     * @return the result of victory petot
     */
    public int victoryPetot(boolean greed) {
        long amount = getRewardEnemies().stream().filter(Enemy::isDefeated).mapToLong(Enemy::getPetotDrop).sum();
        if (greed) amount *= 10;
        if (party.stream().anyMatch(s -> s.isAlive() && "market_charm".equals(s.getEquipmentId(Equipment.Slot.ACCESSORY)))) {
            amount = (amount * 11 + 9) / 10;
        }
        return (int)Math.min(GameProgress.MAX_PETOT, amount);
    }
    /** Returns whether the current student can guard the target.
     * @param target target of the action
     * @return whether the current student can guard the target
     */
    public boolean canDefend(Student target) {
        Student actor = getCurrent();
        return canUseDefendOrEscape() && validAlly(target) && (target != actor || party.stream().filter(Student::isAlive).count() == 1);
    }
    /** Protects one ally until the defender is selected for their next action.
     * @param target target of the action
     * @return the result of defend
     */
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
    /** Returns the student intercepting the current attack, if any.
     * @return the student intercepting the current attack, if any
     */
    public Student getBlockingStudent() { return blockingStudent; }
    /** Returns the ally protected from the current attack, if any.
     * @return the ally protected from the current attack, if any
     */
    public Student getProtectedStudent() { return protectedStudent; }
    /** Returns the ally guarded by the given student, if any.
     * @param defender the defender
     * @return the ally guarded by the given student, if any
     */
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
        BattleEnemyActions action = new BattleEnemyActions(this, party, enemies, defeatedEarlier, effects,
                random, encounterDifficulty, encounterPartyLevel, pulseInitial, pulsePrevious,
                pulseCaster, pulseLoops, sparkCaster, sparkTarget, message);
        boolean acted = action.act(enemy);
        message = action.message(); lastAbility = action.announcement();
        pulseCaster = action.pulseCaster(); pulseLoops = action.pulseLoops();
        sparkCaster = action.sparkCaster(); sparkTarget = action.sparkTarget();
        return acted;
    }

    Student lastDamageRecipient() { return lastDamageRecipient; }
    void removeDamageOverTime(GameCharacter target) { dots.remove(target); }
    void removeMirrorDamageOverTime() { dots.removeMirrors(); }
    void addEnemyDamageOverTime(Enemy source, Student target, int damage, int duration) {
        dots.add(source, target, damage, false, duration, 0);
    }

    /** Resolves one delayed Crystal Pulse loop against surviving original targets.
     * @return the result of resolve guardian loop
     */
    public boolean resolveGuardianLoop() {
        if (!hasPendingGuardianLoop()) return false;
        BattleEnemyFollowups.PulseResult result = BattleEnemyFollowups.pulse(this, party, effects, random, pulseInitial, pulsePrevious, pulseCaster, pulseLoops);
        message = result.message(); pulseLoops = result.loops(); pulseCaster = result.caster(); return true;
    }
    /** Resolves an Arcane Spark explosion.
     * @return whether resolved
     */
    public boolean resolveSparkExplosion() {
        if (!hasPendingSparkExplosion()) return false;
        Enemy caster = sparkCaster; Student target = sparkTarget; sparkCaster = null; sparkTarget = null;
        message = BattleEnemyFollowups.spark(this, party, effects, caster, target); return true;
    }

    /** Chance to hit; difficulty and party size scale accuracy, with dodge capped at 60%.
     * @param agility the agility
     * @param accuracy the accuracy
     * @param scalingFactor the scaling factor
     * @return the result of hit chance
     */
    public static double hitChance(double agility, double accuracy, double scalingFactor) {
        if (!Double.isFinite(agility) || !Double.isFinite(accuracy) || agility < 0 || accuracy <= 0 || !Double.isFinite(scalingFactor) || scalingFactor <= 0)
            throw new IllegalArgumentException("Invalid accuracy parameters");
        double dodge = agility / (agility + accuracy * scalingFactor / 100.0);
        return 1.0 - Math.min(0.60, dodge);
    }
    boolean hits(GameCharacter attacker, GameCharacter defender) {
        double factor = attacker instanceof Student ? heroAccuracyScalingFactor : accuracyScalingFactor;
        return random.nextDouble() < hitChance(defender.getEffectiveAgility(), attacker.getEffectiveAccuracy(), factor);
    }
    int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier) {
        return enemyDamage(enemy, target, bonus, multiplier, false);
    }
    int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier, boolean guaranteedHit) {
        return enemyDamage(enemy, target, bonus, multiplier, guaranteedHit, false);
    }
    int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier, boolean guaranteedHit, boolean ignoreDefense) {
        int damage = enemyDamageResolver.resolve(enemy, target, bonus, multiplier, guaranteedHit, ignoreDefense);
        lastDamageRecipient = enemyDamageResolver.lastDamageRecipient();
        if (enemyDamageResolver.blockingStudent() != null) {
            blockingStudent = enemyDamageResolver.blockingStudent();
            protectedStudent = enemyDamageResolver.protectedStudent();
        }
        return damage;
    }

    boolean validAlly(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    boolean validEnemy(Enemy target) { return target != null && enemies.contains(target) && target.isAlive(); }
    /** Applies proportional defense; negative defense increases damage, capped at -20.
     * @param power the power
     * @param defense the defense
     * @return the result of damage after defense
     */
    public static int damageAfterDefense(int power, int defense) {
        return Math.max(MINIMUM_DAMAGE, (int)Math.ceil(Math.max(0, power) * 100.0 / (100 + Math.max(-20, defense))));
    }
    int hurt(Enemy target, int attack) {
        return hurt(target, attack, false);
    }
    int hurt(Enemy target, int attack, boolean guaranteedHit) {
        heroActions.recordHit(target, 0);
        if (!guaranteedHit && !hits(getCurrent(), target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        int defense = target.getDefense();
        Student actor = getCurrent();
        if (defense > 0 && actor != null && "demon_hunter".equals(actor.getEquipmentId(Equipment.Slot.WEAPON))) {
            defense -= (int)Math.ceil(defense * 0.25);
        }
        boolean shielded = target.getStoneSkinHits() > 0;
        int damage = target.takeDamage(damageAfterDefense(attack, defense) + random.nextInt(DAMAGE_VARIATION));
        reflectStoneSkin(shielded, actor, damage);
        heroActions.recordHit(target, damage);
        effects.add(new BattleEffect(target, -damage));
        applyManaLeak(hasManaLeak(getCurrent()), target, damage);
        return damage;
    }
    /** Stone Skin returns 10% of actual damage without triggering further reflection. */
    void reflectStoneSkin(boolean shielded, GameCharacter source, int damage) {
        if (!shielded || damage <= 0 || !(source instanceof Student hero) || !hero.isAlive()) return;
        int reflected = hero.takeDamage((int)(((long)damage + 9) / 10));
        if (reflected > 0) {
            effects.add(BattleEffect.status(hero, "Reflect"));
            effects.add(new BattleEffect(hero, -reflected));
            if (!hero.isAlive()) { clearKnockedOutHeroEffects(hero); if (getCurrent() == hero) turn = -1; }
        }
    }
    static boolean hasManaLeak(Student student) {
        return student != null && "looper_ring".equals(student.getEquipmentId(Equipment.Slot.ACCESSORY));
    }
    void applyManaLeak(boolean enabled, Enemy target, int damage) {
        if (!enabled || damage <= 0 || target.getSp() <= 0) return;
        int amount = (int) Math.ceil(target.getMaxSp() * (5 + random.nextInt(6)) / 100.0);
        int drained = target.loseSp(amount);
        if (drained > 0) effects.add(BattleEffect.manaLeak(target, drained));
    }
    String defeated(Enemy target) { return target.isAlive() ? "" : " " + target.getName() + " is defeated."; }
    void clearGuards() {
        guards.clear();
        for (Student student : party) student.endDefend();
    }
    Random dotRandom() { return random; }
    List<BattleEffect> dotEffects() { return effects; }
    void dotKnockout(Student student) { guards.remove(student); student.endDefend(); if (getCurrent() == student) turn = -1; }
    void dotMessage(String value) { message = value; }
    void heroMessage(String text) { message = text; }
    void heroAnnouncement(AbilityAnnouncement value) { lastAbility = value; }
    void finish(String action) {
        message = action;
        if (hasPendingLooperHits()) return;
        if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); return; }
        turn = -1;
    }
}
