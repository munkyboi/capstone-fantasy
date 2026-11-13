import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
public final class RainOfArrowsTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static final class Rolls extends Random { double chance; int targetIndex; public int nextInt(int n){return n==2?Math.floorMod(targetIndex++,2):0;} public double nextDouble(){return chance;} }
 @SuppressWarnings("unchecked") static void targets(Battle b,Enemy... e)throws Exception{Field f=Battle.class.getDeclaredField("enemies");f.setAccessible(true);List<Enemy> list=(List<Enemy>)f.get(b);list.clear();list.addAll(List.of(e));for(Enemy enemy:e){Field hp=Enemy.class.getDeclaredField("hp");hp.setAccessible(true);hp.setInt(enemy,10000);}}
 public static void main(String[] args)throws Exception{
  GameProgress p=new GameProgress(List.of(Student.CHARICE));Student hero=p.getParty().get(0);Rolls rng=new Rolls();Battle b=new Battle(p,rng);Enemy e=new Enemy("Target",true);targets(b,e);
  check(b.selectStudent(hero)&&b.ability(null,null),"rain targets all without choosing a specific enemy");check(hero.getSp()==55&&b.getLastAbility().skill().equals("Rain of Arrows"),"cost and announcement");
  check(b.getRainArrowTargets().size()==12&&b.getEffects().stream().filter(x->x.getEnemy()==e && x.getAmount()<0&&!x.isManaLeak()).count()==12,"twelve independently resolved arrows");
  check(b.hasDamageOverTime(e),"successful DOT roll");hero.restoreSp(30);int start=e.getHp();for(int i=0;i<5;i++){check(b.tickDamageOverTime(e),"DOT tick "+i);check(hero.getSp()==35+5*i,"5 SP restored on damaging tick");}
  check(!b.hasDamageOverTime(e)&&start-e.getHp()==hero.getAttack(),"five ticks deal 100% attack total");
  GameProgress q=new GameProgress(List.of(Student.CHARICE));Student archer=q.getParty().get(0);Rolls dodgeRng=new Rolls();Battle dodge=new Battle(q,dodgeRng);Enemy target=new Enemy("Target",true);targets(dodge,target);check(dodge.selectStudent(archer)&&dodge.ability(null,null),"apply DOT");archer.restoreSp(0);dodgeRng.chance=.999999;int hp=target.getHp();check(dodge.tickDamageOverTime(target)&&target.getHp()==hp&&archer.getSp()==0,"dodged DOT does not restore SP");
  GameProgress m=new GameProgress(List.of(Student.CHARICE));Student dual=m.getParty().get(0);Rolls mr=new Rolls();Battle multi=new Battle(m,mr);Enemy a=new Enemy("A",true),c=new Enemy("B",true);targets(multi,a,c);check(multi.selectStudent(dual)&&multi.ability(null,null),"multi-target volley");check(multi.getRainArrowTargets().containsAll(List.of(a,c))&&multi.hasDamageOverTime(a)&&multi.hasDamageOverTime(c),"random arrows and per-target DOT");
  for(int roll:new int[]{0,20}){
   GameProgress rangeParty=new GameProgress(List.of(Student.CHARICE));Student rangeHero=rangeParty.getParty().get(0);
   Random rangeRng=new Random(){public int nextInt(int n){return n==21?roll:0;}public double nextDouble(){return 0;}};
   Battle rangeBattle=new Battle(rangeParty,rangeRng);Enemy rangeTarget=new Enemy("Target",true);targets(rangeBattle,rangeTarget);
   int before=rangeTarget.getHp(),power=(int)Math.ceil(rangeHero.getAttack()*(40+roll)/100.0);
   int expected=Battle.damageAfterDefense(power,rangeTarget.getDefense());
   check(rangeBattle.selectStudent(rangeHero)&&rangeBattle.ability(null,null),"range volley");
   check(before-rangeTarget.getHp()==12*expected,"arrow damage bound "+(40+roll)+"%");
  }
  GameProgress insufficient=new GameProgress(List.of(Student.CHARICE));Student h=insufficient.getParty().get(0);h.restoreSp(1);Battle attack=new Battle(insufficient,new Rolls());Enemy foe=attack.getEnemies().get(0);check(attack.selectStudent(h)&&!attack.attack(foe)&&attack.getCurrent()==h&&h.getSp()==1,"insufficient Twin Shot SP keeps turn");h.restoreSp(4);check(!attack.ability(null,null)&&attack.getCurrent()==h&&h.getSp()==4,"insufficient Rain SP keeps turn");
  Student scaled=new Student(Student.CHARICE);scaled.gainExperience(100);check(scaled.getAbilitySpCost()==6,"ability cost scales with level");
  SwingUtilities.invokeAndWait(()->{try{GameProgress ui=new GameProgress(List.of(Student.CHARICE));BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),new Rolls());panel.start(ui,false,0);Field f=BattlePanel.class.getDeclaredField("battle");f.setAccessible(true);Battle fight=(Battle)f.get(panel);targets(fight,new Enemy("Target",true));panel.getActionMap().get("battle-down").actionPerformed(new ActionEvent(panel,0,""));panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));check(fight.getLastAbility()!=null&&fight.getLastAbility().skill().equals("Rain of Arrows"),"keyboard casts volley");JPanel scene=(JPanel)field(panel,"scene");scene.setSize(960,540);
   check(field(panel,"rainShooter")!=null&&((List<?>)field(panel,"rainTargets")).size()==12,"volley has an independent animation and actual targets");
   Field clock=BattlePanel.class.getDeclaredField("rainStartedNanos");clock.setAccessible(true);
   for(int elapsed:new int[]{150,500}){clock.setLong(panel,System.nanoTime()-elapsed*1_000_000L);scene.paint(new java.awt.image.BufferedImage(960,540,java.awt.image.BufferedImage.TYPE_INT_ARGB).getGraphics());}
   Field attacker=BattlePanel.class.getDeclaredField("attackingSprite");attacker.setAccessible(true);attacker.set(panel,null);
   check(field(panel,"rainShooter")!=null,"follow-up effects do not cancel the volley");for(String name:List.of("apTimer","effectTimer","ambushAlertTimer","victoryTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,name)).stop();}catch(Exception ex){throw new RuntimeException(ex);}});
  System.out.println("Rain of Arrows checks passed");
 }
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
}
