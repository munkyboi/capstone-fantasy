import game.model.GameProgress;
import game.model.Student;
import game.view.BattlePanel;
import game.view.CampPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Checks character-specific KO sprites in battle and camp. */
public final class FallenSpriteTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static BufferedImage paint(JPanel panel) {
        panel.setSize(960, 540); panel.doLayout();
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); try { panel.paint(g); } finally { g.dispose(); }
        return image;
    }
    private static int differences(BufferedImage a, BufferedImage b, int x1, int y1, int x2, int y2) {
        int count = 0; for (int y = y1; y < y2; y += 2) for (int x = x1; x < x2; x += 2)
            if (a.getRGB(x, y) != b.getRGB(x, y)) count++;
        return count;
    }
    public static void main(String[] args) throws Exception {
        for (String name : List.of("zack", "bogart", "charice", "angel")) {
            var url = BattlePanel.class.getResource("/assets/sprites/fallen-" + name + "-v1.png");
            check(url != null, "packaged fallen sprite " + name);
            BufferedImage art = ImageIO.read(url); check(art.getColorModel().hasAlpha(), "transparent " + name);
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress progress = new GameProgress(List.of(0, 1, 2, 3));
                BattlePanel panel = new BattlePanel(() -> {}); panel.start(progress, false, 0);
                var sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                JPanel scene = (JPanel) sceneField.get(panel);
                CampPanel camp = new CampPanel(p -> {}, () -> {});
                Method draw = CampPanel.class.getDeclaredMethod("drawCharacter", Graphics2D.class, Student.class, int.class);
                draw.setAccessible(true);
                for (Student student : progress.getParty()) {
                    BufferedImage active = paint(scene);
                    BufferedImage activeCamp = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = activeCamp.createGraphics(); draw.invoke(camp, g, student, 480); g.dispose();
                    student.takeDamage(9999); check(!student.isAlive(), "KO " + student.getName());
                    BufferedImage down = paint(scene);
                    BufferedImage downCamp = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
                    g = downCamp.createGraphics(); draw.invoke(camp, g, student, 480); g.dispose();
                    check(differences(active, down, 0, 180, 380, 430) > 50, "fallen battle pose " + student.getName());
                    check(differences(activeCamp, downCamp, 350, 200, 600, 450) > 50, "fallen camp pose " + student.getName());
                    student.heal(1);
                    check(differences(activeCamp, drawAgain(draw, camp, student), 350, 200, 600, 450) == 0,
                            "standing pose returns when HP is restored " + student.getName());
                }
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Fallen sprite checks passed");
    }
    private static BufferedImage drawAgain(Method draw, CampPanel camp, Student student) throws ReflectiveOperationException {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); try { draw.invoke(camp, g, student, 480); } finally { g.dispose(); }
        return image;
    }
}
