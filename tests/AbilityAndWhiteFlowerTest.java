import game.model.*;
import game.io.SaveStore;
import game.view.*;
import java.util.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/** Ability announcements and rare, persistent revival consumables. */
public final class AbilityAndWhiteFlowerTest {
    static void check(boolean value, String msg) { if (!value) throw new AssertionError(msg); }
    static Random fixed(double roll) { return new Random() { public int nextInt(int n) { return 0; } public double nextDouble() { return roll; } }; }
    static void key(JComponent panel, String name) { panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name)); }
    static Object field(Object object, String name) throws Exception { Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    static Student student(GameProgress p,int id) {return p.getParty().stream().filter(s -> s.getId()==id).findFirst().orElseThrow();}
    public static void main(String[] args) throws Exception {
        for (int size=1; size<=4; size++) {
            GameProgress fresh = new GameProgress(java.util.stream.IntStream.range(0,size).boxed().toList());
            check(fresh.getInventory().getWhiteFlowers()==1,"new party starts with one flower: " + size);
        }
        for (int id=0;id<4;id++) {
            GameProgress p = new GameProgress(List.of(id)); Battle b = new Battle(p,fixed(0)); Student actor=student(p,id); b.selectStudent(actor);
            check(b.ability(actor,b.getEnemies().get(0)),"ability succeeds " + id);
            String[] names={"Arcane Burst","Cadet Strike","Twin Shot","Angel Hug"};
            check(b.getLastAbility().caster().equals(actor.getName()) && b.getLastAbility().skill().equals(names[id]),"hero announcement " + id);
        }
        Battle monsters=new Battle(List.of(1),fixed(0)); Enemy w=monsters.getEnemies().get(0);
        check(monsters.enemyAttack(w) && monsters.getLastAbility()==null,"normal attack has no announcement");
        monsters.enemyAttack(w); monsters.enemyAttack(w);
        check(monsters.getLastAbility()!=null && monsters.getLastAbility().skill().equals("Arcane Spark"),"monster skill announcement");
        GameProgress finalParty = new GameProgress(List.of(1)); for(int i=0;i<4;i++) finalParty.advance();
        Battle bossBattle = new Battle(finalParty, fixed(0), false, 4, GameSettings.Difficulty.EASY);
        Enemy boss=bossBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
        for(int i=0;i<4;i++) check(bossBattle.enemyAttack(boss),"boss acts " + i);
        check(bossBattle.getLastAbility()!=null && bossBattle.getLastAbility().skill().equals("Crystal Pulse"),"boss skill announcement");
        GameProgress stage2 = new GameProgress(List.of(1)); stage2.advance();
        Battle healing = new Battle(stage2, fixed(0)); Enemy sentinel=healing.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Enemy ally=healing.getEnemies().stream().filter(e -> e!=sentinel).findFirst().orElseThrow(); ally.takeDamage(40);
        check(healing.enemyAttack(sentinel) && healing.getLastAbility().skill().equals("Stone Mend"),"sentinel heal announcement");
        Enemy dead=new Enemy("Library Wisp A"); dead.takeDamage(999);
        GameProgress loot=new GameProgress(List.of(0,1)); loot.getInventory().restoreWhiteFlowers(0);
        check(MonsterLoot.award(List.of(dead),loot,fixed(.049)).contains("white_flower"),"rare drop succeeds below 5 percent");
        check(!MonsterLoot.award(List.of(dead),loot,fixed(.05)).contains("white_flower"),"rare drop fails at 5 percent");
        check(loot.getInventory().getWhiteFlowers()==1,"drop added to inventory");
        Student fallen=student(loot,1), alive=student(loot,0); fallen.takeDamage(9999);
        Battle fight=new Battle(loot,fixed(0)); check(fight.selectStudent(alive),"reviver ready");
        check(!fight.useWhiteFlower(alive) && loot.getInventory().getWhiteFlowers()==1,"cannot waste on living ally");
        check(fight.useWhiteFlower(fallen) && fallen.getHp()==45 && loot.getInventory().getWhiteFlowers()==0,"revive restores 30 percent and consumes one");
        check(fight.getEffects().stream().anyMatch(effect -> effect.getStudent()==fallen && effect.getAmount()==45),"revive shows healing");
        fallen.takeDamage(9999); check(loot.getInventory().useWhiteFlower(fallen)==0 && !fallen.isAlive(),"no stock means no revive");
        loot.getInventory().addWhiteFlower(); loot.getInventory().addWhiteFlower(); loot.advance();
        Path dir=Files.createTempDirectory("white-flower-");
        try { SaveStore store=new SaveStore(dir); for(SaveStore.Format format:SaveStore.Format.values()) {
            store.save(1,format,loot); GameProgress loaded=store.load(1,format);
            check(loaded.getInventory().getWhiteFlowers()==2 && !student(loaded,1).isAlive(),"item and KO survive " + format);
        }
        GameProgress spent = new GameProgress(List.of(0,1)); student(spent,1).takeDamage(9999);
        check(spent.getInventory().useWhiteFlower(student(spent,1)) > 0, "use starting flower"); spent.advance();
        for (SaveStore.Format format : SaveStore.Format.values()) {
            store.save(2,format,spent); check(store.load(2,format).getInventory().getWhiteFlowers()==0,
                    "a spent flower is not replenished on load: " + format);
        }
        Path old=dir.resolve("slot-1.txt"); String oldText=Files.readString(old).replace("version=9", "version=8")
                .replaceAll("(?m)^inventory\\.whiteFlowers=.*\\R", "");
        Files.writeString(old,oldText); check(store.load(1,SaveStore.Format.TEXT).getInventory().getWhiteFlowers()==0,"older saves default to no flowers");
        } finally { try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);} }
        SwingUtilities.invokeAndWait(() -> {try {
            GameProgress p=new GameProgress(List.of(0,1)); student(p,1).takeDamage(9999);
            BattlePanel panel=new BattlePanel(()->{}, ignored->{},new GameSettings(),fixed(0));panel.start(p,false,0);
            key(panel,"battle-down"); key(panel,"battle-down"); key(panel,"battle-confirm");
            check((boolean)field(panel,"choosingItem"),"item picker shown when both consumables exist");
            key(panel,"battle-down"); key(panel,"battle-confirm");
            check(field(panel,"targeting").toString().equals("WHITE_FLOWER"),"flower selects KO targets");
            key(panel,"battle-confirm"); check(student(p,1).getHp()==45 && p.getInventory().getWhiteFlowers()==0,"keyboard revive");
            @SuppressWarnings("unchecked") Map<Student,Double> points=(Map<Student,Double>)field(panel,"actionPoints");
            check(points.get(student(p,1))==0.0,"revived ally begins with empty AP");
            GameProgress mage=new GameProgress(List.of(0)); BattlePanel ability=new BattlePanel(()->{}, ignored->{},new GameSettings(),fixed(0));ability.start(mage,false,0);
            key(ability,"battle-down");key(ability,"battle-confirm");key(ability,"battle-confirm");
            Battle.AbilityAnnouncement alert=(Battle.AbilityAnnouncement)field(ability,"abilityAnnouncement");
            check(alert!=null && alert.caster().equals("Zack") && alert.skill().equals("Arcane Burst"),"battle ability popup state");
            javax.swing.Timer alertTimer=(javax.swing.Timer)field(ability,"abilityAlertTimer");
            check(alertTimer.isRunning(),"popup expires on timer");
            Method announce=BattlePanel.class.getDeclaredMethod("announceAbility");announce.setAccessible(true);announce.invoke(ability);
            check(((java.util.Queue<?>)field(ability,"abilityAlerts")).size()==1,"rapid skills are queued");
            alertTimer.stop(); for(var listener:alertTimer.getActionListeners()) listener.actionPerformed(new ActionEvent(alertTimer,0,"test"));
            check(field(ability,"abilityAnnouncement")!=null && ((java.util.Queue<?>)field(ability,"abilityAlerts")).isEmpty(),"next alert displays after first");
            alertTimer.stop(); for(var listener:alertTimer.getActionListeners()) listener.actionPerformed(new ActionEvent(alertTimer,0,"test"));
            check(field(ability,"abilityAnnouncement")==null,"last alert expires");
            GameProgress campP=new GameProgress(List.of(0,1)); student(campP,1).takeDamage(9999);
            CampPanel camp=new CampPanel(x->{},()->{});camp.open(campP,false);key(camp,"camp-right");key(camp,"camp-revive");
            check(student(campP,1).getHp()==45 && campP.getInventory().getWhiteFlowers()==0,"camp revival");
            for(BattlePanel b:List.of(panel,ability)) for(String timer:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")) ((javax.swing.Timer)field(b,timer)).stop();
        } catch(Exception e){throw new RuntimeException(e);} });
        System.out.println("Ability popup and White Flower checks passed");
    }
}
