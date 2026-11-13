import game.model.*;
import java.util.*;

/** The shared hit/dodge factor scales with starting party size and difficulty. */
public final class PartySizeScalingTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        double[] multipliers = {3.0, 2.0, 1.5, 1.0};
        for (int size = 1; size <= 4; size++) {
            List<Integer> ids = java.util.stream.IntStream.range(0, size).boxed().toList();
            for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) {
                Battle battle = new Battle(new GameProgress(ids), new Random(1), false, 0, difficulty);
                double expected = 100.0 * difficulty.multiplier() * multipliers[size - 1];
                check(Math.abs(battle.getAccuracyScalingFactor() - expected) < 1e-9,
                        "factor for " + size + " and " + difficulty);
            }
        }
        GameProgress p = new GameProgress(List.of(0, 1, 2));
        p.getParty().get(0).takeDamage(9999);
        Battle battle = new Battle(p, new Random(1), false, 0);
        check(battle.getAccuracyScalingFactor() == 150.0, "party size includes fallen members");
        Student hero = battle.getParty().stream().filter(Student::isAlive).findFirst().orElseThrow();
        Enemy enemy = battle.getEnemies().get(0);
        double factor = battle.getAccuracyScalingFactor();
        check(Battle.hitChance(enemy.getEffectiveAgility(), hero.getEffectiveAccuracy(), factor)
                        > Battle.hitChance(enemy.getEffectiveAgility(), hero.getEffectiveAccuracy(), 100),
                "larger shared factor raises hero hit chance");
        check(Battle.hitChance(hero.getEffectiveAgility(), enemy.getEffectiveAccuracy(), factor)
                        > Battle.hitChance(hero.getEffectiveAgility(), enemy.getEffectiveAccuracy(), 100),
                "larger shared factor raises enemy hit chance");
        for (int size : new int[] {1, 4}) {
            List<Integer> ids = java.util.stream.IntStream.range(0, size).boxed().toList();
            Random fixed = new Random() {
                @Override public int nextInt(int bound) { return bound == 4 ? 1 : 0; }
                @Override public double nextDouble() { return 0.6; }
            };
            Battle actual = new Battle(new GameProgress(ids), fixed);
            Student zack = actual.getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
            Enemy wisp = actual.getEnemies().get(0);
            int enemyHp = wisp.getHp(); check(actual.selectStudent(zack) && actual.attack(wisp), "hero attack");
            check((wisp.getHp() < enemyHp) == (size == 1), "shared scaling affects hero hit roll");
            int heroHp = zack.getHp(); check(actual.enemyAttack(wisp), "enemy attack");
            check((zack.getHp() < heroHp) == (size == 1), "shared scaling affects enemy hit roll");
        }
        System.out.println("Party-size scaling checks passed");
    }
}
