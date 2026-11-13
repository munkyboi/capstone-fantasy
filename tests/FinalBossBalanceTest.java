import game.model.*;import java.util.*;
/** Seeded fixed-step combat model: level 20, full equipment, three kits and no allocated points.
 * It approximates AP turns and player choices; it is not a substitute for a visual playtest. */
public class FinalBossBalanceTest {
 static boolean run(int mask,int level,GameSettings.Difficulty d,int seed){
  List<Integer> ids=new ArrayList<>();for(int i=0;i<4;i++)if((mask&(1<<i))!=0)ids.add(i);
  GameProgress p=new GameProgress(ids);for(int i=0;i<4;i++)p.advance();
  for(Student s:p.getParty()){while(s.getLevel()<level)s.gainExperience(s.getXpRequired());String[] weapons={"great_sage_staff","light_saber","demon_hunter","giant_slayer_gloves"},armors={"ethereal_robe","blade_mail","ultra_instinct_shirt","designer_leather_vest"},accessories={"scholars_ward","heroes_ring","looper_ring","ring_of_power"}; String[] gear={weapons[s.getId()],armors[s.getId()],accessories[s.getId()]}; Equipment.Slot[] slots=Equipment.Slot.values(); for(int i=0;i<gear.length;i++){p.getInventory().addEquipment(gear[i]);p.equip(s,slots[i],gear[i]);}}
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
    int safety=0;while(b.isActionAnimating()&&safety++<20){if(b.hasPendingTwinShot())b.advanceTwinShot();else if(b.hasPendingAngelAttack()&&!b.hasPendingLooperHits())b.advanceAngelAttack();else if(b.hasPendingLooperHits())b.advanceLooperHit();else if(b.hasPendingSecondCast())b.completeSecondCast();else if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();else if(b.hasPendingGuardianLoop())b.resolveGuardianLoop();}
    ap.put(s,0.0);acted=true;break;
   }
   if(b.isOver())break;if(acted)continue;
   for(Enemy e:b.getEnemies())if(e.isAlive()&&ep.getOrDefault(e,0.0)>=e.getActionThreshold()){
    b.tickDamageOverTime(e);if(e.isAlive()&&!b.isOver())b.enemyAttack(e);if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();while(b.hasPendingGuardianLoop())b.resolveGuardianLoop();ep.put(e,0.0);acted=true;break;
   }
   if(b.isOver())break;if(acted)continue;
   for(Student s:b.getParty())if(s.isAlive()){double old=ap.get(s);double next=Math.min(s.getActionThreshold(),old+1*(1+.05*(s.getEffectiveSpeed()-s.getStartingSpeed())));ap.put(s,next);if(old<s.getActionThreshold()&&next>=s.getActionThreshold())b.regenerate(List.of(s),false);}
   for(Enemy e:b.getEnemies())if(e.isAlive())ep.put(e,Math.min(e.getActionThreshold(),ep.getOrDefault(e,0.0)+1));
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
   check(boss.getMaxHp()==Math.round(1250*d.multiplier()*hpFactor) && boss.getActionThreshold()==Math.round(40*thresholdFactor/d.multiplier()),"party and difficulty stats " + d + " " + size);
   double damageFactor=new double[]{0,.20,.595,.7225,.75}[size];
   check(boss.scaleDamage(100)==Math.round(100*d.multiplier()*damageFactor),"damage honors difficulty and party size " + d + " " + size);
   check(battle.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum()==5*Math.round(39*d.multiplier())+Math.round(50000*d.multiplier()),"rewards still use difficulty " + d);
   int wins=0,total=0;for(int mask=1;mask<16;mask++)if(Integer.bitCount(mask)==size)for(int seed=0;seed<8;seed++){if(run(mask,20,d,seed))wins++;total++;}
   System.out.printf("Level 20 %-6s party %d: %d/%d wins%n",d,size,wins,total);

  }
  System.out.println("Final boss balance checks passed");
 }
}
