import game.model.*;
import java.util.List;
import java.util.Random;

/** Checks that later encounters scale consistently with difficulty and rewards. */
public final class StageBalanceTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        int[] hp = {65, 65, 70, 75, 81};
        int[] attack = {16, 16, 17, 18, 19};
        int[] xp = {20, 20, 23, 26, 30};
        int[] petot = {12, 12, 15, 18, 16};
        for (int stage = 0; stage < 5; stage++) {
            Enemy wisp = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, stage);
            check(wisp.getMaxHp() == hp[stage] && wisp.getAttack() == attack[stage]
                    && wisp.getExperienceDrop() == xp[stage] && wisp.getPetotDrop() == petot[stage], "stage " + (stage + 1));
            Enemy hard = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.HARD, stage);
            check(hard.getMaxHp() == Math.round(65 * new double[]{1, 1, 1.08, 1.16, 1.25}[stage] * 1.5)
                    && hard.getExperienceDrop() == Math.round(xp[stage] * 1.5), "hard difficulty stage " + (stage + 1));
        }
        GameProgress party = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
        for (int i = 0; i < 4; i++) party.advance();
        Battle finalBattle = new Battle(party, new Random(4), false, 4);
        check(finalBattle.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum() == 50195, "boss battle Petot");
        check(finalBattle.getEnemies().stream().mapToInt(Enemy::getExperienceDrop).sum() == 10225, "boss battle XP before party multiplier");
        check(finalBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow().getMaxHp() == 1806, "boss health for three heroes");
        System.out.println("Stage balance checks passed");
    }
}
