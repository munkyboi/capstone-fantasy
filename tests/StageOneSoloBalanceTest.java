import game.model.*;import java.util.*;
/** Seeded combat-model checks; these do not replace a manual playtest. */
public class StageOneSoloBalanceTest {
 static boolean run(int id,GameSettings.Difficulty difficulty,boolean ambush,int seed){
  GameProgress p=new GameProgress(List.of(id));Random r=new Random(seed);Battle b=new Battle(p,r,ambush,0,difficulty);Student s=b.getParty().get(0);Map<Enemy,Double> ep=new IdentityHashMap<>();for(Enemy e:b.getEnemies())ep.put(e,ambush?e.getActionThreshold()*.8:0.0);double ap=ambush?0:s.getActionThreshold();
  for(int i=0;i<2000&&!b.isOver();i++){
   if(ap+1e-8>=s.getActionThreshold()){
    if(!b.selectStudent(s))break; Enemy target=b.getEnemies().stream().filter(Enemy::isAlive).min(Comparator.comparingInt(Enemy::getHp)).orElseThrow();
    if(s.getHp()<s.getMaxHp()*.55&&p.getKits()>0)b.useKit(s);
    else if(s.getId()==Student.ANGEL&&s.getHp()<s.getMaxHp()*.65&&s.getSp()>=s.getAbilitySpCost())b.ability(s,null);
    else if(s.getId()!=Student.ANGEL&&s.getSp()>=s.getAbilitySpCost())b.ability(null,target);else b.attack(target);
    int n=0;while(b.isActionAnimating()&&n++<30){if(b.hasPendingTwinShot())b.advanceTwinShot();else if(b.hasPendingAngelAttack()&&!b.hasPendingLooperHits())b.advanceAngelAttack();else if(b.hasPendingLooperHits())b.advanceLooperHit();else if(b.hasPendingSecondCast())b.completeSecondCast();else if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();}ap=0;continue;
   }
   Enemy ready=b.getEnemies().stream().filter(Enemy::isAlive).filter(e->ep.getOrDefault(e,0.0)+1e-8>=e.getActionThreshold()).findFirst().orElse(null);
   if(ready!=null){b.enemyAttack(ready);if(b.hasPendingSparkExplosion())b.resolveSparkExplosion();ep.put(ready,0.0);continue;}
   double dt=s.getActionThreshold()-ap;for(Enemy e:b.getEnemies())if(e.isAlive())dt=Math.min(dt,e.getActionThreshold()-ep.get(e));
   ap=Math.min(s.getActionThreshold(),ap+dt);for(Enemy e:b.getEnemies())if(e.isAlive())ep.put(e,Math.min(e.getActionThreshold(),ep.get(e)+dt));
   if(ap+1e-8>=s.getActionThreshold())b.regenerate(List.of(s),false);
  }return b.isVictory();
 }
 public static void main(String[] args) {
  for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) {
   GameProgress solo = new GameProgress(List.of(Student.CHARICE));
   Battle battle = new Battle(solo, new Random(0), false, 0, difficulty);
   Enemy wisp = battle.getEnemies().stream().filter(e -> !e.isLootGoblin()).findFirst().orElseThrow();
   Enemy standard = new Enemy("Library Wisp A", false, false, difficulty, 0, 1);
   if (wisp.getMaxHp() != Math.round(65 * difficulty.multiplier() * .5)
       || wisp.getActionThreshold() != Math.round(36 * 1.6 / difficulty.multiplier())
       || wisp.getExperienceDrop() != standard.getExperienceDrop()
       || wisp.getPetotDrop() != standard.getPetotDrop()) throw new AssertionError("solo opening balance " + difficulty);
   GameProgress team = new GameProgress(List.of(Student.CHARICE, Student.ZACK));
   Battle teamBattle = new Battle(team, new Random(0), false, 0, difficulty);
   Enemy teamWisp = teamBattle.getEnemies().stream().filter(e -> !e.isLootGoblin()).findFirst().orElseThrow();
   if (teamWisp.getMaxHp() != standard.getMaxHp()) throw new AssertionError("team balance unchanged " + difficulty);
  }
  for (boolean ambush : new boolean[]{false,true}) {
   int wins=0, total=1000;
   for(int seed=0;seed<total;seed++) if(run(Student.CHARICE,GameSettings.Difficulty.INSANE,ambush,seed)) wins++;
   System.out.printf("Solo level 1 Charice, Insane, ambush=%s: %d/%d wins%n",ambush,wins,total);
   if(wins < (ambush ? 350 : 750)) throw new AssertionError("opening encounter too punishing");
  }
  System.out.println("Stage one solo balance checks passed");
 }
}
