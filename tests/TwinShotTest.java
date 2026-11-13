import game.model.*;
import java.lang.reflect.*;
import java.util.*;
import game.view.BattlePanel;
import javax.swing.*;
import java.awt.event.ActionEvent;

/** Verifies timed Twin Shot damage, temporary defense reduction, and follow-up rolls. */
public final class TwinShotTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Random rolls(double... values){return new Random(){int i; public int nextInt(int n){return 0;} public double nextDouble(){return values[Math.min(i++,values.length-1)];}};}
    @SuppressWarnings("unchecked") static void addTarget(Battle battle,Enemy enemy)throws Exception{
        Field f=Battle.class.getDeclaredField("enemies");f.setAccessible(true);List<Enemy> enemies=(List<Enemy>)f.get(battle);enemies.clear();enemies.add(enemy);
    }
    static Enemy target(){return Enemy.sentinel("Attack",true,GameSettings.Difficulty.NORMAL,0,1,0);}
    public static void main(String[] args)throws Exception{
        GameProgress p=new GameProgress(List.of(Student.CHARICE));Student hero=p.getParty().get(0);Battle b=new Battle(p,rolls(0));Enemy e=target();addTarget(b,e);
        check(hero.getAbilitySpCost()==5&&b.selectStudent(hero)&&b.ability(null,e)&&hero.getSp()==55,"first shot costs 5 SP");
        check(e.getHp()==184&&e.getDefense()==-1&&b.hasPendingTwinShot()&&b.twinShotDelayMillis()==300,"150% current ATK first shot and 300ms delay");
        check(b.getEffects().stream().anyMatch(effect->"DEF -10".equals(effect.getLabel())),"actual defense reduction is visible");
        check(!b.attack(e)&&b.advanceTwinShot()&&e.getHp()==172&&b.twinShotDelayMillis()==300,"second shot uses reduced defense and 300ms delay");
        check(b.advanceTwinShot()&&e.getHp()==160&&!b.isActionAnimating()&&b.getCurrent()==null&&e.getDefense()==9,"bonus shot resolves and defense is restored");
        GameProgress q=new GameProgress(List.of(Student.CHARICE));Student h=q.getParty().get(0);Battle boundary=new Battle(q,rolls(0,.5,.5));Enemy foe=target();addTarget(boundary,foe);
        check(boundary.selectStudent(h)&&boundary.ability(null,foe)&&boundary.advanceTwinShot(),"boundary cast");
        check(foe.getHp()==173&&!boundary.hasPendingTwinShot(),"50 percent boundary neither reduces defense nor triggers extra shot");
        GameProgress m=new GameProgress(List.of(Student.CHARICE));Student mh=m.getParty().get(0);Battle miss=new Battle(m,rolls(.999,.999));Enemy me=target();addTarget(miss,me);
        check(miss.selectStudent(mh)&&miss.ability(null,me)&&me.getHp()==200&&miss.advanceTwinShot()&&me.getHp()==189&&!miss.hasPendingTwinShot(),"second shot always hits after missed first shot");
        GameProgress scaledParty=new GameProgress(List.of(Student.CHARICE));Student scaledHero=scaledParty.getParty().get(0);
        Battle scaled=new Battle(scaledParty,rolls(0));Enemy veteran=Enemy.sentinel("Veteran",true,GameSettings.Difficulty.INSANE,0,50,0);addTarget(scaled,veteran);
        int initialDef=veteran.getDefense();double x=49.0/99.0,curve=x*x*(3-2*x);int penalty=(int)Math.ceil(20*(1+2.97*curve));
        check(scaled.selectStudent(scaledHero)&&scaled.ability(null,veteran)&&veteran.getDefense()==Math.max(-20,initialDef-penalty),"defense penalty scales with level and difficulty");
        scaled.advanceTwinShot();scaled.advanceTwinShot();check(veteran.getDefense()==initialDef,"scaled defense restored after skill");
        GameProgress capParty=new GameProgress(List.of(Student.CHARICE));Student capHero=capParty.getParty().get(0);
        Battle capBattle=new Battle(capParty,rolls(0));Enemy capTarget=new Enemy("Wisp",false,false,GameSettings.Difficulty.INSANE,0,100);addTarget(capBattle,capTarget);
        check(capBattle.selectStudent(capHero)&&capBattle.ability(null,capTarget)&&capTarget.getDefense()==-20,"negative defense is capped at minus 20");
        capBattle.advanceTwinShot();capBattle.advanceTwinShot();check(capTarget.getDefense()>0,"capped defense returns to normal");
        GameProgress wispParty=new GameProgress(List.of(Student.CHARICE));Student archer=wispParty.getParty().get(0);
        Battle wispBattle=new Battle(wispParty,rolls(0));Enemy wisp=new Enemy("Wisp");addTarget(wispBattle,wisp);
        check(wispBattle.selectStudent(archer)&&wispBattle.ability(null,wisp)&&wisp.getDefense()==-5,"wisp defense becomes negative");
        int beforeFollowup=wisp.getHp();check(wispBattle.advanceTwinShot()&&beforeFollowup-wisp.getHp()==12,"negative defense increases followup damage");
        GameProgress shieldParty=new GameProgress(List.of(Student.CHARICE));Student sh=shieldParty.getParty().get(0);
        Battle shieldBattle=new Battle(shieldParty,rolls(0));Enemy shield=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,1,0);addTarget(shieldBattle,shield);
        check(shieldBattle.enemyAttack(shield)&&shield.getDefense()==22,"Stone Skin starts active");
        check(shieldBattle.selectStudent(sh)&&shieldBattle.ability(null,shield)&&shield.getDefense()==12,"Twin Shot reduces shielded defense");
        check(shieldBattle.advanceTwinShot()&&shieldBattle.advanceTwinShot()&&shield.getDefense()==22&&shield.getStoneSkinHits()==2,"ending the skill preserves remaining Stone Skin hits");
        GameProgress l=new GameProgress(List.of(Student.CHARICE));Student lh=l.getParty().get(0);l.getInventory().addEquipment("looper_ring");check(l.equip(lh,Equipment.Slot.ACCESSORY,"looper_ring"),"equip looper");
        Battle loop=new Battle(l,rolls(0));Enemy le=target();addTarget(loop,le);check(lh.getAbilitySpCost()==6&&loop.selectStudent(lh)&&loop.ability(null,le),"looper cost");
        check(loop.advanceTwinShot()&&loop.advanceTwinShot()&&!loop.hasPendingTwinShot()&&loop.hasPendingLooperHits(),"looper begins after skill");
        int count=0;while(loop.hasPendingLooperHits()){check(loop.advanceLooperHit(),"extra hit");count++;}check(count==5&&loop.getCurrent()==null,"five extras then turn ends");
        Student leveled=new Student(Student.CHARICE);leveled.gainExperience(100);check(leveled.getAbilitySpCost()==6,"SP cost scales with level");
        SwingUtilities.invokeAndWait(()->{try {
            BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),rolls(0));
            panel.start(new GameProgress(List.of(Student.CHARICE)),false,0);
            Field battleField=BattlePanel.class.getDeclaredField("battle");battleField.setAccessible(true);
            Battle uiBattle=(Battle)battleField.get(panel);addTarget(uiBattle,target());
            panel.getActionMap().get("battle-down").actionPerformed(new ActionEvent(panel,0,""));
            panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));
            panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));
            Field timerField=BattlePanel.class.getDeclaredField("twinShotTimer");timerField.setAccessible(true);
            javax.swing.Timer timer=(javax.swing.Timer)timerField.get(panel);
            check(timer.isRunning()&&timer.getInitialDelay()==300,"battle screen schedules second shot at 300ms");
            Method advance=BattlePanel.class.getDeclaredMethod("advanceTwinShot");advance.setAccessible(true);advance.invoke(panel);
            check(timer.isRunning()&&timer.getInitialDelay()==300,"battle screen schedules extra shot at 300ms");
            timer.stop();
            for(String name:List.of("apTimer","effectTimer","ambushAlertTimer","victoryTimer","secondCastTimer","looperHitTimer","angelAttackTimer","sparkExplosionTimer","abilityAlertTimer")){
                Field f=BattlePanel.class.getDeclaredField(name);f.setAccessible(true);((javax.swing.Timer)f.get(panel)).stop();
            }
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("Twin Shot checks passed");
    }
}
