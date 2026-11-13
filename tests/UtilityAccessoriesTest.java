import game.model.*;
import java.nio.file.*;
import java.util.*;
import game.io.SaveStore;

/** Utility effects, chance boundaries, reward scaling and save compatibility. */
public final class UtilityAccessoriesTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random fixed(double value){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return value;}};}
 static Student equip(GameProgress p,int id,String item){Student s=p.getParty().stream().filter(x->x.getId()==id).findFirst().orElseThrow();p.getInventory().addEquipment(item);check(p.equip(s,Equipment.Slot.ACCESSORY,item),"equip "+item);return s;}
 public static void main(String[] args)throws Exception {
  for(double roll:new double[]{.249999,.25}){
   GameProgress p=new GameProgress(List.of(Student.ZACK));Student s=equip(p,Student.ZACK,"focus_charm");s.restoreSp(100);Battle b=new Battle(p,fixed(roll));
   check(b.regenerate(List.of(s),false)==(roll<.25),"focus chance");check(s.getSp()==(roll<.25?109:100),"focus restores five percent SP");
  }
  for(double roll:new double[]{.149999,.15}){
   GameProgress p=new GameProgress(List.of(Student.CHARICE));Student s=equip(p,Student.CHARICE,"guardian_band");Battle b=new Battle(p,fixed(roll));Enemy e=b.getEnemies().get(0);
   int hp=s.getHp(),normal=e.scaleDamage(Battle.damageAfterDefense(e.getAttack(),s.getDefense()));check(b.enemyAttack(e),"enemy action");
   check(hp-s.getHp()==(roll<.15?(int)Math.ceil(normal*.75):normal),"guardian chance and damage");
   check(b.getEffects().stream().anyMatch(effect->"Ward".equals(effect.getLabel()))==(roll<.15),"ward indicator");
  }
  for(double roll:new double[]{.199999,.20}){
   GameProgress p=new GameProgress(List.of(Student.CHARICE));Student s=equip(p,Student.CHARICE,"courage_charm");s.takeDamage(45);Battle b=new Battle(p,fixed(roll));Enemy e=b.getEnemies().get(0);
   int hp=s.getHp(),damage=e.scaleDamage(Battle.damageAfterDefense(e.getAttack(),s.getDefense()));check(b.enemyAttack(e),"enemy action");
   check(s.getHp()==hp-damage+(roll<.20?5:0),"courage healing boundary");
  }
  GameProgress p=new GameProgress(List.of(Student.ZACK,Student.BOGART));Student z=equip(p,Student.ZACK,"market_charm");Battle b=new Battle(p,fixed(0));b.getEnemies().forEach(e->e.takeDamage(9999));
  check(b.victoryPetot(false)==14&&b.victoryPetot(true)==132,"market bonus, rounding and greed");
  z.takeDamage(9999);check(b.victoryPetot(false)==12,"KO wearer does not grant bonus");
  GameProgress save=new GameProgress(List.of(Student.ZACK));Student savedHero=equip(save,Student.ZACK,"focus_charm");savedHero.restoreSp(100);save.advance();Path dir=Files.createTempDirectory("utility-accessory-");
  try{SaveStore store=new SaveStore(dir);for(SaveStore.Format f:SaveStore.Format.values()){store.save(1,f,save);Student restored=store.load(1,f).getParty().get(0);check("focus_charm".equals(restored.getEquipmentId(Equipment.Slot.ACCESSORY)),"save "+f);check(restored.getSp()==100,"SP preserved "+f);}}
  finally{try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
  System.out.println("Utility accessory checks passed");
 }
}
