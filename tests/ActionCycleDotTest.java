import game.model.*;
import game.view.*;
import java.util.*;
import java.lang.reflect.*;
import javax.swing.*;

public final class ActionCycleDotTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(List.of(Student.BOGART));p.advance();p.advance();p.advance();
            Random r=new Random(){@Override public int nextInt(int n){return 0;}@Override public double nextDouble(){return 0;}};
            BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),r);panel.startMerchant(p);
            Field bf=BattlePanel.class.getDeclaredField("battle");bf.setAccessible(true);Battle b=(Battle)bf.get(panel);Enemy merchant=b.getEnemies().get(0);Student hero=p.getParty().get(0);
            check(b.attack(merchant)&&b.enemyAttack(merchant),"summon");b.getEnemies().stream().filter(Enemy::isMirror).forEach(e->e.takeDamage(9999));
            check(b.enemyAttack(merchant)&&b.getStunTurns(hero)==2,"merchant stuns");hero.heal(999);
            check(b.enemyAttack(merchant)&&b.enemyAttack(merchant)&&b.hasDamageOverTime(hero),"merchant applies DOT");hero.heal(999);int hp=hero.getHp();
            Field ap=BattlePanel.class.getDeclaredField("actionPoints");ap.setAccessible(true);@SuppressWarnings("unchecked") Map<Student,Double> points=(Map<Student,Double>)ap.get(panel);
            Field ep=BattlePanel.class.getDeclaredField("enemyPoints");ep.setAccessible(true);@SuppressWarnings("unchecked") Map<Enemy,Double> enemyPoints=(Map<Enemy,Double>)ep.get(panel);enemyPoints.replaceAll((e,v)->0.0);
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);
            check(b.advanceDamageOverTime(1.0)&&hero.getHp()==hp-22,"first tick while stunned");
            points.put(hero,(double)hero.getActionThreshold());advance.invoke(panel,0.0);
            check(b.getStunTurns(hero)==1&&points.get(hero)==0.0,"stun consumes an AP cycle");
            check(b.advanceDamageOverTime(1.0)&&hero.getHp()==hp-44,"second timed tick");
            points.put(hero,(double)hero.getActionThreshold());advance.invoke(panel,0.0);
            check(b.getStunTurns(hero)==0,"second stun cycle ends");
            points.put(hero,(double)hero.getActionThreshold());advance.invoke(panel,0.0);
            check(b.getCurrent()==hero,"hero can select a command");
            check(b.advanceDamageOverTime(1.0)&&hero.getHp()==hp-66,"DOT continues during selection");
            hero.takeDamage(hero.getHp()-1);
            check(b.advanceDamageOverTime(1.0)&&b.isDefeat()&&b.getCurrent()==null,"DOT knockout ends battle during selection");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")){Field t=BattlePanel.class.getDeclaredField(name);t.setAccessible(true);((javax.swing.Timer)t.get(panel)).stop();}
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}});
        System.out.println("Action cycle DOT checks passed");
    }
}
