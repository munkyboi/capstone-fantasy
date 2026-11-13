import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

/** Ready monsters take separate turns while AP charging and hero priority continue. */
public final class MonsterTurnQueueTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 static Random fixed(){return new Random(){public int nextInt(int n){return n==2?1:0;}public double nextDouble(){return 0;}};}
 static void stop(BattlePanel p)throws Exception{for(String n:List.of("apTimer","effectTimer","ambushAlertTimer","victoryTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","abilityAlertTimer","monsterTurnTimer"))((javax.swing.Timer)field(p,n)).stop();}
 public static void main(String[]args)throws Exception{
  SwingUtilities.invokeAndWait(()->{try{
   GameProgress p=new GameProgress(List.of(Student.BOGART));BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),fixed());panel.start(p,true,0);
   Battle battle=(Battle)field(panel,"battle");check(battle.getEnemies().size()==2,"two monsters");
   @SuppressWarnings("unchecked") Map<Enemy,Double> enemyPoints=(Map<Enemy,Double>)field(panel,"enemyPoints");
   @SuppressWarnings("unchecked") Map<Student,Double> heroPoints=(Map<Student,Double>)field(panel,"actionPoints");
   Student hero=p.getParty().get(0);heroPoints.put(hero,0.0);for(Enemy e:battle.getEnemies())enemyPoints.put(e,(double)e.getActionThreshold());
   Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);
   int hp=hero.getHp();advance.invoke(panel,0.0);int afterFirst=hero.getHp();
   javax.swing.Timer timer=(javax.swing.Timer)field(panel,"monsterTurnTimer");
   check(afterFirst<hp && timer.isRunning() && timer.getInitialDelay()==400,"first action starts pause");
   check(enemyPoints.values().stream().filter(v->v==0).count()==1,"only one monster acts");
   advance.invoke(panel,.1);check(hero.getHp()==afterFirst && heroPoints.get(hero)>0,"hero AP advances without simultaneous monster action");
   timer.stop();advance.invoke(panel,0.0);check(hero.getHp()<afterFirst && enemyPoints.values().stream().filter(v->v==0).count()==1,"second monster acts after pause");
   for(Enemy e:battle.getEnemies())enemyPoints.put(e,(double)e.getActionThreshold());heroPoints.put(hero,(double)hero.getActionThreshold());
   advance.invoke(panel,0.0);check(battle.getCurrent()==hero,"ready hero retains priority during pause");
   panel.start(p,true,0);check(!timer.isRunning(),"new encounter clears pending pause");stop(panel);
  }catch(Exception e){throw new RuntimeException(e);}});
  System.out.println("Monster turn queue checks passed");
 }
}
