import game.model.*;
import game.view.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/** A stunned hero has a persistent battlefield icon until the final skipped turn. */
public final class StunIndicatorTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container) layout((Container) child); }
    static BufferedImage paint(BattlePanel panel) { BufferedImage image = new BufferedImage(960,720,BufferedImage.TYPE_INT_ARGB); Graphics2D g=image.createGraphics(); try { panel.paint(g); } finally { g.dispose(); } return image; }
    static int difference(BufferedImage a, BufferedImage b) { int n=0; for(int y=0;y<a.getHeight();y++) for(int x=0;x<a.getWidth();x++) if(a.getRGB(x,y)!=b.getRGB(x,y)) n++; return n; }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> { try {
            GameProgress p = new GameProgress(List.of(Student.BOGART)); p.advance(); p.advance(); p.advance();
            Random rng = new Random() { @Override public int nextInt(int n) { return 0; } @Override public double nextDouble() { return 0; } };
            BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), rng); panel.startMerchant(p);
            panel.setSize(960,720); layout(panel);
            Field bf=BattlePanel.class.getDeclaredField("battle"); bf.setAccessible(true); Battle b=(Battle)bf.get(panel);
            Student hero=p.getParty().get(0); Enemy merchant=b.getEnemies().get(0);
            check(b.attack(merchant) && b.enemyAttack(merchant) && b.enemyAttack(merchant), "merchant casts missiles");
            check(b.getStunTurns(hero)==2, "hero is stunned");
            check(StunIndicatorTest.class.getResource("/assets/ui/stunned-icon-v1.png")!=null, "stun artwork packaged");
            BufferedImage stunned=paint(panel); check(b.consumeStunTurn(hero), "skip first turn");
            BufferedImage firstTurn=paint(panel); check(difference(stunned,firstTurn)==0, "icon remains during second turn");
            check(b.consumeStunTurn(hero), "skip last turn"); BufferedImage recovered=paint(panel);
            check(difference(firstTurn,recovered)>50, "icon disappears when stun ends");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","abilityAlertTimer")) { Field field=BattlePanel.class.getDeclaredField(name);field.setAccessible(true);((Timer)field.get(panel)).stop(); }
        } catch(ReflectiveOperationException e) { throw new RuntimeException(e); }});
        System.out.println("Stun indicator checks passed");
    }
}
