import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** DOT clears on knockout and advances for waiting heroes during escape. */
public final class DotEscapeAndKnockoutTest {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    @SuppressWarnings("unchecked") static void addDot(Battle b,GameCharacter s,int damage)throws Exception{
        Class<?> type=Class.forName("game.model.BattleDotEngine$DamageOverTime");Constructor<?> ctor=type.getDeclaredConstructor(GameCharacter.class,GameCharacter.class,int.class);ctor.setAccessible(true);
        GameCharacter source=s instanceof Student?b.getEnemies().get(0):b.getParty().get(0);
        ((List<Object>)field(field(b,"dots"),"burns")).add(ctor.newInstance(source,s,damage));
    }
    static void key(BattlePanel p,String name){p.getActionMap().get(name).actionPerformed(new ActionEvent(p,0,""));}
    static void stop(BattlePanel p)throws Exception{for(String n:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","sparkExplosionTimer","abilityAlertTimer"))((javax.swing.Timer)field(p,n)).stop();}
    public static void main(String[] args)throws Exception{
        Random fixed=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};
        for(boolean useHug:new boolean[]{false,true}){
            GameProgress p=new GameProgress(List.of(Student.BOGART,Student.ANGEL));Battle b=new Battle(p,fixed);Student victim=p.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();Student healer=p.getParty().stream().filter(s->s.getId()==Student.ANGEL).findFirst().orElseThrow();
            addDot(b,victim,50);addDot(b,victim,20);victim.takeDamage(victim.getHp()-1);
            check(b.enemyAttack(b.getEnemies().get(0))&&!victim.isAlive(),"enemy knocks out afflicted hero");
            check(((List<?>)field(field(b,"dots"),"burns")).isEmpty(),"all DOT cleared immediately on KO");
            check(b.selectStudent(healer),"reviver ready");check(useHug?b.ability(victim,null):b.useWhiteFlower(victim),"revival succeeds");
            check(victim.isAlive()&&!b.hasDamageOverTime(victim)&&!b.tickDamageOverTime(victim),"revival does not restore DOT");
        }
        GameProgress external=new GameProgress(List.of(Student.BOGART,Student.ANGEL));Battle outside=new Battle(external,fixed);Student fallen=external.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();Student angel=external.getParty().stream().filter(s->s.getId()==Student.ANGEL).findFirst().orElseThrow();addDot(outside,fallen,50);fallen.takeDamage(9999);check(outside.selectStudent(angel)&&outside.ability(fallen,null)&&!outside.hasDamageOverTime(fallen),"revival clears stale externally caused KO effects");
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(List.of(Student.BOGART,Student.ANGEL));Random noRegen=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return .5;}};BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),noRegen);int[] exits={0};panel.setEscapeAction(x->exits[0]++);panel.start(p,false,0);Battle b=(Battle)field(panel,"battle");Student lead=b.getCurrent(),other=b.getParty().stream().filter(s->s!=lead).findFirst().orElseThrow();
            addDot(b,lead,50);addDot(b,other,50);int leadHp=lead.getHp(),otherHp=other.getHp();for(int i=0;i<4;i++)key(panel,"battle-down");key(panel,"battle-confirm");
            check(b.isEscaping()&&b.getCurrent()==null&&other.getHp()==otherHp,"no tick for a bar already full before DOT is applied");
            @SuppressWarnings("unchecked") Map<Enemy,Double> ep=(Map<Enemy,Double>)field(panel,"enemyPoints");ep.replaceAll((e,v)->-1000.0);
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);b.advanceDamageOverTime(1.0); advance.invoke(panel,5.1);
            check(lead.getHp()==leadHp-10&&other.getHp()==otherHp-10&&b.getCurrent()==null&&exits[0]==0,"both heroes continue DOT cycles without commands");
            advance.invoke(panel,1.0);check(exits[0]==1,"escape still completes on initiator's third action");stop(panel);
            GameProgress parallel=new GameProgress(List.of(Student.BOGART));BattlePanel both=new BattlePanel(()->{},x->{},new GameSettings(),noRegen);both.start(parallel,true,0);
            Battle simultaneous=(Battle)field(both,"battle");Student hero=parallel.getParty().get(0);Enemy foe=simultaneous.getEnemies().get(0);addDot(simultaneous,hero,50);addDot(simultaneous,foe,50);
            @SuppressWarnings("unchecked") Map<Student,Double> heroBars=(Map<Student,Double>)field(both,"actionPoints");@SuppressWarnings("unchecked") Map<Enemy,Double> monsterBars=(Map<Enemy,Double>)field(both,"enemyPoints");
            heroBars.put(hero,(double)hero.getActionThreshold()-1);monsterBars.put(foe,(double)foe.getActionThreshold()-1);int heroBefore=hero.getHp(),foeBefore=foe.getHp();
            simultaneous.advanceDamageOverTime(1.0); advance.invoke(both,.1);check(hero.getHp()==heroBefore-10&&foe.getHp()==foeBefore-10&&simultaneous.getCurrent()==hero,"both DOTs tick on their timers even though hero acts first");
            advance.invoke(both,.2);check(hero.getHp()==heroBefore-10&&foe.getHp()==foeBefore-10,"same fills do not tick twice while choosing");
            key(both,"battle-confirm");key(both,"battle-confirm");int afterAttack=foe.getHp();advance.invoke(both,0.0);
            check(foe.getHp()==afterAttack && simultaneous.getEffects().stream().noneMatch(e->e.getEnemy()==foe&&e.getAmount()==-10),"enemy DOT indicator is not repeated on its action");stop(both);
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("DOT escape and knockout checks passed");
    }
}
