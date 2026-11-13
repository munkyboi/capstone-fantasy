import game.model.*;
import game.view.BattlePanel;
import java.util.*;
import java.lang.reflect.*;
import javax.swing.*;

/** Stone Mend heals living monsters, including the caster, never party members. */
public final class StoneMendTargetTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Random fixed(){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};}
    static GameProgress progress(){GameProgress p=new GameProgress(List.of(0,1,2));p.advance();return p;}
    public static void main(String[] args)throws Exception {
        GameProgress party=progress();Battle battle=new Battle(party,fixed());
        Enemy sentinel=battle.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Enemy wisp=battle.getEnemies().stream().filter(e->!e.isSentinel()).findFirst().orElseThrow();
        sentinel.takeDamage(61); int before=sentinel.getHp(),sp=sentinel.getSp();
        int[] hp=party.getParty().stream().mapToInt(Student::getHp).toArray();
        check(battle.enemyAttack(sentinel),"self heal action");
        check(sentinel.getHp()==before+13 && sentinel.getSp()==sp-sentinel.getHealSpCost(),"heals itself for 20% of missing HP rounded up");
        check(battle.getLastAbility().skill().equals("Stone Mend") && battle.getMessage().contains("heals " + sentinel.getName()),"self-heal log");
        check(battle.getEffects().size()==1 && battle.getEffects().get(0).getEnemy()==sentinel && battle.getEffects().get(0).getStudent()==null,"heal indicator targets monster");
        check(Arrays.equals(hp,party.getParty().stream().mapToInt(Student::getHp).toArray()),"healing never changes party HP");
        wisp.takeDamage(50);before=wisp.getHp();
        for(int i=0;i<4;i++) {
            check(battle.enemyAttack(sentinel) && wisp.getHp()==before
                    && (battle.getLastAbility()==null || !battle.getLastAbility().skill().equals("Stone Mend")), "four actions without Stone Mend");
        }
        check(battle.enemyAttack(sentinel) && wisp.getHp()==before+10,"heals the most injured monster for 20% of missing HP on fifth action");
        check(battle.getEffects().get(0).getEnemy()==wisp,"other monster receives indicator");
        SwingUtilities.invokeAndWait(()->{try {
            GameProgress p=progress();BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),fixed());panel.start(p,true,1);
            Field bf=BattlePanel.class.getDeclaredField("battle");bf.setAccessible(true);Battle b=(Battle)bf.get(panel);
            Enemy s=b.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow(); s.takeDamage(60);
            Field points=BattlePanel.class.getDeclaredField("enemyPoints");points.setAccessible(true);
            @SuppressWarnings("unchecked") Map<Enemy,Double> enemyPoints=(Map<Enemy,Double>)points.get(panel);
            enemyPoints.replaceAll((enemy,value)->-1000.0);enemyPoints.put(s,(double)s.getActionThreshold());
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);advance.invoke(panel,0.001);
            Field attacker=BattlePanel.class.getDeclaredField("attackingSprite");attacker.setAccessible(true);
            check(attacker.get(panel)==null,"healing caster does not lunge toward heroes");
            check(b.getEffects().get(0).getEnemy()==s,"battle screen retains correct heal target");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")){
                Field timer=BattlePanel.class.getDeclaredField(name);timer.setAccessible(true);((javax.swing.Timer)timer.get(panel)).stop();}
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}});
        System.out.println("Stone Mend targeting checks passed");
    }
}
