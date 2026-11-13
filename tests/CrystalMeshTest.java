import game.model.*;
import java.util.*;
import java.lang.reflect.*;

public final class CrystalMeshTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random fixed(double roll){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return roll;}};}
 static GameProgress party(){GameProgress p=new GameProgress(List.of(0,1,2,3));for(int i=0;i<4;i++)p.advance();return p;}
 static void heal(GameProgress p){p.getParty().forEach(h->h.heal(999999));}
 static void killCompanions(Battle b){for(Enemy e:b.getEnemies())if(!e.isBoss())e.takeDamage(999999);}
 public static void main(String[]a)throws Exception {
  for(double roll:new double[]{0,.149999,.15,.399999,.40,.99}){
   GameProgress p=party();Battle b=new Battle(p,fixed(roll),false,4);Enemy boss=b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
   check(boss.getMaxHp()==1000 && boss.getMaxSp()==2000 && boss.getSp()==2000,"normal full-party HP and SP");
   check(boss.getExperienceDrop()==10000 && boss.getPetotDrop()==50000,"guardian base rewards");
   killCompanions(b);check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Mesh")&&boss.getSp()==1900,"mesh when alone costs 100 SP");
   long count=b.getEnemies().stream().filter(e->!e.isBoss()&&e.isAlive()).count();int expected=roll<.15?3:roll<.40?2:1;
   check(count==expected&&b.getEnemies().size()==6,"cumulative summon chance "+roll);
   check(b.getEnemies().stream().anyMatch(e->e.isAlive() && e.isSentinel() && !e.isBoss() && !e.isAttackSentinel()),"summon always includes support");
   check(b.getRewardEnemies().stream().filter(Enemy::isDefeated).count()==5,"first wave preserved for rewards");
  }
  GameProgress p=party();Battle b=new Battle(p,fixed(.99),false,4);Enemy boss=b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();killCompanions(b);check(b.enemyAttack(boss),"initial summon");killCompanions(b);
  for(int i=0;i<5;i++){heal(p);check(b.enemyAttack(boss)&&(b.getLastAbility()==null||!b.getLastAbility().skill().equals("Crystal Mesh")),"mesh five-turn cooldown");while(b.hasPendingGuardianLoop())b.resolveGuardianLoop();}
  heal(p);check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Pulse"),"charged pulse has priority");while(b.hasPendingGuardianLoop())b.resolveGuardianLoop();
  heal(p);int sp=boss.getSp();check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Mesh")&&boss.getSp()==sp-100,"mesh returns after cooldown");
  check(b.getRewardEnemies().stream().filter(Enemy::isDefeated).count()==6,"multiple defeated waves preserved");
  GameProgress partial=party();Battle partialBattle=new Battle(partial,fixed(.99),false,4);Enemy partialBoss=partialBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  List<Enemy> defeated=partialBattle.getEnemies().stream().filter(e->!e.isBoss()).toList();defeated.get(0).takeDamage(999999);defeated.get(2).takeDamage(999999);
  check(partialBattle.enemyAttack(partialBoss)&&!partialBattle.getLastAbility().skill().equals("Crystal Mesh"),"two defeated cannot trigger mesh");
  defeated.get(3).takeDamage(999999);heal(partial);check(partialBattle.enemyAttack(partialBoss)&&partialBattle.getLastAbility().skill().equals("Crystal Mesh"),"three defeated trigger mesh");
  check(defeated.get(1).isAlive()&&partialBattle.getEnemies().stream().filter(e->e.isAlive()&&e.isSentinel()&&!e.isBoss()&&!e.isAttackSentinel()).count()==2,"existing support remains and missing support returns");
  GameProgress blocked=party();Battle wait=new Battle(blocked,fixed(.99),false,4);Enemy waitBoss=wait.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  wait.getEnemies().stream().filter(Enemy::isAttackSentinel).forEach(e->e.takeDamage(999999));check(wait.enemyAttack(waitBoss)&&!wait.getLastAbility().skill().equals("Crystal Mesh"),"wait when both supports live");
  wait.getEnemies().stream().filter(e->e.isSentinel()&&!e.isAttackSentinel()&&!e.isBoss()).findFirst().orElseThrow().takeDamage(999999);heal(blocked);check(wait.enemyAttack(waitBoss)&&wait.getLastAbility().skill().equals("Crystal Mesh"),"summon once a support is missing");
  Method healing=Enemy.class.getDeclaredMethod("heal",int.class);healing.setAccessible(true);
  for(var d:GameSettings.Difficulty.values())for(int lvl:new int[]{1,50}){
   Enemy guardian=Enemy.finalOpponent("Guardian",true,d,lvl,4);guardian.takeDamage(guardian.getHp()-1);
   double x=(lvl-1)/99.0,curve=x*x*(3-2*x);int cap=(int)Math.ceil(200*(1+2.97*curve)*d.multiplier());
   int actual=(Integer)healing.invoke(guardian,999999);check(actual==Math.min(cap,guardian.getMaxHp()-1),"level and difficulty scaled healing cap");
  }
  System.out.println("Crystal Mesh checks passed");
 }
}
