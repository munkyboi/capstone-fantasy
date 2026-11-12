import game.model.*;
import game.io.SaveStore;
import game.view.*;
import java.util.*;
import java.nio.file.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class MerchantEncounterTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static Random fixed(double d){return new Random(){public int nextInt(int n){return 0;}public double nextDouble(){return d;}};}
    static void key(JComponent c,String k){c.getActionMap().get(k).actionPerformed(new ActionEvent(c,0,k));}
    public static void main(String[] args)throws Exception {
        GameProgress p=new GameProgress(List.of(Student.BOGART));
        try{Battle.merchantBattle(p,fixed(0),GameSettings.Difficulty.NORMAL);throw new AssertionError("stage gate");}catch(IllegalArgumentException expected){}
        p.advance();p.advance();p.advance();
        Battle b=Battle.merchantBattle(p,fixed(0),GameSettings.Difficulty.NORMAL); Enemy m=b.getEnemies().get(0);
        check(m.getHp()==350&&m.getSp()==800&&m.getAttack()==40&&m.getDefense()==10&&m.getSpeed()==4&&m.getAgility()==10&&m.getAccuracy()==8&&m.getActionThreshold()==40,"base stats");
        check(b.enemyAttack(m)&&b.getEnemies().size()==3&&m.getSp()==710&&b.getLastAbility().skill().equals("Mirror Image"),"mirror summon");
        Enemy image=b.getEnemies().get(1);check(image.isMirror()&&image.getMaxHp()==263&&image.getMaxSp()==600&&image.getAttack()==20&&image.getDefense()==5&&image.getAgility()==5&&image.getAccuracy()==4&&image.getSpeed()==4,"75 percent HP and SP; half other stats and full speed");
        check(b.enemyAttack(image)&&b.getLastAbility().skill().equals("Magic Missiles")&&b.getEnemies().size()==3,"mirror can cast but cannot summon");
        Student s=p.getParty().get(0);int hp=s.getHp();
        check(b.enemyAttack(m)&&b.getLastAbility().skill().equals("Magic Missiles")&&m.getSp()==655&&s.getHp()<hp&&b.getStunTurns(s)==2,"missiles and stun");
        check(b.consumeStunTurn(s)&&b.getStunTurns(s)==1&&b.consumeStunTurn(s)&&!b.consumeStunTurn(s),"stun lasts two turns");
        s.restoreSp(15);int burnHp=s.getHp();check(b.enemyAttack(m)&&b.getLastAbility().skill().equals("Mana Burn")&&s.getHp()==burnHp-24&&s.getSp()==15&&b.getEffects().stream().filter(e->e.getStudent()==s).count()==2,"mana burn deals damage from missing SP twice");
        check(b.enemyAttack(m)&&b.getLastAbility().skill().equals("Heat Stroke")&&b.hasDamageOverTime(s)&&b.getEffects().stream().filter(e->e.getStudent()==s).count()==2,"heat stroke applies damage over time twice");
        GameProgress missParty=new GameProgress(List.of(Student.BOGART));missParty.advance();missParty.advance();missParty.advance();
        Battle miss=Battle.merchantBattle(missParty,fixed(.6),GameSettings.Difficulty.NORMAL);Enemy missMerchant=miss.getEnemies().get(0);
        check(miss.enemyAttack(missMerchant)&&miss.enemyAttack(missMerchant)&&miss.getStunTurns(missParty.getParty().get(0))==0,"missiles miss at 60 percent boundary");
        GameProgress cooldownParty=new GameProgress(List.of(Student.BOGART));cooldownParty.advance();cooldownParty.advance();cooldownParty.advance();
        Battle cooldown=Battle.merchantBattle(cooldownParty,fixed(0),GameSettings.Difficulty.NORMAL);Enemy caster=cooldown.getEnemies().get(0);
        check(cooldown.enemyAttack(caster),"initial copies");
        for(int turn=0;turn<10;turn++){cooldownParty.getParty().get(0).heal(999);check(cooldown.enemyAttack(caster),"cooldown action");}
        check(cooldown.getEnemies().stream().filter(Enemy::isMirror).count()==2,"active copies prevent another summon");
        cooldown.getEnemies().stream().filter(Enemy::isMirror).forEach(e->e.takeDamage(9999));
        cooldownParty.getParty().get(0).heal(999);check(cooldown.enemyAttack(caster)&&cooldown.getLastAbility().skill().equals("Mirror Image")&&cooldown.getEnemies().size()==3,"copies can be summoned after cooldown and defeat");
        m.takeDamage(99999);check(b.isVictory(),"merchant defeat ends battle even with copies");
        p.unlockCarbonMarket();
        Path dir=Files.createTempDirectory("merchant-save");try{SaveStore store=new SaveStore(dir);for(SaveStore.Format format:SaveStore.Format.values()){
            store.save(1,format,p);check(store.load(1,format).isMerchantDefeated(),"unlock persists "+format);
        }}finally{try(var files=Files.walk(dir)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
        SwingUtilities.invokeAndWait(()->{GameProgress q=new GameProgress(List.of(Student.BOGART));q.advance();q.advance();q.advance();
            WorldMapPanel map=new WorldMapPanel((a,n)->{},a->{},()->{});int[] calls={0,0};map.setMerchantAction(a->calls[0]++);map.setShopAction(a->calls[1]++);map.open(q);
            key(map,"map-left");key(map,"map-up");key(map,"map-enter");check(calls[0]==1&&calls[1]==0,"locked starts challenge");
            q.unlockCarbonMarket();map.open(q);key(map,"map-left");key(map,"map-up");key(map,"map-enter");check(calls[1]==1,"unlocked opens shop");
            CampPanel camp=new CampPanel(a->{},()->{});camp.setShopAction(a->calls[1]++);camp.open(q,false);key(camp,"camp-market");check(calls[1]==2,"camp shortcut");
        });
        SwingUtilities.invokeAndWait(()->{try {
            GameProgress q=new GameProgress(List.of(Student.BOGART));q.advance();q.advance();q.advance();
            BattlePanel panel=new BattlePanel(()->{}, ignored->{}, new GameSettings(),fixed(0));panel.startMerchant(q);
            java.lang.reflect.Field field=BattlePanel.class.getDeclaredField("battle");field.setAccessible(true);Battle fight=(Battle)field.get(panel);
            Enemy vendor=fight.getEnemies().get(0);fight.attack(vendor);
            java.lang.reflect.Field partyPointsField=BattlePanel.class.getDeclaredField("actionPoints");partyPointsField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<Student,Double> partyPoints=(Map<Student,Double>)partyPointsField.get(panel);partyPoints.replaceAll((student,value)->0.0);
            java.lang.reflect.Field pointsField=BattlePanel.class.getDeclaredField("enemyPoints");pointsField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<Enemy,Double> points=(Map<Enemy,Double>)pointsField.get(panel);points.put(vendor,40.0);
            java.lang.reflect.Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);advance.invoke(panel,.01);
            check(fight.getEnemies().size()==3&&fight.getEnemies().stream().allMatch(points::containsKey),"copies receive action bars");
            vendor.takeDamage(99999);java.lang.reflect.Method refresh=BattlePanel.class.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(panel);
            check(q.isMerchantDefeated()&&q.getEncounter()==3&&!q.getInventory().isRewardClaimed(2),"victory unlocks without stage rewards or progression");
            for(String n:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")){java.lang.reflect.Field timer=BattlePanel.class.getDeclaredField(n);timer.setAccessible(true);((javax.swing.Timer)timer.get(panel)).stop();}
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}});
        System.out.println("Merchant encounter checks passed");
    }
}
