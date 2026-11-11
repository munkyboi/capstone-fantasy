import game.model.*;
import java.util.List;
import java.util.Random;

/** Exercises enemy skill cadence, targeting, defense and battle effects. */
public final class MonsterSkillsTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    private static Random fixed() { return new Random() { @Override public int nextInt(int bound) { return 0; } }; }
    public static void main(String[] args) {
        Battle wispBattle = new Battle(List.of(Student.ZACK, Student.BOGART, Student.ANGEL), fixed());
        Enemy wisp = wispBattle.getEnemies().get(0);
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "first Wisp action is normal");
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "second Wisp action is normal");
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("Arcane Spark"), "third Wisp action uses skill");
        check(wispBattle.getEffects().size() == 1 && wispBattle.getEffects().get(0).getAmount() < 0,
                "Arcane Spark produces one damage indicator");
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "cadence resets after skill");

        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL)); progress.advance();
        Battle battle = new Battle(progress, fixed());
        Enemy sentinel = battle.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Student defender = battle.getParty().get(0);
        check(battle.enemyAttack(sentinel) && battle.enemyAttack(sentinel), "Sentinel uses normal attacks first");
        check(battle.selectStudent(defender), "select defender");
        check(!battle.enemyAttack(sentinel), "enemy cannot act while a student is selected");
        battle.defend();
        int before = defender.getHp();
        check(battle.enemyAttack(sentinel) && battle.getMessage().contains("Stone Sweep"), "third action uses sweep");
        check(battle.getEffects().size() == 3 && battle.getEffects().stream().allMatch(e -> e.getAmount() < 0),
                "sweep applies effects to all living students");
        int expected = Math.max(1, sentinel.getAttack() - 5 - defender.getDefense()) / 2;
        check(before - defender.getHp() == Math.max(1, expected), "defend reduces sweep damage");
        sentinel.takeDamage(999);
        check(!battle.enemyAttack(sentinel), "defeated enemies cannot act");
        System.out.println("Monster skill checks passed");
    }
}
