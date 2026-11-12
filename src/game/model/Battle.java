package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Controls an encounter, independent of the Swing interface. */
public final class Battle {
    private static final int KISS_HEAL = 28;
    private static final int ARCANE_BURST_BONUS = 4;
    private static final int CADET_STRIKE_BONUS = 9;
    private static final int TWIN_SHOT_BONUS = 4;
    private static final int DAMAGE_VARIATION = 5;
    private static final int MINIMUM_DAMAGE = 1;
    private final List<Student> party = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random;
    private final java.util.Map<Student, Student> guards = new java.util.HashMap<>();
    private Student blockingStudent, protectedStudent, lastDamageRecipient;
    private final double accuracyScalingFactor;
    private final GameProgress progress;
    private final List<BattleEffect> effects = new ArrayList<>();
    private final java.util.Set<Student> apRefills = new java.util.HashSet<>();
    private final java.util.List<DamageOverTime> burns = new java.util.ArrayList<>();
    private static final class DamageOverTime {
        final Enemy target; final int total; double elapsed; int ticks;
        DamageOverTime(Enemy target, int damage) { this.target = target; total = damage; }
    }
    private int turn = -1;
    private boolean pendingSecondCast;
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
        if (encounter < 0 || encounter > 4 || encounter > progress.getEncounter()) {
            throw new IllegalArgumentException("This encounter is not available yet");
        }
        this.progress = progress;
        this.random = random;
        this.accuracyScalingFactor = 100.0 * java.util.Objects.requireNonNull(difficulty).multiplier();
        party.addAll(progress.getParty());
        for (Student student : party) { student.clearDefense(); }
        party.sort(Comparator.comparingDouble(Student::getEffectiveSpeed).reversed());
        if (party.stream().noneMatch(Student::isAlive)) {
            throw new IllegalArgumentException("At least one student must be able to battle");
        }
        double averageLevel = party.stream().mapToInt(Student::getLevel).average().orElse(1.0);
        int count = EncounterRules.rollMonsterCount(encounter, random);
        boolean finalEncounter = encounter == GameProgress.FINAL_BOSS_ENCOUNTER;
        ambushed = ambushed && !finalEncounter;
        int sentinels = finalEncounter ? 5 : encounter == 3 ? 1 + random.nextInt(3) : encounter >= 1 ? 1 : 0;
        int wisps = count - sentinels - (finalEncounter ? 1 : 0);
        for (int index = 0; index < wisps; index++) {
            enemies.add(new Enemy("Library Wisp " + (char) ('A' + index), false, false, difficulty, encounter, averageLevel));
        }
        for (int index = 0; index < sentinels; index++) {
            String name = sentinels == 1 ? "Stone Sentinel" : "Stone Sentinel " + (char) ('A' + index);
            enemies.add(new Enemy(name, true, false, difficulty, encounter, averageLevel));
        }
        if (finalEncounter) { enemies.add(new Enemy("Crystal Guardian", true, true, difficulty, encounter, averageLevel)); }
        message = finalEncounter ? "The Crystal Guardian and five Stone Sentinels appear!"
                : sentinels > 0 ? sentinels + " Stone Sentinel(s)" + (wisps > 0 ? " and " + wisps + " Library Wisp(s) appear!" : " appear!")
                : count + " Library Wisp(s) appear! Choose an action.";
        if (ambushed) { message = "You have been ambushed!\n" + message; }
        enemies.sort(Comparator.comparingInt(Enemy::getSpeed).reversed());
    }
    /** Returns party in speed order.
     * @return party in speed order
     */ public List<Student> getParty() { return List.copyOf(party); }
    /** Returns enemies in speed order.
     * @return enemies in speed order
     */ public List<Enemy> getEnemies() { return List.copyOf(enemies); }
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
    /** Advances active Light Saber effects by elapsed battle time, including command selection. */
    public boolean advanceDamageOverTime(double seconds) {
        if (seconds <= 0 || isOver() || burns.isEmpty()) return false;
        boolean changed = false;
        for (DamageOverTime burn : burns) {
            Enemy enemy = burn.target;
            if (!enemy.isAlive()) continue;
            burn.elapsed += seconds;
            int due = Math.min(5, (int) Math.floor(burn.elapsed + 1e-9));
            while (burn.ticks < due && enemy.isAlive()) {
                int before = (int) ((long) burn.total * burn.ticks / 5);
                burn.ticks++;
                int damage = (int) ((long) burn.total * burn.ticks / 5) - before;
                int taken = enemy.takeDamage(damage);
                if (taken > 0) {
                    if (!changed) effects.clear();
                    effects.add(new BattleEffect(enemy, -taken)); changed = true;
                }
            }
        }
        burns.removeIf(burn -> !burn.target.isAlive() || burn.ticks >= 5);
        if (changed) {
            message = isVictory() ? "Light Saber damage defeats the last enemy! Victory!" : "Light Saber damage strikes!";
            if (isVictory()) clearGuards();
        }
        return changed;
    }
    private void applyLightSaber(Student actor, Enemy target, int damage) {
        if (damage > 0 && target.isAlive() && "light_saber".equals(actor.getEquipmentId(Equipment.Slot.WEAPON))) {
            burns.add(new DamageOverTime(target, damage));
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
        if (isOver() || getCurrent() != null || index < 0 || !student.isAlive()) { return false; }
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
     */ public boolean isVictory() { return enemies.stream().noneMatch(Enemy::isAlive); }
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
        if (pendingSecondCast || isOver() || getCurrent() == null || !validEnemy(target)) { return false; }
        effects.clear();
        Student actor = getCurrent();
        int attackPower = actor.getAttack() * (actor.consumeEtherealCharge() ? 4 : 1);
        if (actor.getId() == Student.ANGEL) {
            int hits = (enemies.stream().filter(Enemy::isAlive).count() == 1 ? 2 : 3)
                    + ("giant_slayer_gloves".equals(actor.getEquipmentId(Equipment.Slot.WEAPON)) ? 1 : 0);
            StringBuilder text = new StringBuilder(actor.getName() + " attacks!");
            Enemy currentTarget = target;
            for (int hit = 0; hit < hits; hit++) {
                if (hit > 0) {
                    List<Enemy> living = enemies.stream().filter(Enemy::isAlive).toList();
                    if (living.isEmpty()) { break; }
                    currentTarget = living.get(random.nextInt(living.size()));
                }
                int damage = hurt(currentTarget, attackPower);
                text.append("\n").append(currentTarget.getName()).append(" takes ")
                        .append(damage).append(" damage.").append(defeated(currentTarget));
            }
            finish(text.toString());
            return true;
        }
        int damage = hurt(target, attackPower);
        applyLightSaber(actor, target, damage);
        finish(actor.getName() + " attacks " + target.getName() + " for " + damage + " damage." + defeated(target));
        return true;
    }
    /** Uses the current student's special ability.
     * @param ally healing target for Angel
     * @param enemy enemy target for single-target abilities
     * @return whether the action was performed
     */
    public boolean ability(Student ally, Enemy enemy) {
        if (pendingSecondCast || isOver() || getCurrent() == null) { return false; }
        Student actor = getCurrent();
        if (actor.getSp() < actor.getAbilitySpCost()) { return false; }
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS && !validAlly(ally)) { return false; }
        if (actor.getAbilityType() != Student.AbilityType.HEALING_KISS
                && actor.getAbilityType() != Student.AbilityType.ARCANE_BURST && !validEnemy(enemy)) { return false; }
        if (!actor.spendAbilitySp()) { return false; }
        String skillName = switch (actor.getAbilityType()) {
            case ARCANE_BURST -> "Arcane Burst"; case CADET_STRIKE -> "Cadet Strike";
            case TWIN_SHOT -> "Twin Shot"; case HEALING_KISS -> "Healing Kiss";
        };
        lastAbility = new AbilityAnnouncement(actor.getName(), skillName);
        if (actor.getAbilityType() == Student.AbilityType.HEALING_KISS) {
            if (!validAlly(ally)) { return false; }
            effects.clear();
            int healed = ally.heal(KISS_HEAL);
            if (healed > 0) { effects.add(new BattleEffect(ally, healed)); }
            finish("Angel uses Healing Kiss. " + ally.getName() + " recovers " + healed + " HP.");
        } else if (actor.getAbilityType() == Student.AbilityType.ARCANE_BURST) {
            boolean doubleCast = "great_sage_staff".equals(actor.getEquipmentId(Equipment.Slot.WEAPON)) && random.nextDouble() < 0.5;
            String text = arcaneBurst(actor, "Zack uses Arcane Burst!");
            if (doubleCast && !isVictory()) {
                pendingSecondCast = true;
                message = text;
            } else {
                finish(text);
            }
        } else {
            if (!validEnemy(enemy)) { return false; }
            effects.clear();
            int damage = hurt(enemy, actor.getAttack() + (actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? CADET_STRIKE_BONUS : TWIN_SHOT_BONUS));
            applyLightSaber(actor, enemy, damage);
            if (actor.getAbilityType() == Student.AbilityType.TWIN_SHOT && enemy.isAlive()) { damage += hurt(enemy, actor.getAttack()); }
            String ability = actor.getAbilityType() == Student.AbilityType.CADET_STRIKE ? "Cadet Strike" : "Twin Shot";
            finish(actor.getName() + " uses " + ability + " on " + enemy.getName() + " for " + damage + " damage." + defeated(enemy));
        }
        return true;
    }
    private String arcaneBurst(Student actor, String heading) {
        effects.clear();
        StringBuilder text = new StringBuilder(heading);
        for (Enemy target : enemies) {
            if (target.isAlive()) {
                int damage = hurt(target, actor.getAttack() + ARCANE_BURST_BONUS);
                text.append("\n").append(target.getName()).append(" takes ").append(damage).append(" damage.").append(defeated(target));
            }
        }
        return text.toString();
    }
    /** Whether the Great Sage Staff has a follow-up cast waiting to resolve. */
    public boolean hasPendingSecondCast() { return pendingSecondCast; }
    /** Applies the delayed follow-up without spending extra SP or an extra action. */
    public boolean completeSecondCast() {
        if (!pendingSecondCast) return false;
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
        if (pendingSecondCast || isOver() || getCurrent() == null || !validAlly(target)) { return false; }
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
        if (pendingSecondCast || isOver() || getCurrent() == null || target == null || !party.contains(target) || target.isAlive()) return false;
        int restored = progress.getInventory().useWhiteFlower(target);
        if (restored <= 0) return false;
        effects.clear(); effects.add(new BattleEffect(target, restored));
        finish(getCurrent().getName() + " uses White Flower. " + target.getName() + " returns with " + restored + " HP.");
        return true;
    }
    /** Whether a living party member can be protected by the current actor. */
    public boolean canDefend(Student target) {
        Student actor = getCurrent();
        return actor != null && validAlly(target) && (target != actor || party.stream().filter(Student::isAlive).count() == 1);
    }
    /** Protects one ally until the defender is selected for their next action. */
    public boolean defend(Student target) {
        if (pendingSecondCast || isOver() || !canDefend(target)) return false;
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
        if (isOver() || getCurrent() != null || !validEnemy(enemy)) { return false; }
        effects.clear(); blockingStudent = null; protectedStudent = null; lastAbility = null;
        List<Student> living = party.stream().filter(Student::isAlive).toList();
        boolean skill = enemy.usesSkillNext();
        enemy.recordAction();
        Enemy healTarget = enemy.isSentinel() && !enemy.isBoss() && enemy.getSp() >= enemy.getHealSpCost()
                ? enemies.stream().filter(other -> other != enemy && other.isAlive()
                        && other.getHp() * 4 <= other.getMaxHp() * 3)
                    .min(java.util.Comparator.comparingDouble(other -> (double) other.getHp() / other.getMaxHp())).orElse(null)
                : null;
        if (healTarget != null && enemy.spendHealSp()) {
            lastAbility = new AbilityAnnouncement(enemy.getName(), "Stone Mend");
            int healed = healTarget.heal(enemy.healingPower(healTarget));
            effects.add(new BattleEffect(healTarget, healed));
            message = enemy.getName() + " uses Stone Mend and heals " + healTarget.getName() + " for " + healed + " HP.";
        } else if (skill && enemy.isSentinel()) {
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
            int damage = enemyDamage(enemy, target, skill ? 4 : 0, 1);
            Student recipient = lastDamageRecipient;
            message = skill ? enemy.getName() + " uses Arcane Spark on " + target.getName() + "."
                    : enemy.getName() + " attacks " + target.getName() + ".";
            if (recipient != target) message += " " + recipient.getName() + " protects " + target.getName() + ".";
            message += " " + recipient.getName() + " takes " + damage + " damage.";
            if (!recipient.isAlive()) { message += " " + recipient.getName() + " is knocked out."; }
        }
        if (isDefeat()) { message += "\nThe party is defeated."; clearGuards(); }
        else if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); }
        return true;
    }

    /** Chance to hit, based on the defender's agility and attacker's accuracy. */
    public static double hitChance(double agility, double accuracy, double scalingFactor) {
        if (!Double.isFinite(agility) || !Double.isFinite(accuracy) || agility < 0 || accuracy <= 0 || !Double.isFinite(scalingFactor) || scalingFactor <= 0)
            throw new IllegalArgumentException("Invalid accuracy parameters");
        double dodge = agility / (agility + accuracy / scalingFactor);
        return Math.max(0.0, Math.min(1.0, (accuracy - dodge) / accuracy));
    }
    private boolean hits(GameCharacter attacker, GameCharacter defender) {
        return random.nextDouble() < hitChance(defender.getEffectiveAgility(), attacker.getEffectiveAccuracy(), accuracyScalingFactor);
    }
    private int enemyDamage(Enemy enemy, Student target, int bonus, int multiplier) {
        Student intended = target;
        Student protector = party.stream().filter(candidate -> candidate != intended && candidate.isDefending()
                && candidate.isAlive() && guards.get(candidate) == intended).findFirst().orElse(null);
        if (protector != null) {
            target = protector; blockingStudent = protector; protectedStudent = intended;
            effects.add(BattleEffect.status(protector, "Guard"));
        }
        lastDamageRecipient = target;
        if (!hits(enemy, target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        String armor = target.getEquipmentId(Equipment.Slot.ARMOR);
        if ("ultra_instinct_shirt".equals(armor) && random.nextDouble() < 0.5) {
            effects.add(BattleEffect.status(target, "Dodge"));
            return 0;
        }
        int damage = target.takeDamage(enemy.scaleDamage(Math.max(MINIMUM_DAMAGE,
                enemy.getAttack() + bonus - target.getDefense() + random.nextInt(DAMAGE_VARIATION))) * multiplier);
        effects.add(new BattleEffect(target, -damage));
        if (!target.isAlive()) { guards.remove(target); target.endDefend(); }
        if (damage > 0 && "blade_mail".equals(armor)) {
            effects.add(BattleEffect.status(target, "Reflect"));
            int reflected = enemy.takeDamage((int) Math.round(damage * 0.5));
            if (reflected > 0) effects.add(new BattleEffect(enemy, -reflected));
        }
        if (damage > 0 && target.isAlive() && "ethereal_robe".equals(armor)) target.chargeEthereal();
        if (damage > 0 && target.isAlive() && "ring_of_power".equals(target.getEquipmentId(Equipment.Slot.ACCESSORY))
                && random.nextDouble() < 0.5) { apRefills.add(target); }
        return damage;
    }

    private boolean validAlly(Student target) { return target != null && party.contains(target) && target.isAlive(); }
    private boolean validEnemy(Enemy target) { return target != null && enemies.contains(target) && target.isAlive(); }
    private int hurt(Enemy target, int attack) {
        if (!hits(getCurrent(), target)) { effects.add(BattleEffect.status(target, "Dodge")); return 0; }
        int damage = target.takeDamage(Math.max(MINIMUM_DAMAGE, attack - target.getDefense() + random.nextInt(DAMAGE_VARIATION)));
        effects.add(new BattleEffect(target, -damage));
        return damage;
    }
    private String defeated(Enemy target) { return target.isAlive() ? "" : " " + target.getName() + " is defeated."; }
    private void clearGuards() {
        guards.clear();
        for (Student student : party) student.endDefend();
    }
    private void finish(String action) {
        message = action;
        if (isVictory()) { message += "\nAll enemies defeated! Victory!"; clearGuards(); return; }
        turn = -1;
    }
}
