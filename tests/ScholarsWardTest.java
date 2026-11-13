import game.io.SaveStore;
import game.model.*;
import java.nio.file.*;
import java.util.*;

/** Scholar's Ward purchase, equipment, AP recovery and persistence. */
public final class ScholarsWardTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random roll(double value){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return value;}};}
 public static void main(String[]args)throws Exception {
  int index=java.util.stream.IntStream.range(0,CarbonMarket.offers().size()).filter(i->CarbonMarket.offers().get(i).id().equals("scholars_ward")).findFirst().orElseThrow();
  GameProgress p=new GameProgress(List.of(Student.ZACK,Student.BOGART));Student z=p.getParty().stream().filter(s->s.getId()==Student.ZACK).findFirst().orElseThrow(),b=p.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();
  p.addPetot(2500);check(CarbonMarket.buy(p,index).startsWith("Purchased")&&p.getPetot()==0,"market price");
  check(!p.equip(b,Equipment.Slot.ACCESSORY,"scholars_ward"),"restricted to Zack");
  check(p.equip(z,Equipment.Slot.ACCESSORY,"scholars_ward")&&z.getMaxHp()==78&&z.getDefense()==5,"ward grants HP and defense");
  z.restoreSp(100);Battle success=new Battle(p,roll(.249999));check(success.regenerate(List.of(z),false)&&z.getSp()==118,"AP recovery succeeds below 25 percent");
  check(success.getEffects().stream().anyMatch(e->e.getStudent()==z&&"+18 SP".equals(e.getLabel())),"SP recovery indicator");
  z.restoreSp(100);Battle boundary=new Battle(p,roll(.25));check(!boundary.regenerate(List.of(z),false)&&z.getSp()==100,"25 percent boundary fails");
  z.restoreSp(179);Battle capped=new Battle(p,roll(0));check(capped.regenerate(List.of(z),false)&&z.getSp()==180,"recovery stops at max SP");
  z.takeDamage(9999);z.restoreSp(0);check(!capped.regenerate(List.of(z),false)&&z.getSp()==0,"fallen wearer does not recover");
  z.heal(999);z.restoreSp(37);p.advance();Path dir=Files.createTempDirectory("scholars-ward-");
  try {SaveStore store=new SaveStore(dir);for(SaveStore.Format f:SaveStore.Format.values()){
   store.save(1,f,p);Student saved=store.load(1,f).getParty().stream().filter(s->s.getId()==Student.ZACK).findFirst().orElseThrow();
   check("scholars_ward".equals(saved.getEquipmentId(Equipment.Slot.ACCESSORY))&&saved.getMaxHp()==78&&saved.getSp()==37,"equipment and SP persist in "+f);
  }} finally {try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
  System.out.println("Scholar's Ward checks passed");
 }
}
