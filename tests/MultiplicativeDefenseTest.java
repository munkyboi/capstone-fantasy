import game.model.*;

/** Checks proportional damage, rounding, the negative cap, and minimum damage. */
public final class MultiplicativeDefenseTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(Battle.damageAfterDefense(40,20)==34,"20 defense mitigates 40 power proportionally, rounded up");
        check(Battle.damageAfterDefense(100,20)==84,"20 defense scales with attack power");
        check(Battle.damageAfterDefense(11,-5)==12,"negative defense increases damage");
        check(Battle.damageAfterDefense(40,-20)==50,"minus 20 defense increases damage by 25 percent");
        check(Battle.damageAfterDefense(40,-100)==50,"negative defense is capped");
        check(Battle.damageAfterDefense(0,200)==1,"successful hits deal at least one damage");
        System.out.println("Multiplicative defense checks passed");
    }
}
