import game.io.*;
import game.model.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Older equipment HP bonuses remain loadable without accepting arbitrary max HP. */
public final class EquipmentSaveMigrationTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory("equipment-save-migration-");
  try {
   GameProgress p=new GameProgress(List.of(Student.BOGART));p.advance();Student hero=p.getParty().get(0);
   p.getInventory().addEquipment("blade_mail");p.getInventory().addEquipment("heroes_ring");
   check(p.equip(hero,Equipment.Slot.ARMOR,"blade_mail") && p.equip(hero,Equipment.Slot.ACCESSORY,"heroes_ring"),"equip");
   SaveStore store=new SaveStore(dir);store.save(1,SaveStore.Format.TEXT,p);
   Path path=dir.resolve("slot-1.txt"); Properties original=new Properties();try(var in=Files.newBufferedReader(path)){original.load(in);}
   for(int oldMax:new int[]{465,480,540,555}) {
    Properties data=new Properties();data.putAll(original);data.setProperty("student.1.maxHp",Integer.toString(oldMax));data.setProperty("student.1.hp",Integer.toString(oldMax/2));
    try(var out=Files.newBufferedWriter(path)){data.store(out,null);}
    Student loaded=store.load(1,SaveStore.Format.TEXT).getParty().get(0);
    check(loaded.getMaxHp()==555 && loaded.getHp()==Math.round((double)(oldMax/2)*555/oldMax),"preserve HP proportion from "+oldMax);
    check("blade_mail".equals(loaded.getEquipmentId(Equipment.Slot.ARMOR)) && "heroes_ring".equals(loaded.getEquipmentId(Equipment.Slot.ACCESSORY)),"equipment retained");
   }
   original.setProperty("student.1.maxHp","467");original.setProperty("student.1.hp","400");try(var out=Files.newBufferedWriter(path)){original.store(out,null);}
   boolean rejected=false;try{store.load(1,SaveStore.Format.TEXT);}catch(SaveException e){rejected=true;}check(rejected,"unrecognized max HP rejected");
  } finally {try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  System.out.println("Equipment save migration checks passed");
 }
}
