import game.model.*;
import game.view.BattlePanel;
import game.io.SaveStore;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/** Protecting allies, intercepting damage, expiry, and level-scaled ability costs. */
public final class DefendMechanicsTest {
    static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    static Random fixed() { return new Random() { public int nextInt(int bound) { return 0; } public double nextDouble() { return 0; } }; }
    static Student byId(Battle battle, int id) { return battle.getParty().stream().filter(s -> s.getId() == id).findFirst().orElseThrow(); }
    static void key(BattlePanel panel,String action) { panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel,0,action)); }
    public static void main(String[] args) throws Exception {
        Battle battle = new Battle(List.of(0,1),fixed()); Student zack=byId(battle,0), bogart=byId(battle,1); Enemy enemy=battle.getEnemies().get(0);
        check(battle.selectStudent(bogart) && !battle.canDefend(bogart) && battle.defend(zack), "choose ally to protect");
        check(bogart.isDefending() && bogart.getDefense()==19 && HealthRegeneration.amount(bogart)==42, "defense and regeneration bonuses");
        int zhp=zack.getHp(), bhp=bogart.getHp();
        check(battle.enemyAttack(enemy) && zack.getHp()==zhp && bogart.getHp()==bhp-Battle.damageAfterDefense(enemy.getAttack(),bogart.getDefense()), "protector takes hit using boosted defense");
        check(battle.getBlockingStudent()==bogart && battle.getProtectedStudent()==zack && battle.getMessage().contains("protects"), "interception communicated");
        check(battle.selectStudent(zack), "another ally takes turn"); battle.defend(bogart);
        check(bogart.isDefending() && battle.getGuardedAlly(bogart)==zack, "guard persists across ally actions");
        check(battle.selectStudent(bogart) && !bogart.isDefending() && battle.getGuardedAlly(bogart)==null
                && bogart.getDefense()==16 && HealthRegeneration.amount(bogart)==12, "guard ends at defender's next action");
        Battle alone = new Battle(List.of(0),fixed()); Student solo=byId(alone,0);
        check(alone.selectStudent(solo) && alone.canDefend(solo) && alone.defend(solo), "solo student can defend");
        Battle knockout = new Battle(List.of(0,1),fixed()); Student protectedHero=byId(knockout,0), guard=byId(knockout,1); Enemy foe=knockout.getEnemies().get(0);
        check(knockout.selectStudent(guard) && knockout.defend(protectedHero), "guard before KO"); guard.takeDamage(9999);
        int before=protectedHero.getHp(); check(knockout.enemyAttack(foe) && protectedHero.getHp()<before, "KO defender cannot intercept");
        Battle victory = new Battle(List.of(0,1),fixed()); Student ward=byId(victory,0), protector=byId(victory,1);
        check(victory.selectStudent(protector) && victory.defend(ward), "guard before victory");
        victory.getEnemies().forEach(e -> e.takeDamage(e.getHp()-1));
        check(victory.selectStudent(ward) && victory.attack(victory.getEnemies().get(0)) && victory.isVictory(), "finish encounter");
        check(!protector.isDefending() && HealthRegeneration.amount(protector)==12, "guard bonuses end after battle");
        int[] costs={15,5,5,10}; for(int id=0;id<4;id++){Student s=new Student(id);check(s.getAbilitySpCost()==costs[id],"initial ability cost");
            s.gainExperience(300); check(s.getAbilitySpCost()==costs[id]+3,"ability cost grows by one per level");
            s.restoreSp(s.getAbilitySpCost()-1); check(!s.spendAbilitySp(),"cannot use an ability below the level-scaled cost");}
        Path saveDir = Files.createTempDirectory("defend-sp-");
        try {
            GameProgress leveled = new GameProgress(List.of(0)); leveled.advance(); leveled.getParty().get(0).gainExperience(300);
            SaveStore store = new SaveStore(saveDir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, leveled);
                check(store.load(1, format).getParty().get(0).getAbilitySpCost() == 18, "scaled cost survives " + format);
            }
        } finally { try (var paths = Files.walk(saveDir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
        SwingUtilities.invokeAndWait(() -> {try {
            GameProgress p=new GameProgress(List.of(0,1)); BattlePanel panel=new BattlePanel(()->{}, ignored->{},new GameSettings(),fixed()); panel.start(p,false,0);
            Field field=BattlePanel.class.getDeclaredField("battle");field.setAccessible(true); Battle active=(Battle)field.get(panel);
            Student actor=active.getCurrent(); for(int i=0;i<3;i++)key(panel,"battle-down"); key(panel,"battle-confirm");
            Field targeting=BattlePanel.class.getDeclaredField("targeting");targeting.setAccessible(true);
            check(targeting.get(panel).toString().equals("DEFEND"),"Defend enters ally targeting"); key(panel,"battle-confirm");
            check(actor.isDefending() && active.getGuardedAlly(actor)!=null && active.getGuardedAlly(actor)!=actor,"keyboard confirms ally");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer")){Field t=BattlePanel.class.getDeclaredField(name);t.setAccessible(true);((javax.swing.Timer)t.get(panel)).stop();}
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}});
        System.out.println("Defend mechanics and ability cost checks passed");
    }
}
