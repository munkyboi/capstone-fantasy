import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

/** DOT runs on one-second clocks, even while the player is choosing commands. */
public final class TimedDotTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static Random hits() { return new Random() { public int nextInt(int n) { return 0; } public double nextDouble() { return 0; } }; }
    static Object field(Object object, String name) throws Exception { Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    @SuppressWarnings("unchecked") static Enemy addTarget(Battle battle) throws Exception {
        Enemy enemy = new Enemy("Target", true); Field hp=Enemy.class.getDeclaredField("hp");hp.setAccessible(true);hp.setInt(enemy,10000);
        List<Enemy> enemies=(List<Enemy>)field(battle,"enemies"); enemies.clear();enemies.add(enemy);return enemy;
    }
    public static void main(String[] args) throws Exception {
        GameProgress p=new GameProgress(List.of(Student.CHARICE)); Student hero=p.getParty().get(0); Battle battle=new Battle(p,hits()); Enemy target=addTarget(battle);
        check(battle.selectStudent(hero)&&battle.ability(null,null)&&battle.hasDamageOverTime(target),"rain DOT applied"); hero.restoreSp(0);
        int hp=target.getHp(); check(!battle.advanceDamageOverTime(.99)&&target.getHp()==hp,"no early tick");
        check(battle.advanceDamageOverTime(.01)&&target.getHp()<hp&&hero.getSp()==5,"first second deals damage and restores SP");
        check(battle.advanceDamageOverTime(4)&&!battle.hasDamageOverTime(target)&&hero.getSp()==25,"five ticks finish and restore 25 SP");
        SwingUtilities.invokeAndWait(()->{try {
            GameProgress ui=new GameProgress(List.of(Student.CHARICE)); BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),hits());panel.start(ui,false,0);
            Battle fight=(Battle)field(panel,"battle");Enemy foe=addTarget(fight);Student archer=ui.getParty().get(0);
            check(fight.ability(null,null)&&fight.selectStudent(archer),"apply DOT and reopen command selection");int before=foe.getHp();
            Field dotClock=BattlePanel.class.getDeclaredField("lastDotNanos");dotClock.setAccessible(true);dotClock.setLong(panel,System.nanoTime()-1_010_000_000L);
            Field apClock=BattlePanel.class.getDeclaredField("lastTickNanos");apClock.setAccessible(true);apClock.setLong(panel,System.nanoTime());
            Method tick=BattlePanel.class.getDeclaredMethod("tickActionPoints");tick.setAccessible(true);tick.invoke(panel);
            check(foe.getHp()<before&&fight.getCurrent()==archer,"DOT ticks while selecting a command");
            for(String name:List.of("apTimer","effectTimer","ambushAlertTimer","victoryTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,name)).stop();
        } catch(Exception ex){throw new RuntimeException(ex);} });
        System.out.println("Timed DOT checks passed");
    }
}
