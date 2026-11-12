import game.model.*;
import java.util.List;
import java.util.Random;

/** Verifies level scaling and its interaction with difficulty and active parties. */
public final class EnemyLevelScalingTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        GameProgress p = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
        Student zack = p.getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
        Student bogart = p.getParty().stream().filter(s -> s.getId() == Student.BOGART).findFirst().orElseThrow();
        Student charice = p.getParty().stream().filter(s -> s.getId() == Student.CHARICE).findFirst().orElseThrow();
        zack.gainExperience(400); // Level 5; average level 3 with one other level-1 member.
        charice.gainExperience(1900); // A high-level reserve must not affect enemy scaling.
        p.setActiveParty(List.of(Student.ZACK, Student.BOGART));
        bogart.takeDamage(9999); // Knocked-out active members still count as party members.
        Battle first = new Battle(p, new Random(1), false, 0);
        Enemy wisp = first.getEnemies().get(0);
        check(wisp.getMaxHp() == 66 && wisp.getAttack() == 16 && wisp.getExperienceDrop() == 22,
                "active party average includes KO and excludes reserve");
        Enemy levelOne = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 1);
        check(levelOne.getMaxHp() == 65 && levelOne.getActionThreshold() == 24, "level-one balance stays unchanged");
        Enemy veteran = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 51);
        Enemy hard = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.HARD, 0, 51);
        check(veteran.getMaxHp() == 326 && veteran.getAttack() == 48 && veteran.getDefense() == 13
                && veteran.getActionThreshold() == 16 && veteran.getExperienceDrop() == 70, "level 51 scaling");
        check(hard.getMaxHp() == 489 && hard.getActionThreshold() == 11 && hard.getExperienceDrop() == 105,
                "difficulty stacks with party level");
        Enemy early = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 11);
        Enemy middle = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 51);
        Enemy late = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 91);
        Enemy max = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, 100);
        check(max.getActionThreshold() == 12 && max.getMaxHp() == 580, "level 100 reaches the capped endpoints");
        check(early.getMaxHp() - levelOne.getMaxHp() < middle.getMaxHp() - early.getMaxHp()
                && max.getMaxHp() - late.getMaxHp() < middle.getMaxHp() - early.getMaxHp(), "S-curve eases in and out");
        Enemy previous = levelOne;
        for (int level = 2; level <= 100; level++) {
            Enemy next = new Enemy("Library Wisp A", false, false, GameSettings.Difficulty.NORMAL, 0, level);
            check(next.getMaxHp() >= previous.getMaxHp() && next.getAttack() >= previous.getAttack()
                    && next.getDefense() >= previous.getDefense() && next.getSpeed() >= previous.getSpeed()
                    && next.getActionThreshold() <= previous.getActionThreshold(), "monotonic stats at level " + level);
            previous = next;
        }
        for (int i = 0; i < 4; i++) p.advance();
        Battle boss = new Battle(p, new Random(1), false, 4);
        check(boss.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow().getMaxHp() == 328,
                "boss scales using the same active party average");
        System.out.println("Enemy level scaling checks passed");
    }
}
