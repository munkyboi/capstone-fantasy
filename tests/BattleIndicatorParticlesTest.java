import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

public final class BattleIndicatorParticlesTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 @SuppressWarnings("unchecked") public static void main(String[]a)throws Exception{SwingUtilities.invokeAndWait(()->{try{
  GameProgress p=new GameProgress(List.of(Student.ANGEL)); BattlePanel panel=new BattlePanel(()->{},q->{},new GameSettings(),new Random(3));panel.start(p,false,0);
  Battle battle=(Battle)field(panel,"battle");Enemy enemy=battle.getEnemies().get(0);
  Method show=BattlePanel.class.getDeclaredMethod("showActionEffects",Object.class);show.setAccessible(true);
  battle.replaceEffects(List.of(new BattleEffect(enemy,-10)));show.invoke(panel,new Object[]{null});
  List<Object> batches=(List<Object>)field(panel,"effectBatches");check(batches.size()==1,"first hit has particle batch");
  Object first=batches.get(0);Method effects=first.getClass().getDeclaredMethod("effects");effects.setAccessible(true);Method time=first.getClass().getDeclaredMethod("startedNanos");time.setAccessible(true);
  battle.replaceEffects(List.of(new BattleEffect(enemy,-20)));show.invoke(panel,new Object[]{null});
  check(batches.size()==2 && batches.get(0)==first && (long)time.invoke(batches.get(1)) >= (long)time.invoke(first),"second hit preserves first clock");
  check(((List<BattleEffect>)effects.invoke(first)).get(0).getAmount()==-10 && ((List<BattleEffect>)effects.invoke(batches.get(1))).get(0).getAmount()==-20,"values stay independent");
  Class<?> type=first.getClass();Constructor<?> constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
  batches.set(0,constructor.newInstance(((List<BattleEffect>)effects.invoke(first)),System.nanoTime()-2_000_000_000L,0));
  javax.swing.Timer timer=(javax.swing.Timer)field(panel,"effectTimer");timer.getActionListeners()[0].actionPerformed(new ActionEvent(timer,0,""));
  check(batches.size()==1 && ((List<BattleEffect>)effects.invoke(batches.get(0))).get(0).getAmount()==-20,"each batch expires separately");
  panel.start(p,false,0);check(batches.isEmpty(),"new battle clears old particles");
  for(String n:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,n)).stop();
  System.out.println("Battle indicator particle checks passed");
 }catch(Exception e){throw new RuntimeException(e);}});}
}
