import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** SP recovery is triggered once when a hero's AP bar fills. */
public final class AccessorySpRegenerationUiTest {
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
  GameProgress p=new GameProgress(List.of(Student.ZACK));Student z=p.getParty().get(0);p.getInventory().addEquipment("focus_charm");check(p.equip(z,Equipment.Slot.ACCESSORY,"focus_charm"),"equip");z.restoreSp(100);
  Random random=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};
  BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),random);panel.start(p,true,0);Battle b=(Battle)field(panel,"battle");
  @SuppressWarnings("unchecked") Map<Student,Double> hp=(Map<Student,Double>)field(panel,"actionPoints");
  @SuppressWarnings("unchecked") Map<Enemy,Double> ep=(Map<Enemy,Double>)field(panel,"enemyPoints");ep.replaceAll((enemy,value)->-1000.0);hp.put(z,(double)z.getActionThreshold()-1);
  Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);advance.invoke(panel,.11);
  check(z.getSp()==109&&b.getCurrent()==z,"SP recovered when bar fills");
  check(b.getEffects().stream().anyMatch(effect->"+9 SP".equals(effect.getLabel())),"recovery indicator");
  advance.invoke(panel,2.0);check(z.getSp()==109,"no extra roll while choosing a command");
  for(String n:List.of("apTimer","effectTimer","ambushAlertTimer","victoryTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","abilityAlertTimer","monsterTurnTimer"))((javax.swing.Timer)field(panel,n)).stop();
 }catch(Exception e){throw new RuntimeException(e);}});System.out.println("Accessory SP regeneration UI checks passed");}
}
