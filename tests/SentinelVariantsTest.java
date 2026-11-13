import game.model.*;
import java.util.*;
import java.lang.reflect.*;

public final class SentinelVariantsTest {
    static void check(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
    static Random fixed() { return new Random(){public int nextInt(int n){return 0;} public double nextDouble(){return 0;}}; }
    static GameProgress stage(int n) { GameProgress p=new GameProgress(List.of(Student.BOGART,Student.CHARICE)); for(int i=0;i<n;i++)p.advance(); return p; }
    static String skill(Battle b) { return b.getLastAbility()==null?"":b.getLastAbility().skill(); }
    @SuppressWarnings("unchecked") static void replace(Battle b, Enemy e) throws Exception {
        Field f=Battle.class.getDeclaredField("enemies"); f.setAccessible(true);
        List<Enemy> enemies=(List<Enemy>)f.get(b); enemies.clear(); enemies.add(e);
    }
    public static void main(String[] args) throws Exception {
        Enemy support=Enemy.sentinel("Support",false,GameSettings.Difficulty.NORMAL,0,1,0);
        Enemy attack=Enemy.sentinel("Attack",true,GameSettings.Difficulty.NORMAL,0,1,0);
        check(support.getMaxHp()==110 && support.getMaxSp()==900 && support.getAttack()==13 && support.getDefense()==12 && support.getSpeed()==2 && support.getAgility()==5 && support.getAccuracy()==9 && support.getActionThreshold()==32,"support stats");
        check(attack.getMaxHp()==200 && attack.getMaxSp()==900 && attack.getAttack()==20 && attack.getDefense()==9 && attack.getSpeed()==2 && attack.getAgility()==5 && attack.getAccuracy()==9 && attack.getActionThreshold()==32,"attack stats");
        for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) for (boolean offensive : new boolean[]{false,true}) {
            Enemy e=Enemy.sentinel("Sentinel",offensive,difficulty,0,1,0);
            check(e.getSp()==900 && e.getMaxSp()==900,"fixed SP across difficulties");
        }
        for(int seed=0;seed<100;seed++)for(int st=1;st<=4;st++){
            Battle b=new Battle(stage(st),new Random(seed),false,st);
            long defenders=b.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()&&!e.isAttackSentinel()).count();
            long attackers=b.getEnemies().stream().filter(Enemy::isAttackSentinel).count();
            check(b.getEnemies().stream().filter(e->e.isSentinel()&&!e.isBoss()).allMatch(e->e.getMaxSp()==900 && e.getSp()==900),"every sentinel starts with 900 SP");
            check(defenders<=2,"maximum support");
            if(st==4)check(defenders==2 && attackers==3,"final composition");
        }
        GameProgress team=stage(0); Battle b=new Battle(team,fixed(),false,0); replace(b,support);
        check(b.enemyAttack(support)&&skill(b).equals("Stone Skin")&&support.getSp()==880&&support.getDefense()==22&&support.getStoneSkinHits()==5,"skin costs 20 and adds 10 DEF");
        for(int i=0;i<4;i++){support.takeDamage(1);check(support.getDefense()==22,"skin persists for five hits");}
        support.takeDamage(1);check(support.getDefense()==12&&support.getStoneSkinHits()==0,"skin expires after fifth hit");
        support.takeDamage(support.getHp()-55);int before=support.getHp();
        check(b.enemyAttack(support)&&skill(b).equals("Stone Mend")&&support.getHp()==before+11&&support.getSp()==865,"mend triggers at half HP and costs 15");
        support.takeDamage(20);
        for(int i=0;i<4;i++){team.getParty().forEach(s->s.heal(999));check(b.enemyAttack(support)&&!skill(b).equals("Stone Mend"),"mend cooldown");}
        team.getParty().forEach(s->s.heal(999));check(b.enemyAttack(support)&&skill(b).equals("Stone Mend"),"mend available on fifth action");
        GameProgress offensive=stage(0);Battle combat=new Battle(offensive,fixed(),false,0);replace(combat,attack);
        int total=offensive.getParty().stream().mapToInt(Student::getHp).sum();
        check(combat.enemyAttack(attack)&&skill(combat).equals("Stone Sweep")&&attack.getSp()==885,"sweep cost");
        check(offensive.getParty().stream().mapToInt(Student::getHp).sum()==total-24,"sweep hits all and pierces defense");
        check(combat.enemyAttack(attack)&&skill(combat).equals("Stone Smash")&&attack.getSp()==875,"smash cost");
        check(offensive.getParty().stream().anyMatch(s->combat.getStunTurns(s)==3),"smash stuns three turns");
        for(int i=0;i<3;i++){offensive.getParty().forEach(s->s.heal(999));check(combat.enemyAttack(attack)&&!skill(combat).equals("Stone Smash"),"smash cooldown");}
        offensive.getParty().forEach(s->s.heal(999));check(combat.enemyAttack(attack)&&skill(combat).equals("Stone Smash"),"smash returns after cooldown");
        Random noProc=new Random(){public int nextInt(int n){return 0;} public double nextDouble(){return .30;}};
        GameProgress noProcParty=stage(0);Battle noProcBattle=new Battle(noProcParty,noProc,false,0);
        Enemy noProcAttack=Enemy.sentinel("Attack",true,GameSettings.Difficulty.NORMAL,0,1,0);replace(noProcBattle,noProcAttack);
        Student bogart=noProcParty.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow(); int bogartHp=bogart.getHp();
        check(noProcBattle.enemyAttack(noProcAttack)&&bogart.getHp()==bogartHp-11,"30 percent boundary does not pierce defense");
        check(noProcBattle.enemyAttack(noProcAttack)&&noProcParty.getParty().stream().allMatch(s->noProcBattle.getStunTurns(s)==0),"30 percent boundary does not stun");
        Enemy veteran=Enemy.sentinel("Veteran",false,GameSettings.Difficulty.HARD,0,50,0);
        GameProgress veteranParty=stage(0);Battle veteranBattle=new Battle(veteranParty,fixed(),false,0);replace(veteranBattle,veteran);
        int baseDef=veteran.getDefense();check(veteranBattle.enemyAttack(veteran)&&veteran.getDefense()>baseDef+15,"skin scales with party level and difficulty");
        check(SentinelVariantsTest.class.getResource("/assets/sprites/attack-sentinel-v1.png")!=null,"attack art packaged");
        System.out.println("Sentinel variants checks passed");
    }
}
