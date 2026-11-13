import game.model.*;
import java.util.*;

/** Every accessory restores its own fraction of SP on a 25% AP-fill roll. */
public final class AccessorySpRegenerationTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random fixed(double value){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return value;}};}
 public static void main(String[]args){
  Map<String,Integer> rates=Map.of("market_charm",3,"focus_charm",5,"guardian_band",4,"courage_charm",5,"sentinel_band",5,"campus_medal",6,"heroes_ring",4,"ring_of_power",5,"looper_ring",6,"scholars_ward",10);
  for(var entry:rates.entrySet())for(double roll:new double[]{0.249999,0.25}){
   GameProgress p=new GameProgress(List.of(Student.ZACK));Student s=p.getParty().get(0);p.getInventory().addEquipment(entry.getKey());check(p.equip(s,Equipment.Slot.ACCESSORY,entry.getKey()),"equip "+entry.getKey());
   s.restoreSp(100);Battle b=new Battle(p,fixed(roll));boolean result=b.regenerate(List.of(s),false);
   int amount=(s.getMaxSp()*entry.getValue()+99)/100;check(result==(roll<.25)&&s.getSp()==100+(roll<.25?amount:0),entry.getKey()+" probability and amount");
   check(Equipment.fromId(entry.getKey()).getSpecialDescription().contains(entry.getValue()+"% max SP"),entry.getKey()+" description");
  }
  GameProgress p=new GameProgress(List.of(Student.ZACK,Student.BOGART));Student z=p.getParty().stream().filter(x->x.getId()==Student.ZACK).findFirst().orElseThrow();p.getInventory().addEquipment("focus_charm");p.equip(z,Equipment.Slot.ACCESSORY,"focus_charm");
  Battle battle=new Battle(p,fixed(0));z.restoreSp(179);check(battle.regenerate(List.of(z),false)&&z.getSp()==180,"SP capped");
  z.takeDamage(9999);z.restoreSp(0);check(!battle.regenerate(List.of(z),false)&&z.getSp()==0,"KO wearer does not recover");
  System.out.println("Accessory SP regeneration checks passed");
 }
}
