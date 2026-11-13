import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Escape countdown, penalties, cancellation and battle UI transition. */
public final class HeroEscapeTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void key(BattlePanel p,String name){p.getActionMap().get(name).actionPerformed(new ActionEvent(p,0,""));}
    static void stop(BattlePanel p)throws Exception{for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","sparkExplosionTimer","abilityAlertTimer"))((javax.swing.Timer)field(p,name)).stop();}
    public static void main(String[] args)throws Exception{
        Student near=Student.create(Student.ZACK);near.configureProgression(7,30);near.gainExperience(104);
        check(near.getLevel()==2 && near.getXpProgress()==4 && near.applyEscapeXpPenalty()==4 && near.getXpProgress()==0 && near.getLevel()==2,"XP penalty capped within level");
        Student normal=Student.create(Student.ZACK);normal.configureProgression(7,30);normal.gainExperience(150);
        check(normal.applyEscapeXpPenalty()==11 && normal.getXpProgress()==39,"10 percent target XP rounded up");
        Student capped=Student.create(Student.ZACK);capped.configureProgression(7,20);capped.gainExperience(Long.MAX_VALUE);check(capped.applyEscapeXpPenalty()==0&&capped.getLevel()==20,"cap has no progress to lose");
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(List.of(Student.ZACK));Student hero=p.getParty().get(0);hero.configureProgression(7,30);hero.gainExperience(150);p.addPetot(105);
            int[] returned={0},victories={0};BattlePanel panel=new BattlePanel(()->{},ignored->victories[0]++,new GameSettings(),new Random(2));panel.setEscapeAction(saved->{check(saved==p,"same progress");returned[0]++;});panel.start(p,false,0);
            Battle battle=(Battle)field(panel,"battle");int stage=p.getEncounter();for(int i=0;i<4;i++)key(panel,"battle-down");key(panel,"battle-confirm");
            check(battle.isEscaping()&&battle.getEscapeTurnsRemaining()==2&&battle.getCurrent()==null,"first action starts three-turn escape");
            @SuppressWarnings("unchecked") Map<Student,Double> ap=(Map<Student,Double>)field(panel,"actionPoints");
            @SuppressWarnings("unchecked") Map<Enemy,Double> enemyAp=(Map<Enemy,Double>)field(panel,"enemyPoints");enemyAp.replaceAll((e,v)->0.0);
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);
            ap.put(hero,(double)hero.getActionThreshold());advance.invoke(panel,0.0);
            check(battle.isEscaping()&&battle.getEscapeTurnsRemaining()==1&&returned[0]==0&&p.getPetot()==105,"second turn no penalty");
            ap.put(hero,(double)hero.getActionThreshold());advance.invoke(panel,0.0);
            check(returned[0]==1&&victories[0]==0&&!battle.isVictory()&&p.getEncounter()==stage,"third turn returns to route without victory");
            check(p.getPetot()==94&&hero.getLevel()==2&&hero.getXpProgress()==39,"penalties applied once with ceiling");
            stop(panel);
            GameProgress waiting=new GameProgress(List.of(Student.ZACK,Student.BOGART));int[] escaped={0};
            Random fixed=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return .5;}};
            BattlePanel group=new BattlePanel(()->{},ignored->{},new GameSettings(),fixed);group.setEscapeAction(saved->escaped[0]++);group.start(waiting,false,0);
            Battle groupBattle=(Battle)field(group,"battle");Student initiator=groupBattle.getCurrent();Student companion=groupBattle.getParty().stream().filter(s->s!=initiator).findFirst().orElseThrow();
            for(int i=0;i<4;i++)key(group,"battle-down");key(group,"battle-confirm");
            check(groupBattle.isEscaping() && groupBattle.getCurrent()==null && !groupBattle.selectStudent(companion),"other ready heroes cannot act while escaping");
            @SuppressWarnings("unchecked") Map<Enemy,Double> groupEnemies=(Map<Enemy,Double>)field(group,"enemyPoints");Enemy attacker=groupBattle.getEnemies().get(0);
            groupEnemies.put(attacker,(double)attacker.getActionThreshold()-1);int initialHp=groupBattle.getParty().stream().mapToInt(Student::getHp).sum();
            advance.invoke(group,0.2);check(groupBattle.getCurrent()==null && groupBattle.getParty().stream().mapToInt(Student::getHp).sum()<initialHp,"enemies act while all heroes wait");
            advance.invoke(group,7.0);check(escaped[0]==1,"ready companions do not stall the escape countdown");stop(group);
            GameProgress failure=new GameProgress(List.of(Student.ZACK,Student.BOGART));failure.addPetot(100);Battle failed=new Battle(failure,new Random(2));Student lead=failed.getParty().get(0);
            check(failed.selectStudent(lead)&&failed.beginEscape(),"begin cancellable escape");lead.takeDamage(9999);
            check(failed.cancelEscapeIfKnockedOut()&&!failed.isEscaping()&&failure.getPetot()==100,"KO cancels without penalty");
            check(!failed.advanceEscape(lead),"fallen initiator cannot complete");
            GameProgress interrupted=new GameProgress(List.of(Student.ZACK,Student.BOGART));BattlePanel interruptedPanel=new BattlePanel(()->{},ignored->{},new GameSettings(),fixed);interruptedPanel.start(interrupted,false,0);
            Battle interruptedBattle=(Battle)field(interruptedPanel,"battle");Student interruptedLead=interruptedBattle.getCurrent();
            for(int i=0;i<4;i++)key(interruptedPanel,"battle-down");key(interruptedPanel,"battle-confirm");interruptedLead.takeDamage(9999);
            advance.invoke(interruptedPanel,0.0);check(!interruptedBattle.isEscaping()&&interruptedBattle.getCurrent()!=null&&interruptedBattle.getCurrent()!=interruptedLead,"surviving hero can act after escape fails");stop(interruptedPanel);
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Hero Escape checks passed");
    }
}
