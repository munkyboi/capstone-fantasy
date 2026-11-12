import game.model.*;
import java.util.*;

/** Recall consumes the Merchant's living copies and restores a share of their remaining resources. */
public final class MerchantRecallTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Random fixed(){return new Random(){@Override public int nextInt(int n){return 0;}@Override public double nextDouble(){return 0;}};}
    static Battle encounter(){GameProgress p=new GameProgress(List.of(Student.BOGART));p.advance();p.advance();p.advance();return Battle.merchantBattle(p,fixed(),GameSettings.Difficulty.NORMAL);}
    public static void main(String[] args){
        Battle b=encounter();Enemy merchant=b.getEnemies().get(0);Student hero=b.getParty().get(0);
        check(b.enemyAttack(merchant),"summons copies");List<Enemy> copies=b.getEnemies().stream().filter(Enemy::isMirror).toList();
        Enemy first=copies.get(0),second=copies.get(1);first.takeDamage(63);
        check(b.enemyAttack(first)&&first.getSp()==545,"copy spends SP independently");hero.heal(999);
        merchant.takeDamage(200);
        int expectedHp=(int)Math.ceil((first.getHp()+second.getHp())*.10);
        int expectedSp=Math.min(merchant.getMaxSp()-(merchant.getSp()-30),(int)Math.ceil((first.getSp()+second.getSp())*.10));
        int hpBefore=merchant.getHp(),spBefore=merchant.getSp();
        check(b.enemyAttack(merchant)&&b.getLastAbility().skill().equals("Recall"),"injured merchant recalls");
        check(b.getEnemies().size()==1&&merchant.getHp()==hpBefore+expectedHp&&merchant.getSp()==spBefore-30+expectedSp,"resources transferred and capped");
        check(b.getEffects().stream().anyMatch(e->e.getEnemy()==merchant&&e.getAmount()==expectedHp),"heal indicator appears on merchant");
        check(!b.enemyAttack(first),"recalled copy cannot act");
        merchant.takeDamage(150);
        for (int i=0;i<3;i++) { hero.heal(999); check(b.enemyAttack(merchant) && (b.getLastAbility()==null || !b.getLastAbility().skill().equals("Recall")),"recall cannot repeat during cooldown"); }
        hero.heal(999); check(b.enemyAttack(merchant) && b.getLastAbility().skill().equals("Mirror Image"),"copies can return after their cooldown");
        hero.heal(999); check(b.enemyAttack(merchant) && b.getLastAbility().skill().equals("Recall"),"recall is available again after five turns");
        Battle one=encounter();Enemy vendor=one.getEnemies().get(0);check(one.enemyAttack(vendor),"summon for one-copy test");
        List<Enemy> images=one.getEnemies().stream().filter(Enemy::isMirror).toList();images.get(0).takeDamage(99999);vendor.takeDamage(320);
        check(one.enemyAttack(vendor)&&one.getLastAbility().skill().equals("Recall"),"recall surviving copy");
        check(vendor.getHp()==30+(int)Math.ceil(images.get(1).getHp()*.10),"defeated copy contributes nothing");
        GameProgress highParty=new GameProgress(List.of(Student.BOGART));highParty.advance();highParty.advance();highParty.advance();
        Random high=new Random(){@Override public int nextInt(int n){return n==6?5:0;}@Override public double nextDouble(){return 0;}};
        Battle upper=Battle.merchantBattle(highParty,high,GameSettings.Difficulty.NORMAL);Enemy upperMerchant=upper.getEnemies().get(0);
        check(upper.enemyAttack(upperMerchant),"summon copies for maximum roll");
        List<Enemy> upperCopies=upper.getEnemies().stream().filter(Enemy::isMirror).toList();upperCopies.get(0).takeDamage(99999);upperMerchant.takeDamage(320);
        check(upper.enemyAttack(upperMerchant)&&upperMerchant.getHp()==30+(int)Math.ceil(upperCopies.get(1).getHp()*.15)
                &&upperMerchant.getSp()==770,"15 percent upper bound applies to HP and SP");
        Battle healthy=encounter();Enemy intact=healthy.getEnemies().get(0);check(healthy.enemyAttack(intact)&&healthy.enemyAttack(intact),"healthy merchant acts");
        check(!healthy.getLastAbility().skill().equals("Recall")&&healthy.getEnemies().size()==3,"copies retained until recall is useful");
        System.out.println("Merchant Recall checks passed");
    }
}
