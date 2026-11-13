import game.model.*;
import java.lang.reflect.*;
import java.util.*;
public class StunDefenseTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Student student(GameProgress p,int id){return p.getParty().stream().filter(s->s.getId()==id).findFirst().orElseThrow();}
 static Random fixed(){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return 0;}};}
 static void stun(Battle b,Student s)throws Exception{Method m=Battle.class.getDeclaredMethod("applyStun",Student.class,int.class);m.setAccessible(true);m.invoke(b,s,1);}
 public static void main(String[] args)throws Exception{
  GameProgress p=new GameProgress(List.of(Student.ZACK,Student.BOGART));Student ward=student(p,Student.ZACK),guard=student(p,Student.BOGART);p.getInventory().addEquipment("heroes_ring");check(p.equip(guard,Equipment.Slot.ACCESSORY,"heroes_ring"),"equip ring");
  Battle b=new Battle(p,fixed());stun(b,guard);check(!b.selectStudent(guard),"stunned hero cannot choose an action");int hp=ward.getHp(),guardHp=guard.getHp();check(b.enemyAttack(b.getEnemies().get(0)),"enemy attacks");check(ward.getHp()<hp && guard.getHp()==guardHp && b.getBlockingStudent()==null,"stunned ring wearer cannot auto-defend");
  check(b.consumeStunTurn(guard)&&b.getStunTurns(guard)==0,"stun lasts an action bar turn");check(b.selectStudent(guard),"hero can act after stun ends");
  GameProgress q=new GameProgress(List.of(Student.ZACK,Student.CHARICE));Student ally=student(q,Student.ZACK),defender=student(q,Student.CHARICE);Battle manual=new Battle(q,new Random(){public int nextInt(int n){return n==2?1:0;}public double nextDouble(){return 0;}});check(manual.selectStudent(defender)&&manual.defend(ally),"manual guard active");stun(manual,defender);check(!defender.isDefending()&&manual.getGuardedAlly(defender)==null,"stun cancels active guard");int before=ally.getHp();check(manual.enemyAttack(manual.getEnemies().get(0))&&ally.getHp()<before,"stunned defender cannot intercept");
  System.out.println("Stunned defender checks passed");
 }
}
