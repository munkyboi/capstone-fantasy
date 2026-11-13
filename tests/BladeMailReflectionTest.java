import game.model.*;
import java.lang.reflect.*;
import java.util.*;

/** Checks regeneration-based Blade Mail reflection against actual HP lost. */
public final class BladeMailReflectionTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random fixed(double value){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return value;}};}
 @SuppressWarnings("unchecked") static void replace(Battle battle,Enemy enemy)throws Exception {Field f=Battle.class.getDeclaredField("enemies");f.setAccessible(true);List<Enemy> list=(List<Enemy>)f.get(battle);list.clear();list.add(enemy);}
 public static void main(String[] args)throws Exception{
  GameProgress p=new GameProgress(List.of(Student.ZACK));Student hero=p.getParty().get(0);p.getInventory().addEquipment("blade_mail");check(p.equip(hero,Equipment.Slot.ARMOR,"blade_mail"),"equip");
  Battle b=new Battle(p,fixed(0));Enemy foe=Enemy.sentinel("Attack",true,GameSettings.Difficulty.NORMAL,0,1,0);replace(b,foe);
  int hp=hero.getHp(),enemyHp=foe.getHp(),regen=HealthRegeneration.amount(hero);check(regen==53,"regeneration amount includes armor and max HP");
  check(b.enemyAttack(foe),"enemy attack");int taken=hp-hero.getHp();
  check(taken==12 && enemyHp-foe.getHp()==8,"50 percent base times one plus regen rate, rounded up");
  check(b.getEffects().stream().anyMatch(e->"Reflect".equals(e.getLabel())),"reflect indicator");
  GameProgress q=new GameProgress(List.of(Student.ZACK));Student dodge=q.getParty().get(0);q.getInventory().addEquipment("blade_mail");q.equip(dodge,Equipment.Slot.ARMOR,"blade_mail");
  Battle missed=new Battle(q,fixed(.99999));Enemy attacker=Enemy.sentinel("Attack",true,GameSettings.Difficulty.NORMAL,0,1,0);replace(missed,attacker);int before=attacker.getHp();
  check(missed.enemyAttack(attacker)&&attacker.getHp()==before&&missed.getEffects().stream().noneMatch(e->"Reflect".equals(e.getLabel())),"dodged hits do not reflect");
  System.out.println("Blade Mail reflection checks passed");
 }
}
