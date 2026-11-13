import game.model.*;
import java.util.*;

public final class AccuracyDifficultyScalingTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static boolean hit(GameSettings.Difficulty difficulty){
  GameProgress p=new GameProgress(List.of(Student.ZACK,Student.BOGART,Student.CHARICE));
  Random random=new Random(){public int nextInt(int n){return 0;} public double nextDouble(){return .65;}};
  Battle battle=new Battle(p,random,false,0,difficulty);Student charice=p.getParty().stream().filter(s->s.getId()==Student.CHARICE).findFirst().orElseThrow();Enemy wisp=battle.getEnemies().get(0);int hp=wisp.getHp();
  check(battle.selectStudent(charice)&&battle.attack(wisp),"hero attacks");return wisp.getHp()<hp;
 }
 public static void main(String[] args){
  for(int size=1;size<=4;size++)for(GameSettings.Difficulty d:GameSettings.Difficulty.values()){
   double party=new double[]{3,2,1.5,1}[size-1];GameProgress p=new GameProgress(java.util.stream.IntStream.range(0,size).boxed().toList());Battle b=new Battle(p,new Random(2),false,0,d);
   check(Math.abs(b.getHeroAccuracyScalingFactor()-100*party/d.multiplier())<1e-9,"hero factor "+size+" "+d);
   check(Math.abs(b.getAccuracyScalingFactor()-100*party*d.multiplier())<1e-9,"monster factor "+size+" "+d);
  }
  Enemy level1=new Enemy("Wisp",false,false,GameSettings.Difficulty.NORMAL,0,1), level20=new Enemy("Wisp",false,false,GameSettings.Difficulty.NORMAL,0,20);
  check(level1.getAgility()==9 && level20.getAgility()==19,"enemy agility grows half per level, rounded");
  Enemy merchant=Enemy.merchant(GameSettings.Difficulty.NORMAL,20);check(merchant.getAgility()==20,"merchant uses same growth");
  double initial=Battle.hitChance(level1.getAgility(),12,150),advanced=Battle.hitChance(level20.getAgility(),31,150);
  check(advanced>initial && Battle.hitChance(19,32,150)>advanced,"leveling and allocated ACC improve accuracy");
  check(hit(GameSettings.Difficulty.EASY)&&hit(GameSettings.Difficulty.NORMAL)&&!hit(GameSettings.Difficulty.HARD)&&!hit(GameSettings.Difficulty.INSANE),"actual hit uses hero difficulty factor");
  System.out.println("Accuracy difficulty scaling checks passed");
 }
}
