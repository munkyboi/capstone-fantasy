import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Covers AP rates, SPD progression and approximately 60-Hz refresh timing. */
public final class BattleSpeedProgressionTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static double rate(BattlePanel panel, Student student,double base)throws Exception {
        Method method=BattlePanel.class.getDeclaredMethod("studentRate",Student.class,double.class);method.setAccessible(true);return (double)method.invoke(panel,student,base);
    }
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{try {
            GameSettings settings=new GameSettings(); BattlePanel panel=new BattlePanel(()->{},p->{},settings,new Random(1));
            for(int id=0;id<4;id++){
                Student hero=Student.create(id);
                for(GameSettings.BattleSpeed speed:GameSettings.BattleSpeed.values()){
                    settings.setBattleSpeed(speed);double base=settings.getApPerSecond();
                    check(Math.abs(rate(panel,hero,base)-base)<1e-9,"starting class rate " + id + " " + speed);
                }
            }
            Student angel=Student.create(Student.ANGEL);angel.gainExperience(500);
            check(angel.getLevel()==6 && angel.getUnmodifiedSpeed()==6,"level speed growth");
            check(Math.abs(rate(panel,angel,10)-12)<1e-9,"level growth accelerates AP");
            check(angel.allocateStatPoint(Student.Stat.SPD),"allocate speed");
            check(Math.abs(rate(panel,angel,10)-14)<1e-9,"allocated speed accelerates AP");
            Student hunter=Student.create(Student.CHARICE);GameProgress party=new GameProgress(List.of(Student.CHARICE));hunter=party.getParty().get(0);
            party.getInventory().addEquipment("demon_hunter");check(party.equip(hunter,Equipment.Slot.WEAPON,"demon_hunter"),"equip speed weapon");
            check(Math.abs(rate(panel,hunter,10)-10*hunter.getEffectiveSpeed()/19.0)<1e-9,"equipment speed included");
            for(String name:List.of("apTimer","effectTimer","victoryTimer")){Field f=BattlePanel.class.getDeclaredField(name);f.setAccessible(true);javax.swing.Timer timer=(javax.swing.Timer)f.get(panel);check(timer.getDelay()==16,"60 FPS timer " + name);timer.stop();}
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Battle speed and SPD progression checks passed");
    }
}
