import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.*;
import javax.swing.*;

public final class AngelAttackTest {
    static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    static Random fixed(){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};}
    static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void testCombo(boolean gloves){
        GameProgress p=new GameProgress(List.of(Student.ANGEL)); Student angel=p.getParty().get(0);
        if(gloves){p.getInventory().addEquipment("giant_slayer_gloves");check(p.equip(angel,Equipment.Slot.WEAPON,"giant_slayer_gloves"),"equip gloves");}
        p.advance();p.advance();p.advance(); Battle b=Battle.merchantBattle(p,fixed(),GameSettings.Difficulty.NORMAL);Enemy enemy=b.getEnemies().get(0);int hp=enemy.getHp();
        check(b.selectStudent(angel)&&b.attack(enemy),"first hit");int count=1,total=hp-enemy.getHp();
        check(!b.attack(enemy),"controls locked during combo");
        while(!b.isAngelExplosionPending()){int before=enemy.getHp();check(b.advanceAngelAttack(),"next hit");total+=before-enemy.getHp();count++;}
        check(count==(gloves?5:3),"number of punches");int before=enemy.getHp();check(b.advanceAngelAttack(),"explosion");
        check(before-enemy.getHp()==(int)Math.ceil(total*.80),"explosion is 80 percent of actual damage");
        check(!b.hasPendingAngelAttack()&&b.getCurrent()==null,"one action finishes");
    }
    static void testEquipmentCombo(int percentRoll){
        GameProgress p=new GameProgress(List.of(Student.ANGEL));Student angel=p.getParty().get(0);
        p.getInventory().addEquipment("giant_slayer_gloves");p.getInventory().addEquipment("looper_ring");
        check(p.equip(angel,Equipment.Slot.WEAPON,"giant_slayer_gloves") && p.equip(angel,Equipment.Slot.ACCESSORY,"looper_ring"),"equip both items");
        p.advance();p.advance();p.advance();Random rolls=new Random(){public int nextInt(int n){return n==21?percentRoll:0;}public double nextDouble(){return 0;}};
        Battle b=Battle.merchantBattle(p,rolls,GameSettings.Difficulty.INSANE);Enemy enemy=b.getEnemies().get(0);int start=enemy.getHp();
        check(b.selectStudent(angel)&&b.attack(enemy),"start equipment combo");
        for(int i=1;i<5;i++)check(b.advanceAngelAttack(),"glove punch");
        check(b.hasPendingLooperHits() && b.isAngelExplosionPending() && !b.advanceAngelAttack(),"looper hits precede explosion");
        for(int i=0;i<5;i++)check(b.advanceLooperHit(),"looper hit");
        int total=start-enemy.getHp(), before=enemy.getHp();
        check(b.advanceAngelAttack(),"explode after all hits");
        check(before-enemy.getHp()==(int)Math.ceil(total*(80+percentRoll)/100.0),"explosion includes glove and looper damage");
        check(!b.isActionAnimating(),"combo finishes once");
    }
    public static void main(String[] args)throws Exception{
        testCombo(false);testCombo(true);testEquipmentCombo(0);testEquipmentCombo(20);
        GameProgress p=new GameProgress(List.of(Student.ANGEL,Student.BOGART));Student angel=p.getParty().stream().filter(s->s.getId()==Student.ANGEL).findFirst().orElseThrow();Student ally=p.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();
        ally.takeDamage(9999);ally.restoreSp(0);Battle b=new Battle(p,fixed());check(b.selectStudent(angel)&&b.ability(ally,null),"Angel Hug revives");
        check(ally.getHp()==45&&ally.getSp()==11,"revive restores 30 percent HP and SP rounded up");
        check(b.getLastAbility().skill().equals("Angel Hug"),"renamed ability");
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress uiParty=new GameProgress(List.of(Student.ANGEL));uiParty.advance();uiParty.advance();uiParty.advance();BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),fixed());panel.startMerchant(uiParty);
            Battle battle=(Battle)field(panel,"battle");panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));
            javax.swing.Timer timer=(javax.swing.Timer)field(panel,"angelAttackTimer");check(timer.isRunning()&&timer.getInitialDelay()==300,"punch delay");timer.stop();
            for(int i=0;i<2;i++){for(var listener:timer.getActionListeners())listener.actionPerformed(new ActionEvent(timer,0,""));timer.stop();}
            check(battle.isAngelExplosionPending()&&timer.getInitialDelay()==300,"explosion delay");
            for(var listener:timer.getActionListeners())listener.actionPerformed(new ActionEvent(timer,0,""));
            for(String n:List.of("angelAttackTimer","effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,n)).stop();
            GameProgress revival=new GameProgress(List.of(Student.ANGEL,Student.BOGART));Student uiAngel=revival.getParty().stream().filter(s->s.getId()==Student.ANGEL).findFirst().orElseThrow();
            Student ko=revival.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();ko.takeDamage(9999);ko.restoreSp(0);
            BattlePanel healer=new BattlePanel(()->{},x->{},new GameSettings(),fixed());healer.start(revival,false,0);Battle healBattle=(Battle)field(healer,"battle");
            @SuppressWarnings("unchecked") Map<Student,Double> points=(Map<Student,Double>)field(healer,"actionPoints");points.put(uiAngel,(double)uiAngel.getActionThreshold());points.put(ko,17.0);check(healBattle.getCurrent()==uiAngel || healBattle.selectStudent(uiAngel),"select healer");
            healer.getActionMap().get("battle-down").actionPerformed(new ActionEvent(healer,0,""));healer.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(healer,0,""));
            healer.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(healer,0,""));
            check(ko.isAlive() && ko.getHp()==45 && ko.getSp()==11 && points.get(ko)==0.0,"keyboard selects fallen ally and resets their AP");
            for(String n:List.of("angelAttackTimer","effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","abilityAlertTimer"))((javax.swing.Timer)field(healer,n)).stop();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Angel combo and Angel Hug checks passed");
    }
}
