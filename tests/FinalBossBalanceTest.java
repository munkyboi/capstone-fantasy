import game.model.*;import java.util.*;
/** Seeded fixed-step combat model: level 10, first-stage weapons, three kits and no allocated points.
 * It approximates AP turns and player choices; it is not a substitute for a visual playtest. */
public class FinalBossBalanceTest {
 static boolean run(int mask,int level,GameSettings.Difficulty d,int seed){
  List<Integer> ids=new ArrayList<>();for(int i=0;i<4;i++)if((mask&(1<<i))!=0)ids.add(i);
  GameProgress p=new GameProgress(ids);for(int i=0;i<4;i++)p.advance();
  for(Student s:p.getParty()){while(s.getLevel()<level)s.gainExperience(s.getXpRequired());String weapon=Equipment.upgradedWeapon(s.getId());p.getInventory().addEquipment(weapon);p.equip(s,Equipment.Slot.WEAPON,weapon);}
  Random r=new Random(seed);Battle b=new Battle(p,r,false,4,d);Map<Student,Double> ap=new IdentityHashMap<>();Map<Enemy,Double> ep=new IdentityHashMap<>();for(Student s:b.getParty())ap.put(s,(double)s.getActionThreshold());for(Enemy e:b.getEnemies())ep.put(e,0.0);
  for(int step=0;step<6000&&!b.isOver();step++){
   boolean acted=false;
   for(Student s:b.getParty())if(s.isAlive()&&ap.get(s)>=s.getActionThreshold()){
    b.tickDamageOverTime(s);if(!s.isAlive()||b.isOver()){ap.put(s,0.0);continue;}
    if(b.consumeStunTurn(s)){ap.put(s,0.0);acted=true;break;}
    if(!b.selectStudent(s))continue;
    Student weak=b.getParty().stream().filter(Student::isAlive).min(Comparator.comparingDouble(x->(double)x.getHp()/x.getMaxHp())).orElse(s);
    Enemy target=b.getEnemies().stream().filter(Enemy::isAlive).min(Comparator.comparingDouble(x->(double)x.getHp()/x.getMaxHp())).orElse(null);
    if(target==null)break;
    if(s.getId()==Student.ANGEL && weak.getHp()<weak.getMaxHp()*.6 && s.getSp()>=s.getAbilitySpCost())b.ability(weak,null);
    else if(weak.getHp()<weak.getMaxHp()*.25 && p.getKits()>0)b.useKit(weak);
    else if(s.getId()!=Student.ANGEL && s.getSp()>=s.getAbilitySpCost())b.ability(null,target);
    else b.attack(target);
    int safety=0;while(b.isActionAnimating()&&safety++<20){if(b.hasPendingAngelAttack()&&!b.hasPendingLooperHits())b.advanceAngelAttack();else if(b.hasPendingLooperHits())b.advanceLooperHit();else if(b.hasPendingSecondCast())b.completeSecondCast();else if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();}
    ap.put(s,0.0);acted=true;break;
   }
   if(b.isOver())break;if(acted)continue;
   for(Enemy e:b.getEnemies())if(e.isAlive()&&ep.get(e)>=e.getActionThreshold()){
    b.tickDamageOverTime(e);if(e.isAlive()&&!b.isOver())b.enemyAttack(e);if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();ep.put(e,0.0);acted=true;break;
   }
   if(b.isOver())break;if(acted)continue;
   for(Student s:b.getParty())if(s.isAlive()){double old=ap.get(s);double next=Math.min(s.getActionThreshold(),old+1*(1+.05*(s.getEffectiveSpeed()-s.getStartingSpeed())));ap.put(s,next);if(old<s.getActionThreshold()&&next>=s.getActionThreshold())b.regenerate(List.of(s),false);}
   for(Enemy e:b.getEnemies())if(e.isAlive())ep.put(e,Math.min(e.getActionThreshold(),ep.get(e)+1));
  }
  return b.isVictory();
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[]args){
  for(var d:GameSettings.Difficulty.values())for(int size=1;size<=4;size++){
   List<Integer> ids=new ArrayList<>();for(int i=0;i<size;i++)ids.add(i);
   GameProgress p=new GameProgress(ids);for(int i=0;i<4;i++)p.advance();Battle battle=new Battle(p,new Random(11),false,4,d);
   check(battle.getEnemies().size()==6 && battle.getEnemies().stream().filter(Enemy::isBoss).count()==1 && battle.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()).count()==5,"final composition " + d + " " + size);
   Enemy boss=battle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
   double hpFactor=new double[]{0,.35,.595,.7225,.80}[size],thresholdFactor=new double[]{0,1.8,1.62,1.44,1.35}[size];
   check(boss.getMaxHp()==Math.round(325*d.multiplier()*hpFactor) && boss.getActionThreshold()==Math.round(40*thresholdFactor/d.multiplier()),"party and difficulty stats " + d + " " + size);
   double damageFactor=new double[]{0,.20,.595,.7225,.75}[size];
   check(boss.scaleDamage(100)==Math.round(100*d.multiplier()*damageFactor),"damage honors difficulty and party size " + d + " " + size);
   check(battle.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum()==5*Math.round(39*d.multiplier())+Math.round(110*d.multiplier()),"rewards still use difficulty " + d);
   int wins=0,total=0;for(int mask=1;mask<16;mask++)if(Integer.bitCount(mask)==size)for(int seed=0;seed<8;seed++){if(run(mask,10,d,seed))wins++;total++;}
   System.out.printf("Level 10 %-6s party %d: %d/%d wins%n",d,size,wins,total);
   if(d==GameSettings.Difficulty.NORMAL)check(wins*4>=total*3,"normal final remains beatable at level 10 " + size);
  }
  GameProgress p=new GameProgress(List.of(0,1,2,3));for(int i=0;i<4;i++)p.advance();Random fixed=new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};
  Battle battle=new Battle(p,fixed,false,4);Enemy sentinel=battle.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()).findFirst().orElseThrow();sentinel.takeDamage(80);
  check(battle.enemyAttack(sentinel) && battle.getLastAbility()!=null && battle.getLastAbility().skill().equals("Stone Mend"),"first Stone Mend");
  for(int i=0;i<4;i++)check(battle.enemyAttack(sentinel) && (battle.getLastAbility()==null || !battle.getLastAbility().skill().equals("Stone Mend")),"four-action Stone Mend cooldown");
  check(battle.enemyAttack(sentinel) && battle.getLastAbility()!=null && battle.getLastAbility().skill().equals("Stone Mend"),"Stone Mend available after cooldown");
  System.out.println("Final boss balance checks passed");
 }
}
