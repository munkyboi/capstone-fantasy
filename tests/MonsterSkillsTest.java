import game.model.*;
import java.util.List;
import java.util.Random;

/** Exercises enemy skill cadence, targeting, defense and battle effects. */
public final class MonsterSkillsTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    private static Random fixed() { return new Random() { @Override public int nextInt(int bound) { return 0; } @Override public double nextDouble() { return 0; } }; }
    public static void main(String[] args) throws Exception {
        Battle wispBattle = new Battle(List.of(Student.ZACK, Student.BOGART, Student.ANGEL), fixed());
        Enemy wisp = wispBattle.getEnemies().get(0);
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "first Wisp action is normal");
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "second Wisp action is normal");
        check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("Arcane Spark"), "third Wisp action uses skill");
        check(wispBattle.getEffects().size() == 1 && wispBattle.getEffects().get(0).getAmount() < 0,
                "Arcane Spark produces one damage indicator");
        check(wisp.getSp()==68 && wispBattle.hasPendingSparkExplosion(), "spark spends SP and queues explosion");
        check(wispBattle.resolveSparkExplosion(), "resolve explosion");
        for(int i=0;i<3;i++){ wispBattle.getParty().forEach(s -> s.heal(999)); check(wispBattle.enemyAttack(wisp) && wispBattle.getMessage().contains("attacks"), "cooldown action " + i); }
        wispBattle.getParty().forEach(s -> s.heal(999)); check(wispBattle.enemyAttack(wisp) && wisp.getSp()==56 && wispBattle.getMessage().contains("Arcane Spark"), "skill returns after three actions");

        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL)); progress.advance();
        Battle battle = new Battle(progress, fixed());
        Enemy sentinel = Enemy.sentinel("Attack Sentinel", true, GameSettings.Difficulty.NORMAL, 0, 1, 0);
        var field = Battle.class.getDeclaredField("enemies"); field.setAccessible(true);
        @SuppressWarnings("unchecked") List<Enemy> enemies = (List<Enemy>) field.get(battle);
        enemies.clear(); enemies.add(sentinel);
        Student defender = battle.getParty().get(0), protectedAlly = battle.getParty().get(1);
        check(battle.selectStudent(defender) && battle.defend(protectedAlly), "select an ally to guard");
        int before = defender.getHp(), allyBefore = protectedAlly.getHp();
        check(battle.enemyAttack(sentinel) && battle.getMessage().contains("Stone Sweep"), "attack sentinel uses sweep");
        check(protectedAlly.getHp() == allyBefore && battle.getEffects().stream().anyMatch(e -> "Guard".equals(e.getLabel())),
                "defender intercepts sweep aimed at the protected ally");
        int expected = Battle.damageAfterDefense((int)Math.ceil(sentinel.getAttack() * .6), 0);
        check(before - defender.getHp() == expected * 2, "defender receives both piercing sweep hits");
        sentinel.takeDamage(999);
        check(!battle.enemyAttack(sentinel), "defeated enemies cannot act");
        System.out.println("Monster skill checks passed");
    }
}
