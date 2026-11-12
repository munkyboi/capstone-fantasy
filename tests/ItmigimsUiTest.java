import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Checks the timed escape through the battle clock and victory rewards. */
public final class ItmigimsUiTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{try{
            Random random=new Random(){public int nextInt(int bound){return bound==100?95:0;}public double nextDouble(){return 0;}};
            GameProgress p=new GameProgress(List.of(0,1,2,3));BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),random);
            panel.start(p,true,0);Battle battle=(Battle)field(panel,"battle");Enemy goblin=battle.getEnemies().get(0);
            check(goblin.isLootGoblin(),"goblin is in the battle");goblin.takeDamage(1);
            @SuppressWarnings("unchecked") Map<Enemy,Double> points=(Map<Enemy,Double>)field(panel,"enemyPoints");
            Method advance=BattlePanel.class.getDeclaredMethod("advanceActionPoints",double.class);advance.setAccessible(true);
            for(int action=0;action<13;action++){points.put(goblin,(double)goblin.getActionThreshold());advance.invoke(panel,0.001);}
            check(goblin.hasEscaped() && battle.isVictory(),"goblin escapes on battle clock");
            check(field(panel,"attackingSprite")==goblin && (boolean)field(panel,"showEffects"),"escape animation is active");
            Battle.AbilityAnnouncement alert=(Battle.AbilityAnnouncement)field(panel,"abilityAnnouncement");
            check(alert!=null && alert.caster().equals("Itmigims") && alert.skill().equals("Escape"),"escape ability popup");
            check((int)field(panel,"victoryPetot")==0 && (int)field(panel,"victoryExperience")==0,"escaped enemy awards no currency or XP");
            check(p.getKits()==3 && p.getInventory().getWhiteFlowers()==1 && !p.getInventory().getEquipmentIds().contains("market_charm"),"escaped goblin awards no drops");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")) ((javax.swing.Timer)field(panel,name)).stop();
            for(boolean greed : new boolean[]{false,true}) {
                GameSettings settings=new GameSettings();settings.setGreedIsGood(greed);
                GameProgress win=new GameProgress(List.of(0,1,2,3));BattlePanel victory=new BattlePanel(()->{},x->{},settings,random);
                victory.start(win,false,0);Battle winBattle=(Battle)field(victory,"battle");winBattle.getEnemies().get(0).takeDamage(999999);
                Method refresh=BattlePanel.class.getDeclaredMethod("refresh");refresh.setAccessible(true);refresh.invoke(victory);
                int expected=greed?50000:5000;
                check(win.getPetot()==expected && (int)field(victory,"victoryPetot")==expected && (int)field(victory,"victoryExperience")==0,"defeated goblin awards Petot once and no XP");
                refresh.invoke(victory);check(win.getPetot()==expected,"currency is not awarded twice");
                for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")) ((javax.swing.Timer)field(victory,name)).stop();
            }
        }catch(Exception ex){throw new RuntimeException(ex);}});
        System.out.println("Itmigims battle UI checks passed");
    }
}
