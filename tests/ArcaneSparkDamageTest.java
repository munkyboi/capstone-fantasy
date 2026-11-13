import game.model.*;
import java.util.*;

/** Checks Arcane Spark damage ranges across difficulties. */
public final class ArcaneSparkDamageTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static int thirdHit(GameSettings.Difficulty difficulty, int variation) {
        Random random = new Random() {
            @Override public int nextInt(int bound) { return bound == 21 ? variation : 0; }
            @Override public double nextDouble() { return .5; }
        };
        GameProgress p = new GameProgress(List.of(Student.ZACK));
        Battle battle = new Battle(p, random, false, 0, difficulty);
        Student target = p.getParty().get(0); Enemy wisp = battle.getEnemies().get(0);
        for (int i = 0; i < 2; i++) { check(battle.enemyAttack(wisp), "normal attack"); target.heal(9999); }
        int before = target.getHp(); check(battle.enemyAttack(wisp), "Arcane Spark action");
        check(battle.getLastAbility() != null && "Arcane Spark".equals(battle.getLastAbility().skill()), "skill triggers");
        return before - target.getHp();
    }
    public static void main(String[] args) {
        int[][] ranges = {{6,8},{8,10},{12,15},{16,20}};
        GameSettings.Difficulty[] difficulties = GameSettings.Difficulty.values();
        for (int i=0;i<difficulties.length;i++) {
            check(thirdHit(difficulties[i],0)==ranges[i][0], difficulties[i] + " minimum");
            check(thirdHit(difficulties[i],20)==ranges[i][1], difficulties[i] + " maximum");
        }
        System.out.println("Arcane Spark difficulty damage checks passed");
    }
}
