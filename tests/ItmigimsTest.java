import game.model.*;
import java.util.*;

/** Spawn boundaries, stats, countdown, escape and rare shop rewards. */
public final class ItmigimsTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static class Spawn extends Random {
        final int roll; int spawnRolls;
        Spawn(int roll){this.roll=roll;}
        public int nextInt(int bound){if(bound==100){spawnRolls++;return roll;}return 0;}
        public double nextDouble(){return 0;}
    }
    static class LootRolls extends Random {
        final double[] values; int count;
        LootRolls(double... values){this.values=values;}
        public double nextDouble(){return values[count++];}
        public int nextInt(int bound){return 0;}
    }
    static GameProgress stage(int index){GameProgress p=new GameProgress(List.of(0,1,2,3));for(int i=0;i<index;i++)p.advance();return p;}
    public static void main(String[] args) throws Exception {
        check(ItmigimsTest.class.getResource("/assets/sprites/itmigims-v1.png") != null, "sprite packaged in game");
        Enemy base=Enemy.itmigims(GameSettings.Difficulty.NORMAL,0,1);
        check(base.getMaxHp()==450 && base.getSp()==100 && base.getMaxSp()==100 && base.getAttack()==1
                && base.getDefense()==18 && base.getAgility()==18 && base.getAccuracy()==18 && base.getSpeed()==15 && base.getActionThreshold()==30,"level-one stats");
        check(base.getEscapeCountdown()==-1,"countdown hidden until hit");
        base.takeDamage(0);check(base.getEscapeCountdown()==-1,"zero damage does not start countdown");
        base.takeDamage(1);check(base.getEscapeCountdown()==13,"first damage starts countdown");
        java.lang.reflect.Method heal=Enemy.class.getDeclaredMethod("heal",int.class);heal.setAccessible(true);int injuredHp=base.getHp();
        check((int)heal.invoke(base,999)==0 && base.getHp()==injuredHp,"Itmigims is immune to healing");
        for(int stage=0;stage<5;stage++) {
            for(int roll:new int[]{94,95,99}) {
                Spawn random=new Spawn(roll);Battle b=new Battle(stage(stage),random,false,stage);
                long count=b.getEnemies().stream().filter(Enemy::isLootGoblin).count();
                check(count==(stage==4||roll<95?0:1),"spawn boundary stage " + stage + " roll " + roll);
                check(b.getEnemies().size()==new int[]{1,2,3,4,6}[stage],"goblin replaces wisp within spawn count");
                long sentinels=b.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()).count();
                check(sentinels==new int[]{0,1,1,1,5}[stage],"required sentinels preserved");
                check(random.spawnRolls==(stage==4?0:1),"no goblin roll on final stage");
            }
        }
        GameProgress hitParty=stage(0); Battle hits=new Battle(hitParty,new Spawn(95)); Enemy hitGoblin=hits.getEnemies().get(0);
        Student attacker=hits.getParty().get(0); check(hits.selectStudent(attacker) && hits.attack(hitGoblin),"player hits goblin");
        check(hitGoblin.getEscapeCountdown()==13,"player hit starts countdown");
        check(hits.selectStudent(attacker) && hits.attack(hitGoblin) && hitGoblin.getEscapeCountdown()==13,"other characters' actions do not advance countdown");
        GameProgress p=stage(0); Battle battle=new Battle(p,new Spawn(95)); Enemy goblin=battle.getEnemies().get(0);
        check(goblin.isLootGoblin(),"goblin spawned");
        check(battle.getMessage().contains("Itmigims") && !battle.getMessage().contains("Wisp"),"solo goblin announcement is accurate");
        int[] partyHp=p.getParty().stream().mapToInt(Student::getHp).toArray();
        check(battle.enemyAttack(goblin) && goblin.getEscapeCountdown()==-1 && battle.getLastAbility()==null,"unhit goblin does not count down or attack");
        goblin.takeDamage(1);
        for(int i=12;i>=1;i--){check(battle.enemyAttack(goblin),"goblin action");check(goblin.getEscapeCountdown()==i && goblin.isAlive(),"countdown " + i);}
        check(java.util.Arrays.equals(partyHp,p.getParty().stream().mapToInt(Student::getHp).toArray()),"goblin never attacks during countdown");
        check(battle.enemyAttack(goblin) && goblin.hasEscaped() && !goblin.isAlive() && !goblin.isDefeated(),"escapes on thirteenth action");
        check(goblin.getSp()==90 && battle.getLastAbility().skill().equals("Escape") && battle.isVictory(),"escape spends 10 SP and ends empty encounter");
        GameProgress group=stage(1);Battle support=new Battle(group,new Spawn(95),false,1);Enemy healer=support.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Enemy immune=support.getEnemies().stream().filter(Enemy::isLootGoblin).findFirst().orElseThrow();immune.takeDamage(200);int immuneHp=immune.getHp(),healerSp=healer.getSp();
        check(support.enemyAttack(healer)&&immune.getHp()==immuneHp&&healer.getSp()==healerSp,"sentinel does not waste a heal on Itmigims");
        check(MonsterLoot.award(List.of(goblin),p,new LootRolls(0,0)).isEmpty(),"escaped goblin awards no loot");
        Enemy defeated=Enemy.itmigims(GameSettings.Difficulty.NORMAL,0,1);defeated.takeDamage(99999);
        LootRolls none=new LootRolls(.20);check(MonsterLoot.award(List.of(defeated),stage(0),none).isEmpty() && none.count==1,"20 percent boundary");
        GameProgress one=stage(0);check(MonsterLoot.award(List.of(defeated),one,new LootRolls(.1999,.05)).equals(List.of("first_aid_kit")),"20 percent and 5 percent boundaries");
        GameProgress two=stage(0);check(MonsterLoot.award(List.of(defeated),two,new LootRolls(0,.0499)).equals(List.of("first_aid_kit","market_charm")),"second item rolled separately");
        check(two.getKits()==4 && two.getInventory().getEquipmentIds().contains("market_charm"),"shop items credited");
        GameProgress all=stage(0);for(CarbonMarket.Offer o:CarbonMarket.offers())if(!o.id().equals("first_aid_kit"))all.getInventory().addEquipment(o.id());
        for(int i=0;i<10;i++)all.getInventory().addKit();
        check(MonsterLoot.award(List.of(defeated),all,new LootRolls(0,0)).isEmpty(),"no duplicate unique items or overfull kits");
        System.out.println("Itmigims encounter checks passed");
    }
}
