import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Difficulty biases encounter sizes and scales currency and independent loot rolls. */
public final class DifficultyRewardsTest {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static Random rolls(double... values){return new Random(){int i;public double nextDouble(){return values[Math.min(i++,values.length-1)];}public int nextInt(int n){return 0;}};}
    public static void main(String[] args)throws Exception{
        int[] minimum={1,2,3,4,6},maximum={2,3,4,6,6};double[] averages=new double[4];
        for(GameSettings.Difficulty d:GameSettings.Difficulty.values()){
            Random r=new Random(724);for(int stage=0;stage<5;stage++)for(int i=0;i<5000;i++){
                int n=EncounterRules.rollMonsterCount(stage,r,d);check(n>=minimum[stage]&&n<=maximum[stage],"stage range " + d);
                if(stage==3)averages[d.ordinal()]+=n/5000.0;
            }
            Enemy w=new Enemy("Wisp",false,false,d);check(w.getPetotDrop()==Math.round(12*d.multiplier()),"Petot per difficulty " + d);
            Enemy g=Enemy.itmigims(d,0,1);check(g.getPetotDrop()==Math.round(5000*d.multiplier())&&g.getExperienceDrop()==0,"goblin Petot " + d);
            Enemy defeated=new Enemy("Wisp");defeated.takeDamage(9999);double threshold=.30*d.multiplier();
            GameProgress party=new GameProgress(List.of(Student.ZACK));
            check(MonsterLoot.award(List.of(defeated),party,rolls(threshold-0.0001,1),d).contains("first_aid_kit"),"drop below threshold " + d);
            party=new GameProgress(List.of(Student.ZACK));
            check(!MonsterLoot.award(List.of(defeated),party,rolls(threshold,1),d).contains("first_aid_kit"),"drop threshold " + d);
            Enemy goblin=Enemy.itmigims(d,0,1);goblin.takeDamage(999999);
            double first=Math.min(1,.5*d.multiplier()), second=.25*d.multiplier();
            party=new GameProgress(List.of(Student.ZACK));List<String> two=MonsterLoot.award(List.of(goblin),party,rolls(first-.0001,second-.0001),d);
            check(two.size()==2,"two goblin items below conditional threshold " + d);
            party=new GameProgress(List.of(Student.ZACK));List<String> one=MonsterLoot.award(List.of(goblin),party,rolls(first-.0001,second),d);
            check(one.size()==1,"conditional second drop boundary " + d);
        }
        check(averages[0]<averages[1]&&averages[1]<averages[2]&&averages[2]<averages[3],"difficulty biases encounters in ascending order");
        for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) for (int stage=0;stage<5;stage++) for(int seed=0;seed<40;seed++) {
            GameProgress p=new GameProgress(List.of(Student.ZACK));for(int i=0;i<stage;i++)p.advance();
            Battle battle=new Battle(p,new Random(seed),false,stage,difficulty);long sentinels=battle.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()).count();
            check(battle.getEnemies().size()>=minimum[stage] && battle.getEnemies().size()<=maximum[stage],"actual encounter respects stage limit");
            if(stage==3)check(sentinels>=1&&sentinels<=3,"stage four Sentinel range");
            if(stage==4)check(sentinels==5 && battle.getEnemies().stream().filter(Enemy::isBoss).count()==1,"final boss encounter remains fixed");
        }
        SwingUtilities.invokeAndWait(()->{try{
            for(boolean greed:new boolean[]{false,true}){
                GameSettings s=new GameSettings();s.setDifficulty(GameSettings.Difficulty.HARD);s.setGreedIsGood(greed);
                GameProgress p=new GameProgress(List.of(Student.ZACK));BattlePanel panel=new BattlePanel(()->{},x->{},s,new Random(6));panel.start(p,false,0);
                Field f=BattlePanel.class.getDeclaredField("battle");f.setAccessible(true);Battle b=(Battle)f.get(panel);
                int base=b.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum();b.getEnemies().forEach(e->e.takeDamage(99999));
                Method refresh=BattlePanel.class.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(panel);
                check(p.getPetot()==base*(greed?10:1),"greed stacks with difficulty");refresh.invoke(panel);check(p.getPetot()==base*(greed?10:1),"reward only awarded once");
                for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","sparkExplosionTimer","abilityAlertTimer")){Field timer=BattlePanel.class.getDeclaredField(name);timer.setAccessible(true);((javax.swing.Timer)timer.get(panel)).stop();}
            }
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Difficulty encounter and reward checks passed");
    }
}
