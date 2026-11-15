import game.model.*;
import java.lang.reflect.*;
import java.util.*;

public final class StoneSkinReflectionTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Random fixed(){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};}
 @SuppressWarnings("unchecked") static void enemies(Battle b,Enemy... values)throws Exception{Field f=Battle.class.getDeclaredField("enemies");f.setAccessible(true);List<Enemy> list=(List<Enemy>)f.get(b);list.clear();list.addAll(List.of(values));}
 public static void main(String[]args)throws Exception {
  GameProgress p=new GameProgress(List.of(Student.BOGART));Student hero=p.getParty().get(0);Battle b=new Battle(p,fixed());Enemy shield=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,1,0);enemies(b,shield);
  check(b.enemyAttack(shield)&&shield.getDefense()==22&&shield.getAgility()==10&&shield.getStoneSkinHits()==5,"skin applies DEF and agility");
  int beforeHp=hero.getHp(),beforeEnemy=shield.getHp();check(b.selectStudent(hero)&&b.attack(shield),"attack shield");int dealt=beforeEnemy-shield.getHp();
  check(hero.getHp()==beforeHp-(dealt+9)/10,"reflects 10% of actual damage rounded up");check(shield.getStoneSkinHits()==4,"hit uses one charge");
  check(b.getEffects().stream().anyMatch(e->e.getEnemy()==shield&&"Reflect".equals(e.getLabel())),"reflection indicator on shielded monster");
  check(b.getEffects().stream().noneMatch(e->e.getStudent()==hero&&"Reflect".equals(e.getLabel())),"reflection label is not on hero");
  check(b.getEffects().stream().anyMatch(e->e.getStudent()==hero&&e.getAmount()<0),"reflected damage indicator remains on hero");
  for(int i=0;i<4;i++)shield.takeDamage(1);check(shield.getStoneSkinHits()==0&&shield.getAgility()==5&&shield.getDefense()==12,"bonuses expire after fifth hit");
  GameProgress q=new GameProgress(List.of(Student.ANGEL));Student angel=q.getParty().get(0);Battle fatal=new Battle(q,fixed());Enemy target=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,1,0);enemies(fatal,target);check(fatal.enemyAttack(target),"shield cast");angel.takeDamage(angel.getHp()-1);check(fatal.selectStudent(angel)&&fatal.attack(target)&&!angel.isAlive()&&!fatal.hasPendingAngelAttack(),"reflection KO ends combo");
  GameProgress dotProgress=new GameProgress(List.of(Student.BOGART));Student dotHero=dotProgress.getParty().get(0);Battle dotBattle=new Battle(dotProgress,fixed());
  Enemy dotShield=Enemy.sentinel("DOT shield",false,GameSettings.Difficulty.NORMAL,0,1,0);enemies(dotBattle,dotShield);check(dotBattle.enemyAttack(dotShield),"DOT shield cast");
  Field dotField=Battle.class.getDeclaredField("dots");dotField.setAccessible(true);Object dots=dotField.get(dotBattle);
  Method addDot=dots.getClass().getDeclaredMethod("add",GameCharacter.class,GameCharacter.class,int.class,boolean.class,int.class,int.class);addDot.setAccessible(true);
  addDot.invoke(dots,dotHero,dotShield,50,false,5,0);int dotHeroHp=dotHero.getHp();
  check(dotBattle.tickDamageOverTime(dotShield)&&dotHero.getHp()==dotHeroHp-1,"DOT damage is reflected to hero");
  check(dotBattle.getEffects().stream().anyMatch(e->e.getEnemy()==dotShield&&"Reflect".equals(e.getLabel())),"DOT reflection indicator on shielded monster");
  check(dotBattle.getEffects().stream().anyMatch(e->e.getStudent()==dotHero&&e.getAmount()==-1),"DOT reflected damage indicator on hero");
  Enemy caster=Enemy.sentinel("Caster",false,GameSettings.Difficulty.NORMAL,0,1,0); Enemy ally=Enemy.sentinel("Ally",true,GameSettings.Difficulty.NORMAL,0,1,0);
  Method cast=Enemy.class.getDeclaredMethod("castStoneSkin",Enemy.class);cast.setAccessible(true);Method cooldown=Enemy.class.getDeclaredMethod("sentinelCooldowns");cooldown.setAccessible(true);
  check((Boolean)cast.invoke(caster,ally),"first skin cast");int sp=caster.getSp();check(!(Boolean)cast.invoke(caster,ally)&&caster.getSp()==sp,"no recast while active");
  for(int i=0;i<5;i++)ally.takeDamage(1);
  for(int i=0;i<3;i++){cooldown.invoke(caster);check(!(Boolean)cast.invoke(caster,ally),"three cooldown actions");}
  cooldown.invoke(caster);check((Boolean)cast.invoke(caster,ally),"available on fourth action");
  System.out.println("Stone Skin reflection checks passed");
 }
}
