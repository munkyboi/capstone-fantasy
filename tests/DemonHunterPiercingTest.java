import game.io.SaveStore;
import game.model.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Piercing Bolts affects normal attacks and Twin Shot without weakening negative DEF. */
public final class DemonHunterPiercingTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random hit(){return new Random(){public int nextInt(int n){return 0;} public double nextDouble(){return 0;}};}
 @SuppressWarnings("unchecked") static void target(Battle battle,Enemy enemy)throws Exception{Field f=Battle.class.getDeclaredField("enemies");f.setAccessible(true);List<Enemy> enemies=(List<Enemy>)f.get(battle);enemies.clear();enemies.add(enemy);}
 public static void main(String[]args)throws Exception {
  GameProgress p=new GameProgress(List.of(Student.CHARICE));Student hero=p.getParty().get(0);while(hero.getLevel()<30)hero.gainExperience(hero.getXpRequired());
  p.getInventory().addEquipment("demon_hunter");check(p.equip(hero,Equipment.Slot.WEAPON,"demon_hunter"),"equip weapon");
  Enemy foe=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,100,0);int defense=foe.getDefense(),hp=foe.getHp();
  Battle b=new Battle(p,hit());target(b,foe);check(b.selectStudent(hero)&&b.attack(foe),"basic attack");
  int reduced=defense-(int)Math.ceil(defense*.25);
  check(hp-foe.getHp()==Battle.damageAfterDefense((int)Math.ceil(hero.getAttack()*.8),reduced),"basic attack ignores 25 percent defense");
  check(Battle.damageAfterDefense(hero.getAttack(),reduced)>Battle.damageAfterDefense(hero.getAttack(),defense),"piercing improves damage");
  Battle twin=new Battle(p,hit());Enemy shield=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,100,0);target(twin,shield);
  int initial=shield.getHp(),baseDefense=shield.getDefense();check(twin.selectStudent(hero)&&twin.attack(shield),"Twin Shot");
  int firstPower=(int)Math.ceil(hero.getAttack()*.8),firstDefense=baseDefense-(int)Math.ceil(baseDefense*.25);
  check(initial-shield.getHp()==Battle.damageAfterDefense(firstPower,firstDefense),"first shot pierces");
  int afterDebuff=shield.getDefense(),remaining=shield.getHp();check(afterDebuff>0,"target still has positive DEF");
  check(twin.advanceTwinShot()&&remaining-shield.getHp()==Battle.damageAfterDefense((int)Math.ceil(hero.getAttack()*.8),afterDebuff-(int)Math.ceil(afterDebuff*.25)),"followup pierces after debuff");
  twin.advanceTwinShot();check(shield.getDefense()==baseDefense,"temporary DEF reduction clears");
  GameProgress low=new GameProgress(List.of(Student.CHARICE));Student novice=low.getParty().get(0);low.getInventory().addEquipment("demon_hunter");low.equip(novice,Equipment.Slot.WEAPON,"demon_hunter");
  Battle negative=new Battle(low,hit());Enemy wisp=new Enemy("Wisp");target(negative,wisp);negative.selectStudent(novice);negative.attack(wisp);
  check(wisp.getDefense()==-5,"Twin Shot can reduce DEF below zero");int before=wisp.getHp();negative.advanceTwinShot();
  check(before-wisp.getHp()==Battle.damageAfterDefense((int)Math.ceil(novice.getAttack()*.8),-5),"piercing preserves negative defense bonus");
  check(Equipment.fromId("demon_hunter").getSpecialDescription().contains("25%"),"equipment description");
  p.advance();Path dir=Files.createTempDirectory("demon-hunter-");try{SaveStore store=new SaveStore(dir);for(SaveStore.Format fmt:SaveStore.Format.values()){
   store.save(1,fmt,p);Student loaded=store.load(1,fmt).getParty().get(0);check("demon_hunter".equals(loaded.getEquipmentId(Equipment.Slot.WEAPON)),"weapon saved " + fmt);
  }}finally{try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
  System.out.println("Demon Hunter piercing checks passed");
 }
}
