import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

/** Keyboard and button state for Hero's Ring restrictions. */
public final class HeroesRingControlsTest {
    static Object field(Object target,String name)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(List.of(Student.BOGART));Student hero=p.getParty().get(0);
            p.getInventory().addEquipment("heroes_ring");check(p.equip(hero,Equipment.Slot.ACCESSORY,"heroes_ring"),"equip ring");
            BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),new Random(3));panel.start(p,false,0);
            JButton defend=(JButton)field(panel,"defend"), escape=(JButton)field(panel,"escape");
            check(!defend.isEnabled()&&!escape.isEnabled(),"restricted buttons disabled");
            for(int i=0;i<3;i++)panel.getActionMap().get("battle-down").actionPerformed(new ActionEvent(panel,0,""));
            check(((Integer)field(panel,"commandIndex"))==0,"keyboard skips unavailable commands");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,name)).stop();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Hero's Ring controls checks passed");
    }
}
