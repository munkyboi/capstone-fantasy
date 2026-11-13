import game.model.*;
import java.util.*;
public final class BladeMailStatsTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  GameProgress p=new GameProgress(List.of(Student.BOGART)); Student hero=p.getParty().get(0);
  p.getInventory().addEquipment("blade_mail");check(p.equip(hero,Equipment.Slot.ARMOR,"blade_mail"),"equip armor");
  check(hero.getMaxHp()==525 && hero.getDefense()==0 && hero.getSpeed()==12,"blade mail stats");
  check(HealthRegeneration.amount(hero)==147,"20 percentage points added to base regen");
  p.getInventory().addEquipment("heroes_ring");check(p.equip(hero,Equipment.Slot.ACCESSORY,"heroes_ring"),"equip ring");
  check(hero.getMaxHp()==555 && hero.getDefense()==0 && hero.getSpeed()==6 && HealthRegeneration.amount(hero)==266,"zero defense overrides accessory bonuses");
  System.out.println("Blade Mail stat checks passed");
 }
}
