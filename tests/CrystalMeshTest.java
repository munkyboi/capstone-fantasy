import game.model.*;
import java.util.*;

public final class CrystalMeshTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Random fixed(){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return .99;}};}
 static GameProgress party(){GameProgress p=new GameProgress(List.of(0,1,2,3));for(int i=0;i<4;i++)p.advance();return p;}
 static void heal(GameProgress p){p.getParty().forEach(h->h.heal(999999));}
 static void killCompanions(Battle b){for(Enemy e:b.getEnemies())if(!e.isBoss())e.takeDamage(999999);}
 public static void main(String[]a){
  GameProgress p=party();Battle b=new Battle(p,fixed(),false,4);Enemy boss=b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  check(boss.getMaxHp()==2000 && boss.getMaxSp()==2000 && boss.getSp()==2000,"normal full-party HP and SP");
  check(boss.getExperienceDrop()==10000 && boss.getPetotDrop()==50000,"guardian base rewards");
  killCompanions(b);check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Mesh")&&boss.getSp()==1900,"mesh when alone costs 100 SP");
  check(b.getEnemies().size()==6&&b.getEnemies().stream().filter(Enemy::isAttackSentinel).count()==3&&b.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()&&!e.isAttackSentinel()).count()==2,"new wave composition and display size");
  check(b.getRewardEnemies().stream().filter(Enemy::isDefeated).count()==5,"first wave preserved for rewards");
  killCompanions(b);for(int i=0;i<5;i++){heal(p);check(b.enemyAttack(boss)&& (b.getLastAbility()==null||!b.getLastAbility().skill().equals("Crystal Mesh")),"mesh five-turn cooldown");while(b.hasPendingGuardianLoop())b.resolveGuardianLoop();}
  heal(p);check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Pulse"),"charged pulse resolves first");while(b.hasPendingGuardianLoop())b.resolveGuardianLoop();
  heal(p);int sp=boss.getSp();check(b.enemyAttack(boss)&&b.getLastAbility().skill().equals("Crystal Mesh")&&boss.getSp()==sp-100,"mesh can summon again after cooldown");
  check(b.getRewardEnemies().stream().filter(Enemy::isDefeated).count()==10,"multiple defeated waves preserved");
  GameProgress q=party();Battle healBattle=new Battle(q,fixed(),false,4,GameSettings.Difficulty.HARD);Enemy guardian=healBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();Enemy support=healBattle.getEnemies().stream().filter(e->e.isSentinel()&&!e.isAttackSentinel()&&!e.isBoss()).findFirst().orElseThrow();guardian.takeDamage(guardian.getMaxHp()-100);int before=guardian.getHp();check(healBattle.enemyAttack(support)&&guardian.getHp()==before+500,"Stone Mend guardian heal capped at 500");
  System.out.println("Crystal Mesh checks passed");
 }
}
