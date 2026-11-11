import game.model.Battle;
import game.model.Enemy;
import game.model.Student;
import java.util.List;
import java.util.Random;

/** Checks Angel's basic attack against Charice's two-hit ability. */
public final class AngelAttackTest {
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static Battle newBattle() {
        return new Battle(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL), new Random() {
            @Override public int nextInt(int bound) { return 2; }
        });
    }
    /** Runs the attack balance checks.
     * @param args unused
     */
    public static void main(String[] args) {
        Battle hunterBattle = newBattle();
        Student hunter = hunterBattle.getParty().stream().filter(s -> s.getId() == Student.CHARICE).findFirst().orElseThrow();
        Enemy hunterTarget = hunterBattle.getEnemies().get(0);
        check(hunterBattle.selectStudent(hunter), "select Charice");
        int before = hunterTarget.getHp();
        check(hunterBattle.ability(null, hunterTarget), "use Twin Shot");
        int twinShotDamage = before - hunterTarget.getHp();
        Battle monkBattle = newBattle();
        Student monk = monkBattle.getParty().stream().filter(s -> s.getId() == Student.ANGEL).findFirst().orElseThrow();
        Enemy monkTarget = monkBattle.getEnemies().get(0);
        check(monk.getAttack() == 39 && monk.getActionThreshold() == 45, "Angel has increased attack and unchanged AP threshold");
        check(monkBattle.selectStudent(monk), "select Angel");
        before = monkTarget.getHp();
        check(monkBattle.attack(monkTarget), "use Angel basic attack");
        int basicDamage = before - monkTarget.getHp();
        check(twinShotDamage == 26 && basicDamage == 36, "expected damage against a Wisp");
        check(Math.abs((double) basicDamage / twinShotDamage - 1.4) < 0.05, "Angel damage is about 1.4 times Twin Shot");
        System.out.println("Angel attack balance checks passed");
    }
}
