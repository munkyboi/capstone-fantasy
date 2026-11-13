import game.model.*;
import game.io.SaveStore;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class ManaLeakTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Random rolls(double proc){return new Random(){int calls;@Override public int nextInt(int n){return 0;}@Override public double nextDouble(){return calls++==0?0:proc;}};}
    static GameProgress party(){GameProgress p=new GameProgress(List.of(Student.BOGART));p.getInventory().addEquipment("looper_ring");check(p.equip(p.getParty().get(0),Equipment.Slot.ACCESSORY,"looper_ring"),"equip ring");return p;}
    static Enemy sentinel(Battle b){return b.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();}
    public static void main(String[]args)throws Exception{
        GameProgress p=party();p.advance();Student hero=p.getParty().get(0);Battle b=new Battle(p,rolls(.7));Enemy target=sentinel(b);
        check(hero.getMaxSp()==35&&b.selectStudent(hero)&&b.attack(target),"normal attack");
        check(target.getSp()==855&&b.getEffects().stream().filter(BattleEffect::isManaLeak).count()==1,"one 5 percent max SP leak with orange indicator");
        GameProgress many=party();many.advance();Random targetSentinel=new Random(){@Override public int nextInt(int n){return n==3?2:n==2?1:0;}@Override public double nextDouble(){return 0;}};
        Battle multi=new Battle(many,targetSentinel);Student attacker=many.getParty().get(0);Enemy victim=sentinel(multi);
        check(multi.selectStudent(attacker)&&multi.attack(victim),"extra hit attack");
        check(victim.getSp()==855&&multi.getEffects().stream().filter(BattleEffect::isManaLeak).count()==1,"direct hit leaks immediately");
        for(int i=0;i<5;i++) {check(multi.advanceLooperHit(),"resolve extra hit");check(multi.getEffects().stream().anyMatch(BattleEffect::isManaLeak),"each item hit leaks separately");}
        check(victim.getSp()==630,"direct hit and all five item hits each leak 5 percent of maximum SP");
        GameProgress dot=party();dot.advance();dot.advance();dot.advance();Student warrior=dot.getParty().get(0);dot.getInventory().addEquipment("light_saber");check(dot.equip(warrior,Equipment.Slot.WEAPON,"light_saber"),"equip light saber");
        Battle dots=Battle.merchantBattle(dot,rolls(.7),GameSettings.Difficulty.NORMAL);Enemy merchant=dots.getEnemies().get(0);
        check(dots.selectStudent(warrior)&&dots.attack(merchant)&&merchant.getSp()==760,"direct hit leaks merchant SP");
        check(dots.tickDamageOverTime(merchant)&&merchant.getSp()==720&&dots.getEffects().stream().anyMatch(BattleEffect::isManaLeak),"item DOT leaks SP based on maximum");
        GameProgress reflect=party();reflect.advance();Student defender=reflect.getParty().get(0);reflect.getInventory().addEquipment("blade_mail");check(reflect.equip(defender,Equipment.Slot.ARMOR,"blade_mail"),"equip blade mail");
        Battle counter=new Battle(reflect,rolls(0));Enemy enemy=sentinel(counter);check(counter.enemyAttack(enemy)&&counter.enemyAttack(enemy)&&enemy.getSp()==835,"reflected item damage leaks SP");
        var leak=Battle.class.getDeclaredMethod("applyManaLeak",boolean.class,Enemy.class,int.class);leak.setAccessible(true);
        Enemy rounding=new Enemy("Sentinel",true,false,GameSettings.Difficulty.EASY);
        leak.invoke(b,true,rounding,1);check(rounding.getSp()==855,"5 percent of 900 SP drains 45");
        Random highRoll=new Random(){@Override public int nextInt(int n){return n==6?5:0;}@Override public double nextDouble(){return 0;}};
        Battle upper=new Battle(p,highRoll);Enemy upperTarget=sentinel(upper);
        leak.invoke(upper,true,upperTarget,1);check(upperTarget.getSp()==810,"upper roll drains 10 percent of maximum SP");
        Field enemySp=Enemy.class.getDeclaredField("skillPoints");enemySp.setAccessible(true);enemySp.setInt(upperTarget,3);
        leak.invoke(upper,true,upperTarget,1);check(upperTarget.getSp()==0,"drain cannot exceed remaining SP");
        enemySp.setInt(upperTarget,100);leak.invoke(upper,true,upperTarget,0);check(upperTarget.getSp()==100,"zero damage does not drain SP");
        Path dir=Files.createTempDirectory("mana-leak-save");try{
            SaveStore store=new SaveStore(dir);store.save(1,SaveStore.Format.TEXT,p);
            Path text=dir.resolve("slot-1.txt");String old=Files.readString(text).replaceAll("(?m)^mechanics\\.looperManaLeak=.*\\R","").replaceAll("(?m)^student\\.1\\.sp=.*$","student.1.sp=70");Files.writeString(text,old);
            check(store.load(1,SaveStore.Format.TEXT).getParty().get(0).getSp()==35,"legacy text save scales old bonus SP");
            Field version=Student.class.getDeclaredField("looperManaLeakVersion");version.setAccessible(true);version.setBoolean(hero,false);
            Field sp=Student.class.getDeclaredField("skillPoints");sp.setAccessible(true);sp.setInt(hero,70);
            try(ObjectOutputStream stream=new ObjectOutputStream(Files.newOutputStream(dir.resolve("slot-2.dat")))){stream.writeObject(p);}
            Student loaded=store.load(2,SaveStore.Format.BINARY).getParty().get(0);
            check(loaded.getSp()==35&&loaded.getMaxSp()==35,"legacy binary save scales old bonus SP");
        }finally{try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println("Mana Leak checks passed");
    }
}
