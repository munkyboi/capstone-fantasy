import game.model.*;
import java.util.List;
import java.util.Random;

/** Verifies percentage bonuses use level-adjusted base ATK, excluding equipment. */
public final class HeroAbilityScalingTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Random fixed() { return new Random() {
        @Override public int nextInt(int bound) { return 0; }
        @Override public double nextDouble() { return 0; }
    }; }
    private static void attack(int id, String gear, double rate) {
        GameProgress progress = new GameProgress(List.of(id));
        Student hero = progress.getParty().get(0);
        hero.gainExperience(100);
        progress.getInventory().addEquipment(gear);
        check(progress.equip(hero, Equipment.Slot.WEAPON, gear), "equip " + gear);
        Battle battle = new Battle(progress, fixed());
        Enemy enemy = battle.getEnemies().get(0);
        int hp = enemy.getHp();
        check(battle.selectStudent(hero) && battle.ability(null, enemy), "cast " + id);
        int first = Math.max(1, hero.getAttack() + (int) Math.ceil(hero.getBaseAttack() * rate) - enemy.getDefense());
        int expected = first + (id == Student.CHARICE ? Math.max(1, hero.getAttack() - enemy.getDefense()) : 0);
        check(hp - enemy.getHp() == expected, "ability damage " + id + ": expected " + expected + " got " + (hp - enemy.getHp()));
    }
    private static void heal(int missing, int expected) {
        GameProgress progress = new GameProgress(List.of(Student.ANGEL, Student.BOGART));
        Student angel = progress.getParty().stream().filter(s -> s.getId() == Student.ANGEL).findFirst().orElseThrow();
        Student ally = progress.getParty().stream().filter(s -> s.getId() == Student.BOGART).findFirst().orElseThrow();
        ally.takeDamage(missing);
        Battle battle = new Battle(progress, fixed());
        check(battle.selectStudent(angel) && battle.ability(ally, null), "healing kiss succeeds");
        check(ally.getHp() == ally.getMaxHp() - missing + expected, "heals half missing HP rounded up");
    }
    public static void main(String[] args) {
        attack(Student.ZACK, "scholar_staff", 0.40);
        attack(Student.BOGART, "cadet_blade", 0.60);
        attack(Student.CHARICE, "hunter_crossbows", 0.50);
        heal(0, 0); heal(1, 1); heal(51, 26); heal(100, 50);
        System.out.println("Hero ability scaling checks passed");
    }
}
