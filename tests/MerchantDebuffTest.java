import game.model.*;
import java.util.*;

public final class MerchantDebuffTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Random rolls(double hit,boolean last){return new Random(){@Override public int nextInt(int n){return last?n-1:0;}@Override public double nextDouble(){return hit;}};}
    static GameProgress party(Integer... ids){GameProgress p=new GameProgress(List.of(ids));p.advance();p.advance();p.advance();return p;}
    static Battle fight(GameProgress p,Random r){return Battle.merchantBattle(p,r,GameSettings.Difficulty.NORMAL);}
    public static void main(String[] args){
        GameProgress three=party(0,1,2);Battle missiles=fight(three,rolls(0,false));Enemy merchant=missiles.getEnemies().get(0);
        check(missiles.enemyAttack(merchant)&&missiles.enemyAttack(merchant),"cast missiles");
        check(three.getParty().stream().allMatch(s->missiles.getStunTurns(s)==2),"three different living heroes are stunned");
        check(missiles.getEffects().stream().filter(e->e.getStudent()!=null&&e.getAmount()<0).count()==3,"three missiles deal damage");
        GameProgress one=party(1);Battle burn=fight(one,rolls(0,false));Enemy caster=burn.getEnemies().get(0);Student hero=one.getParty().get(0);
        burn.enemyAttack(caster);burn.enemyAttack(caster);hero.heal(999);hero.restoreSp(25);int before=hero.getHp();
        check(burn.enemyAttack(caster)&&burn.getLastAbility().skill().equals("Mana Burn"),"mana burn cast");
        check(hero.getHp()==before-12&&hero.getSp()==25,"two hits at 60 percent missing SP without consuming SP");
        hero.heal(999);check(burn.enemyAttack(caster)&&burn.getLastAbility().skill().equals("Heat Stroke")&&burn.hasDamageOverTime(hero),"heat stroke active");
        check(!burn.advanceDamageOverTime(.99),"no early tick");int initial=hero.getHp();
        check(burn.advanceDamageOverTime(.01)&&hero.getHp()==initial-22&&burn.hasDamageOverTime(hero),"two effects tick once at one second");
        for(int i=0;i<4;i++)check(burn.advanceDamageOverTime(1),"remaining ticks");
        check(hero.getHp()==initial-112&&!burn.hasDamageOverTime(hero),"total 140 percent attack per effect over five seconds");
        GameProgress dual=party(0,1);Battle selected=fight(dual,rolls(.9,true));Enemy vendor=selected.getEnemies().get(0);
        Student tank=dual.getParty().stream().filter(s->s.getId()==Student.BOGART).findFirst().orElseThrow();
        selected.enemyAttack(vendor);selected.enemyAttack(vendor);selected.enemyAttack(vendor);selected.enemyAttack(vendor);
        check(selected.hasDamageOverTime(tank),"heat stroke targets tank");tank.takeDamage(tank.getHp()-1);
        check(selected.selectStudent(tank)&&selected.advanceDamageOverTime(1)&&!tank.isAlive()&&selected.getCurrent()==null&&!selected.isOver(),"DOT knockout releases current actor without ending battle");
        check(MerchantDebuffTest.class.getResource("/assets/ui/damage-over-time-icon-v1.png")!=null,"DOT icon packaged");
        System.out.println("Merchant debuff and DOT checks passed");
    }
}
