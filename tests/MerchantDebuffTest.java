import game.model.*;
import java.util.*;

public final class MerchantDebuffTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Random rolls(double hit,boolean last){return new Random(){@Override public int nextInt(int n){return last?n-1:0;}@Override public double nextDouble(){return hit;}};}
    static GameProgress party(Integer... ids){GameProgress p=new GameProgress(List.of(ids));p.advance();p.advance();p.advance();return p;}
    static Battle fight(GameProgress p,Random r){return Battle.merchantBattle(p,r,GameSettings.Difficulty.NORMAL);}
    static void dismissCopies(Battle b){b.getEnemies().stream().filter(Enemy::isMirror).forEach(e->e.takeDamage(9999));}
    public static void main(String[] args){
        GameProgress three=party(0,1,2);Battle missiles=fight(three,rolls(0,false));Enemy merchant=missiles.getEnemies().get(0);
        check(missiles.enemyAttack(merchant),"summon copies");dismissCopies(missiles);check(missiles.enemyAttack(merchant),"cast missiles");
        check(three.getParty().stream().allMatch(s->missiles.getStunTurns(s)==2),"three different living heroes are stunned");
        check(missiles.getEffects().stream().filter(e->e.getStudent()!=null&&e.getAmount()<0).count()==3,"three missiles deal damage");
        GameProgress one=party(1);Battle burn=fight(one,rolls(0,false));Enemy caster=burn.getEnemies().get(0);Student hero=one.getParty().get(0);
        burn.enemyAttack(caster);dismissCopies(burn);burn.enemyAttack(caster);hero.heal(999);hero.restoreSp(25);int before=hero.getHp();
        check(burn.enemyAttack(caster)&&burn.getLastAbility().skill().equals("Mana Burn"),"mana burn cast");
        check(hero.getHp()==before-5&&hero.getSp()==25,"one hit at 50 percent missing SP without consuming SP");
        for (double chance : new double[]{.599999, .6}) {
            GameProgress chanceParty=party(1);Student chanceHero=chanceParty.getParty().get(0);
            Random rng=new Random(){@Override public int nextInt(int n){return n==11?10:0;}@Override public double nextDouble(){return chance;}};
            Battle chanceBattle=fight(chanceParty,rng);Enemy chanceCaster=chanceBattle.getEnemies().get(0);
            chanceBattle.enemyAttack(chanceCaster);dismissCopies(chanceBattle);chanceBattle.enemyAttack(chanceCaster);chanceHero.heal(999);chanceHero.restoreSp(25);int hp=chanceHero.getHp();
            check(chanceBattle.enemyAttack(chanceCaster)&&chanceBattle.getLastAbility().skill().equals("Mana Burn"),"mana burn chance test");
            check(chanceHero.getHp()==hp-(chance<.6?6:0),"accuracy boundary and 60 percent upper damage");
        }
        hero.heal(999);check(burn.enemyAttack(caster)&&burn.getLastAbility().skill().equals("Heat Stroke")&&burn.hasDamageOverTime(hero),"heat stroke active");
        int initial=hero.getHp();
        check(!burn.tickDamageOverTime(caster)&&hero.getHp()==initial,"another participant's turn does not tick the target's DOT");
        check(burn.tickDamageOverTime(hero)&&hero.getHp()==initial-22&&burn.hasDamageOverTime(hero),"two effects tick once on target turn");
        for(int i=0;i<4;i++)check(burn.tickDamageOverTime(hero),"remaining ticks");
        check(hero.getHp()==initial-112&&!burn.hasDamageOverTime(hero),"total 140 percent attack per effect over five turns");
        boolean[] allowTickHit={false};
        Random selectedRolls=new Random(){public int nextInt(int n){return n-1;}public double nextDouble(){return allowTickHit[0]?0:.9;}};
        GameProgress dual=party(0,1);Battle selected=fight(dual,selectedRolls);Enemy vendor=selected.getEnemies().get(0);
        Student tank=dual.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();
        selected.enemyAttack(vendor);dismissCopies(selected);selected.enemyAttack(vendor);selected.enemyAttack(vendor);selected.enemyAttack(vendor);
        check(selected.hasDamageOverTime(tank),"heat stroke targets tank");tank.takeDamage(tank.getHp()-1);allowTickHit[0]=true;
        check(selected.tickDamageOverTime(tank)&&!tank.isAlive()&&selected.getCurrent()==null&&!selected.isOver(),"DOT knockout does not end battle while an ally lives");
        check(MerchantDebuffTest.class.getResource("/assets/ui/damage-over-time-icon-v1.png")!=null,"DOT icon packaged");
        System.out.println("Merchant debuff and DOT checks passed");
    }
}
