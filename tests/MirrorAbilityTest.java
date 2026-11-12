import game.model.*;
import java.util.*;

public final class MirrorAbilityTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void run(double roll,int amountRoll,boolean hits){
        GameProgress p=new GameProgress(List.of(Student.BOGART));p.advance();p.advance();p.advance();
        Random r=new Random(){@Override public int nextInt(int n){return n==21?amountRoll:0;}@Override public double nextDouble(){return roll;}};
        Battle b=Battle.merchantBattle(p,r,GameSettings.Difficulty.NORMAL);Enemy merchant=b.getEnemies().get(0);Student hero=p.getParty().get(0);
        check(b.enemyAttack(merchant),"summon copies");Enemy copy=b.getEnemies().stream().filter(Enemy::isMirror).findFirst().orElseThrow();
        int hp=hero.getHp();check(b.enemyAttack(copy)&&b.getLastAbility().skill().equals("Magic Missiles"),"copy uses missile");
        check(b.getEffects().stream().filter(e->e.getStudent()==hero).count()==1+(hits?1:0),"one missile and optional stun");
        if(hits){int power=(int)Math.ceil(copy.getAttack()*(80+amountRoll)/100.0);check(hero.getHp()==hp-Math.max(1,power-hero.getDefense())&&b.getStunTurns(hero)==1,"copy power and one-turn stun");}
        else check(hero.getHp()==hp&&b.getStunTurns(hero)==0,"copy miss at 50 percent boundary");
        for(int i=0;i<5;i++){hero.heal(999);check(b.enemyAttack(copy),"copy acts");check(b.getLastAbility()==null||b.getLastAbility().skill().equals("Magic Missiles"),"copy never casts forbidden abilities");}
        for(int i=0;i<6;i++){hero.heal(999);check(b.enemyAttack(merchant),"merchant acts with copies");check(b.getLastAbility()==null||!b.getLastAbility().skill().equals("Magic Missiles"),"merchant cannot cast missiles with living copies");}
    }
    public static void main(String[]args){run(.499999,0,true);run(0,20,true);run(.5,0,false);System.out.println("Mirror ability checks passed");}
}
