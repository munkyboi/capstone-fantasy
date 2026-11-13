import game.model.*;
import game.view.BattlePanel;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

public final class VictoryChoicesTest {
    static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    static Object field(Object o,String n)throws Exception {Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void refresh(BattlePanel p)throws Exception {Method m=BattlePanel.class.getDeclaredMethod("refresh");m.setAccessible(true);m.invoke(p);}
    static void key(BattlePanel p,String name){p.getActionMap().get(name).actionPerformed(new ActionEvent(p,0,name));}
    static void stop(BattlePanel p)throws Exception {for(String n:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(p,n)).stop();}
    static Battle win(BattlePanel panel)throws Exception {Battle b=(Battle)field(panel,"battle");b.getEnemies().forEach(e->e.takeDamage(999999));refresh(panel);return b;}
    public static void main(String[]a)throws Exception {SwingUtilities.invokeAndWait(()->{try{
        int[] camps={0},explores={0}; GameProgress p=new GameProgress(List.of(Student.CHARICE));
        BattlePanel panel=new BattlePanel(()->{},q->camps[0]++,new GameSettings(),new Random(42));
        panel.setExploreAction((q,stage)->{explores[0]++;check(stage==0,"replay same stage");if(stage==q.getEncounter())q.advance();panel.start(q,false,stage);});
        panel.start(p,false,0);Battle original=win(panel);int petot=p.getPetot();refresh(panel);check(p.getPetot()==petot,"rewards paid once");
        JPanel scene=(JPanel)field(panel,"scene");scene.setSize(960,540);BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB);scene.paint(image.getGraphics());
        key(panel,"battle-left");key(panel,"battle-confirm");check(explores[0]==1 && camps[0]==0 && field(panel,"battle")!=original,"explore starts another battle");check(p.getEncounter()==1,"first victory advances progression");check(p.getInventory().isRewardClaimed(0),"one-time rewards stay claimed");
        win(panel);check(((Integer)field(panel,"victoryChoice"))==1,"camp is default"); Method buttonY=scene.getClass().getDeclaredMethod("rewardButtonY");buttonY.setAccessible(true);int clickY=(Integer)buttonY.invoke(scene)+10; scene.dispatchEvent(new MouseEvent(scene,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,550,clickY,1,false));check(camps[0]==1 && explores[0]==1,"camp button responds to click");
        stop(panel);
        GameProgress merchant=new GameProgress(List.of(Student.ZACK));for(int i=0;i<3;i++)merchant.advance();BattlePanel m=new BattlePanel(()->{},q->camps[0]++,new GameSettings(),new Random(1));m.setExploreAction((q,stage)->explores[0]++);m.startMerchant(merchant);win(m);check(merchant.isMerchantDefeated(),"merchant victory unlocks shop");key(m,"battle-left");key(m,"battle-confirm");check(camps[0]==2 && explores[0]==1,"merchant victory only offers camp");stop(m);
        System.out.println("Victory choice checks passed");
    }catch(Exception e){throw new RuntimeException(e);}});}
}
