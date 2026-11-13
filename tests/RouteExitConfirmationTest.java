import game.model.*;
import game.view.WorldMapPanel;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;

public final class RouteExitConfirmationTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void key(WorldMapPanel p,String action){p.getActionMap().get(action).actionPerformed(new ActionEvent(p,0,action));}
    public static void main(String[] args)throws Exception {SwingUtilities.invokeAndWait(()->{
        int[] menus={0},battles={0},camps={0};
        WorldMapPanel map=new WorldMapPanel((p,stage)->battles[0]++,p->camps[0]++,()->menus[0]++);
        GameProgress p=new GameProgress(List.of(Student.ZACK));p.advance();p.advance();p.advance();map.open(p);
        key(map,"map-menu"); check(menus[0]==0,"Escape opens confirmation");
        map.setSize(960,540);map.paint(new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB).getGraphics());
        key(map,"map-camp");check(camps[0]==0,"camp disabled while confirmation open");
        key(map,"map-enter");check(menus[0]==0,"No is default");
        key(map,"map-menu");key(map,"map-right");key(map,"map-menu");check(menus[0]==0,"Escape cancels prompt");
        key(map,"map-menu");key(map,"map-enter");check(menus[0]==0,"prompt resets to No");
        key(map,"map-left");key(map,"map-enter");key(map,"map-menu");check(menus[0]==0 && battles[0]==0,"Escape dismisses replay confirmation");
        key(map,"map-menu");key(map,"map-right");key(map,"map-enter");check(menus[0]==1,"Yes returns to menu");
        System.out.println("Route exit confirmation checks passed");
    });}
}
