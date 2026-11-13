import game.model.*;
import java.lang.reflect.*;
import java.util.*;

public class LooperChainTest {
    static void check(boolean ok, String text) { if (!ok) throw new AssertionError(text); }
    static void set(Object o, String name, Object value) throws Exception { Field f=o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o,value); }
    static final class Rolls extends Random { double roll; public double nextDouble(){return roll;} public int nextInt(int n){return 0;} }
    public static void main(String[] args) throws Exception {
        GameProgress p=new GameProgress(List.of(Student.BOGART)); p.advance(); p.advance();
        Student s=p.getParty().get(0); p.getInventory().addEquipment("looper_ring"); check(p.equip(s,Equipment.Slot.ACCESSORY,"looper_ring"),"equip");
        Rolls rng=new Rolls(); Battle b=new Battle(p,rng); List<Enemy> enemies=b.getEnemies(); check(enemies.size()>1,"multiple enemies");
        Enemy target=enemies.get(0), other=enemies.get(1); for(Enemy e:enemies)set(e,"hp",10000);
        Field actionField=Battle.class.getDeclaredField("heroActions"); actionField.setAccessible(true); Object actions=actionField.get(b);
        Method start=actions.getClass().getDeclaredMethod("looperHits",Student.class,String.class); start.setAccessible(true);
        set(actions,"lastHeroHitTarget",target); set(actions,"lastHeroHitDamage",100); start.invoke(actions,s,"");
        int[] expected={50,60,62,63,63};
        for(int value:expected){int before=target.getHp(); check(b.advanceLooperHit(),"loop executes"); check(before-target.getHp()==value,"loop damage "+value);}
        check(!b.hasPendingLooperHits(),"five-loop limit");
        set(actions,"lastHeroHitTarget",target); set(actions,"lastHeroHitDamage",100); rng.roll=0; start.invoke(actions,s,""); rng.roll=.7;
        check(b.advanceLooperHit() && !b.hasPendingLooperHits(),"failed second roll ends chain");
        rng.roll=.7; start.invoke(actions,s,""); check(!b.hasPendingLooperHits(),"failed first roll");
        rng.roll=0; set(target,"hp",1); set(actions,"lastHeroHitTarget",target); set(actions,"lastHeroHitDamage",100); start.invoke(actions,s,"");
        check(b.advanceLooperHit() && !target.isAlive(),"loop defeats original target"); int before=other.getHp(); check(b.advanceLooperHit(),"retarget"); check(before-other.getHp()==51,"retarget uses actual previous damage");
        System.out.println("Looper chain checks passed");
    }
}
