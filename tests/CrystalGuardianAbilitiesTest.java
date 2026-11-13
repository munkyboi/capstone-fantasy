import game.model.*;
import java.util.*;
import java.lang.reflect.*;

public final class CrystalGuardianAbilitiesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static final class Fixed extends Random {double roll=0; public int nextInt(int n){return 0;} public double nextDouble(){return roll;}}
 static void restore(GameProgress p){p.getParty().forEach(h->h.heal(99999));}
 public static void main(String[]a)throws Exception {
  Enemy raw=Enemy.finalOpponent("Guardian",true,GameSettings.Difficulty.NORMAL,1,4);
  check(raw.getMaxHp()==2000 && raw.getMaxSp()==2000 && raw.getSp()==2000 && raw.getAttack()==35 && raw.getDefense()==24 && raw.getSpeed()==1 && raw.getAgility()==14 && raw.getAccuracy()==20 && raw.getActionThreshold()==54,"scaled base stats");
  GameProgress p=new GameProgress(List.of(Student.BOGART,Student.CHARICE));for(int i=0;i<4;i++)p.advance();Fixed rng=new Fixed();Battle b=new Battle(p,rng,false,4);Enemy boss=b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  check(boss.getBossCountdown()==0 && b.enemyAttack(boss) && boss.getBossCountdown()==5 && boss.getSp()==1950,"charge costs 50 and starts at five");
  for(int i=4;i>=1;i--){restore(p);check(b.enemyAttack(boss) && boss.getBossCountdown()==i,"countdown "+i);if(i==4)check(b.getLastAbility()!=null && b.getLastAbility().skill().equals("Crushing Grip") && boss.getSp()==1935,"grip cast and SP cost during charge");}
  restore(p);int[] before=p.getParty().stream().mapToInt(Student::getHp).toArray();check(b.enemyAttack(boss) && boss.getBossCountdown()==0 && b.hasPendingGuardianLoop() && b.getLastAbility().skill().equals("Crystal Pulse"),"pulse after five charging turns");
  List<Student> heroes=p.getParty();int[] base=new int[heroes.size()],prev=new int[heroes.size()];for(int i=0;i<heroes.size();i++){base[i]=before[i]-heroes.get(i).getHp();check(base[i]>0 && b.getStunTurns(heroes.get(i))==1,"pulse damages and stuns each initial target");}
  for(int loop=0;loop<5;loop++){restore(p);int[] hp=heroes.stream().mapToInt(Student::getHp).toArray();check(b.resolveGuardianLoop(),"loop " + loop);for(int i=0;i<hp.length;i++){int expected=(base[i]*5+prev[i]*2+9)/10;prev[i]=hp[i]-heroes.get(i).getHp();check(prev[i]==expected,"loop formula per hero");}}
  check(!b.hasPendingGuardianLoop(),"loop stops at five");
  for(int i=0;i<5;i++){restore(p);check(b.enemyAttack(boss)&&boss.getBossCountdown()==0,"five-turn cooldown");}
  restore(p);check(b.enemyAttack(boss)&&boss.getBossCountdown()==5,"charge restarts after cooldown");
  GameProgress q=new GameProgress(List.of(Student.BOGART));for(int i=0;i<4;i++)q.advance();Battle dot=new Battle(q,new Fixed(),false,4);Enemy g=dot.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();Student h=q.getParty().get(0);
  check(dot.enemyAttack(g)&&dot.enemyAttack(g)&&dot.hasDamageOverTime(h),"grip applies three-turn DOT");
  int total=0;for(int i=0;i<3;i++){h.heal(9999);int hp=h.getHp();check(dot.tickDamageOverTime(h),"DOT tick "+i);total+=hp-h.getHp();}
  check(!dot.hasDamageOverTime(h)&&total==g.scaleDamage(g.getAttack()),"DOT lasts three ticks and totals 100% scaled ATK");
  Fixed boundaryRng=new Fixed(); GameProgress boundaryParty=new GameProgress(List.of(Student.BOGART));for(int i=0;i<4;i++)boundaryParty.advance();Battle boundary=new Battle(boundaryParty,boundaryRng,false,4);Enemy gb=boundary.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  for(int i=0;i<5;i++){restore(boundaryParty);check(boundary.enemyAttack(gb),"boundary setup");}boundaryRng.roll=.45;restore(boundaryParty);check(boundary.enemyAttack(gb)&&!boundary.hasPendingGuardianLoop(),"45 percent boundary stops loops");
  Fixed stunRng=new Fixed();GameProgress stunParty=new GameProgress(List.of(Student.BOGART));for(int i=0;i<4;i++)stunParty.advance();Battle stunBattle=new Battle(stunParty,stunRng,false,4);Enemy stunBoss=stunBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
  for(int i=0;i<5;i++){restore(stunParty);check(stunBattle.enemyAttack(stunBoss),"stun setup");}stunRng.roll=.40;restore(stunParty);check(stunBattle.enemyAttack(stunBoss)&&stunBattle.getStunTurns(stunParty.getParty().get(0))==0,"40 percent boundary does not stun");check(stunBattle.hasPendingGuardianLoop(),"40 percent roll starts loop below 45 percent");stunRng.roll=.45;check(stunBattle.resolveGuardianLoop()&&!stunBattle.hasPendingGuardianLoop(),"45 percent boundary stops a subsequent loop");
  javax.swing.SwingUtilities.invokeAndWait(()->{try {
   GameProgress up=new GameProgress(List.of(Student.BOGART));for(int i=0;i<4;i++)up.advance();game.view.BattlePanel panel=new game.view.BattlePanel(()->{},ignored->{},new GameSettings(),new Fixed());panel.start(up,false,4);
   Field bf=game.view.BattlePanel.class.getDeclaredField("battle");bf.setAccessible(true);Battle ui=(Battle)bf.get(panel);Field turn=Battle.class.getDeclaredField("turn");turn.setAccessible(true);turn.setInt(ui,-1);
   Enemy ug=ui.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();for(int i=0;i<6;i++){restore(up);check(ui.enemyAttack(ug),"UI pulse setup");}check(ui.hasPendingGuardianLoop(),"UI has delayed loop");
   Method resolve=game.view.BattlePanel.class.getDeclaredMethod("resolveSparkExplosion");resolve.setAccessible(true);resolve.invoke(panel);Field tf=game.view.BattlePanel.class.getDeclaredField("sparkExplosionTimer");tf.setAccessible(true);javax.swing.Timer timer=(javax.swing.Timer)tf.get(panel);check(timer.isRunning() && timer.getInitialDelay()==300,"UI schedules next loop after 300ms");timer.stop();while(ui.hasPendingGuardianLoop()){restore(up);resolve.invoke(panel);timer.stop();}
   for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer")){Field f=game.view.BattlePanel.class.getDeclaredField(name);f.setAccessible(true);((javax.swing.Timer)f.get(panel)).stop();}
  }catch(Exception e){throw new RuntimeException(e);}});
  System.out.println("Crystal Guardian ability checks passed");
 }
}
