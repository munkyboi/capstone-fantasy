import game.model.*;
import java.lang.reflect.*;
import java.util.*;

/** Checks independent DOT dodge rolls and their five-cycle lifetime. */
public final class DotDodgeTest {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Random rolls(double... values) { return new Random() {
        int index;
        public int nextInt(int bound) { return 0; }
        public double nextDouble() { return values[Math.min(index++, values.length - 1)]; }
    }; }
    @SuppressWarnings("unchecked") static void add(Battle battle, GameCharacter source, GameCharacter target) throws Exception {
        Class<?> type = Class.forName("game.model.BattleDotEngine$DamageOverTime");
        Constructor<?> ctor = type.getDeclaredConstructor(GameCharacter.class, GameCharacter.class, int.class);
        ctor.setAccessible(true);
        Field dots = Battle.class.getDeclaredField("dots"); dots.setAccessible(true); Object engine = dots.get(battle);
        Field burns = engine.getClass().getDeclaredField("burns"); burns.setAccessible(true);
        ((List<Object>) burns.get(engine)).add(ctor.newInstance(source, target, 50));
    }
    public static void main(String[] args) throws Exception {
        Battle missed = new Battle(List.of(Student.CHARICE), rolls(.999999));
        Student hero = missed.getParty().get(0); Enemy monster = missed.getEnemies().get(0);
        add(missed, monster, hero); int hp = hero.getHp();
        for (int i=0; i<5; i++) {
            check(missed.tickDamageOverTime(hero) && hero.getHp()==hp, "hero dodges tick " + i);
            check(missed.getEffects().stream().anyMatch(e -> e.getStudent()!=null && "Dodge".equals(e.getLabel())), "hero dodge indicator");
        }
        check(!missed.hasDamageOverTime(hero) && !missed.tickDamageOverTime(hero), "dodged ticks consume duration");
        Battle enemyMiss = new Battle(List.of(Student.CHARICE), rolls(.999999));
        hero=enemyMiss.getParty().get(0); monster=enemyMiss.getEnemies().get(0); add(enemyMiss,hero,monster); hp=monster.getHp();
        check(enemyMiss.tickDamageOverTime(monster) && monster.getHp()==hp && "Dodge".equals(enemyMiss.getEffects().get(0).getLabel()), "monster dodges DOT");
        Battle landed = new Battle(List.of(Student.CHARICE), rolls(0));
        hero=landed.getParty().get(0); monster=landed.getEnemies().get(0); add(landed,monster,hero); hp=hero.getHp();
        check(landed.tickDamageOverTime(hero) && hero.getHp()==hp-10, "successful tick deals its share");
        GameProgress p = new GameProgress(List.of(Student.CHARICE)); hero=p.getParty().get(0);
        p.getInventory().addEquipment("ultra_instinct_shirt"); check(p.equip(hero,Equipment.Slot.ARMOR,"ultra_instinct_shirt"), "equip shirt");
        Battle shirt = new Battle(p, rolls(0,.49)); add(shirt,shirt.getEnemies().get(0),hero); hp=hero.getHp();
        check(shirt.tickDamageOverTime(hero) && hero.getHp()==hp && "Dodge".equals(shirt.getEffects().get(0).getLabel()), "shirt gives separate dodge roll");
        System.out.println("DOT dodge checks passed");
    }
}
