import game.model.*;
import game.view.BattlePanel;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

public class CrystalMeshUiTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception {SwingUtilities.invokeAndWait(()->{try{
  GameProgress p=new GameProgress(List.of(0,1,2,3));for(int i=0;i<4;i++)p.advance();Random rng=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return .99;}};
  BattlePanel panel=new BattlePanel(()->{},q->{},new GameSettings(),rng);panel.start(p,false,4);Battle b=(Battle)field(panel,"battle");Enemy boss=b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  for(Enemy e:b.getEnemies())if(!e.isBoss())e.takeDamage(999999);
  Field turn=Battle.class.getDeclaredField("turn");turn.setAccessible(true);turn.setInt(b,-1);
  Map<Student,Double> heroAp=(Map<Student,Double>)field(panel,"actionPoints");heroAp.replaceAll((h,v)->0.0);
  Map<Enemy,Double> enemyAp=(Map<Enemy,Double>)field(panel,"enemyPoints");enemyAp.replaceAll((e,v)->0.0);enemyAp.put(boss,(double)boss.getActionThreshold());
  Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);advance.invoke(panel,0.0);
  check(b.getLastAbility()!=null&&b.getLastAbility().skill().equals("Crystal Mesh"),"boss uses mesh in battle screen");check(b.getEnemies().size()==6&&enemyAp.size()==6,"screen tracks exactly current wave");
  for(Enemy e:b.getEnemies())if(!e.isBoss())check(enemyAp.get(e)==0.0,"new sentinel starts at zero AP");
  JPanel scene=(JPanel)field(panel,"scene");scene.setSize(960,540);scene.paint(new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB).getGraphics());
  b.getEnemies().forEach(e->e.takeDamage(999999));Method refresh=BattlePanel.class.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(panel);
  check(p.getPetot()==50234,"Petot includes boss and both defeated waves");check(((Integer)field(panel,"victoryExperience"))==10270,"XP includes boss and both defeated waves");refresh.invoke(panel);check(p.getPetot()==50234,"victory paid only once");
  for(String n:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,n)).stop();
  System.out.println("Crystal Mesh battle screen checks passed");
 }catch(Exception e){throw new RuntimeException(e);}});}
}
