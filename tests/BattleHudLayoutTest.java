import game.model.*;
import game.view.BattlePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class BattleHudLayoutTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            GameProgress p=new GameProgress(java.util.List.of(0,1,2,3));for(int i=0;i<4;i++)p.advance();
            BattlePanel panel=new BattlePanel(()->{},x->{},new GameSettings(),new Random(1));panel.start(p,false,4);
            JLabel[] labels=(JLabel[])field(panel,"partyStatus");JProgressBar[] bars=(JProgressBar[])field(panel,"apBars");JScrollPane log=(JScrollPane)field(panel,"logScroll");
            Battle b=(Battle)field(panel,"battle");check(b.getEnemies().size()==6,"six monsters");
            for(int i=0;i<4;i++){String text=labels[i].getText();Student s=b.getParty().get(i);check(text.contains(s.getName())&&text.contains(s.getHp()+" HP")&&text.contains(s.getSp()+" SP")&&!text.contains("/"),"compact hero values");check(bars[i].getPreferredSize().width<=76,"compact hero AP");}
            check(labels[0].getForeground().equals(new Color(250,218,143)),"selected hero highlight retained");
            for(int width:new int[]{960,1440}){panel.setSize(width,width*9/16);layout(panel);Point party=SwingUtilities.convertPoint(labels[0],0,0,panel),logPos=SwingUtilities.convertPoint(log,0,0,panel);check(logPos.x>party.x&&log.getWidth()>0,"log is rightmost column " + width);}
            panel.setSize(960,540);layout(panel);BufferedImage out=new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB);Graphics2D g=out.createGraphics();panel.printAll(g);g.dispose();ImageIO.write(out,"png",Path.of("/tmp/capstone-battle-hud.png").toFile());
            for(String n:java.util.List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","sparkExplosionTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,n)).stop();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Battle HUD layout checks passed");
    }
}
