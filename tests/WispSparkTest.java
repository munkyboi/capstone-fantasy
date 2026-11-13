import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

public final class WispSparkTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Random random(double roll){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return roll;}};}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    public static void main(String[] args)throws Exception{
        for(double roll:new double[]{.199,.2}){
            GameProgress p=new GameProgress(List.of(Student.BOGART,Student.ANGEL,Student.CHARICE));Battle b=new Battle(p,random(roll));Enemy w=b.getEnemies().get(0);
            check(w.getSpeed()==3&&w.getActionThreshold()==36&&w.getSp()==80,"wisp stats");
            for(int i=0;i<3;i++){check(b.enemyAttack(w),"wisp action");if(i<2)b.getParty().forEach(s->s.heal(999));}
            check(w.getSp()==68&&b.hasPendingSparkExplosion()==(roll<.2),"SP and explosion chance boundary");
            if(roll<.2){List<Student> party=b.getParty();int[] hp=party.stream().mapToInt(Student::getHp).toArray();
                check(!b.enemyAttack(w),"other actions wait for explosion");check(b.resolveSparkExplosion(),"resolve explosion");
                for(int i=0;i<party.size();i++){int expected=Battle.damageAfterDefense(16*(i==0?2:1),party.get(i).getDefense());check(hp[i]-party.get(i).getHp()==expected,"explosion damage " + i);}
            }
        }
        GameProgress low=new GameProgress(List.of(Student.BOGART));Battle b=new Battle(low,random(.5));Enemy w=b.getEnemies().get(0);Field sp=Enemy.class.getDeclaredField("skillPoints");sp.setAccessible(true);sp.setInt(w,11);
        for(int i=0;i<4;i++){check(b.enemyAttack(w),"low SP action");low.getParty().get(0).heal(999);check(b.getLastAbility()==null,"no spark without SP");}
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(List.of(Student.BOGART));BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),random(.1));panel.start(p,true,0);
            Battle battle=(Battle)field(panel,"battle");Enemy enemy=battle.getEnemies().get(0);
            @SuppressWarnings("unchecked") Map<Enemy,Double> enemyPoints=(Map<Enemy,Double>)field(panel,"enemyPoints");
            @SuppressWarnings("unchecked") Map<Student,Double> heroPoints=(Map<Student,Double>)field(panel,"actionPoints");
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);
            for(int i=0;i<3;i++){((javax.swing.Timer)field(panel,"monsterTurnTimer")).stop();heroPoints.replaceAll((k,v)->0.0);enemyPoints.put(enemy,(double)enemy.getActionThreshold());advance.invoke(panel,0.001);p.getParty().get(0).heal(999);}
            javax.swing.Timer timer=(javax.swing.Timer)field(panel,"sparkExplosionTimer");check(battle.hasPendingSparkExplosion()&&timer.isRunning()&&timer.getInitialDelay()==300,"UI explosion delay");timer.stop();
            for(var listener:timer.getActionListeners())listener.actionPerformed(new java.awt.event.ActionEvent(timer,0,""));check(!battle.hasPendingSparkExplosion(),"UI resolves explosion");
            for(String name:List.of("sparkExplosionTimer","angelAttackTimer","effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,name)).stop();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Wisp Arcane Spark checks passed");
    }
}
