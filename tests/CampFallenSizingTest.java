import game.model.*;
import game.view.CampPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Fallen camp sprites stay readable and fit beside other party members. */
public final class CampFallenSizingTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Rectangle bounds(CampPanel panel,Student s,int center)throws Exception{
        BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        Method draw=CampPanel.class.getDeclaredMethod("drawCharacter",Graphics2D.class,Student.class,int.class);draw.setAccessible(true);draw.invoke(panel,g,s,center);g.dispose();
        int x1=960,x2=-1,y1=540,y2=-1;for(int y=0;y<540;y++)for(int x=0;x<960;x++)if((image.getRGB(x,y)>>>24)>32){x1=Math.min(x1,x);x2=Math.max(x2,x);y1=Math.min(y1,y);y2=Math.max(y2,y);}
        return new Rectangle(x1,y1,x2-x1+1,y2-y1+1);
    }
    public static void main(String[] args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
        int[] positions={230,365,595,735};
        for(int id=0;id<4;id++){GameProgress p=new GameProgress(java.util.List.of(id));Student s=p.getParty().get(0);s.takeDamage(9999);CampPanel camp=new CampPanel(x->{},()->{});camp.open(p,false);Rectangle fallen=bounds(camp,s,positions[id]);check(fallen.width>=150 && fallen.y+fallen.height<=430,"large grounded sprite " + id);}
        GameProgress p=new GameProgress(java.util.List.of(0,1,2,3));for(Student s:p.getParty())s.takeDamage(9999);CampPanel camp=new CampPanel(x->{},()->{});camp.open(p,false);
        Rectangle[] boxes=new Rectangle[4];for(Student s:p.getParty()){boxes[s.getId()]=bounds(camp,s,positions[s.getId()]);check(boxes[s.getId()].width>=100,"sprite stays visible " + s.getName());}
        for(int i=0;i<3;i++)check(boxes[i].x+boxes[i].width<boxes[i+1].x,"fallen neighbors do not overlap " + i);
        GameProgress pair=new GameProgress(java.util.List.of(1,2));CampPanel pairCamp=new CampPanel(x->{},()->{});Student bogart=pair.getParty().stream().filter(s->s.getId()==1).findFirst().orElseThrow();
        Student charice=pair.getParty().stream().filter(s->s.getId()==2).findFirst().orElseThrow();charice.takeDamage(9999);pairCamp.open(pair,false);
        Rectangle standing=bounds(pairCamp,bogart,positions[1]);Rectangle down=bounds(pairCamp,charice,positions[2]);
        check(standing.x+standing.width<down.x && down.width>=150,"fallen hero fits beside standing hero");
    }catch(Exception e){throw new RuntimeException(e);}});System.out.println("Camp fallen sprite sizing checks passed");}
}
