import game.model.GameProgress;
import game.model.Student;
import game.view.CampPanel;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Checks selection-driven camp depth layers. */
public final class CampParallaxTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static BufferedImage render(CampPanel panel, String name, double offset) throws Exception {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            Method draw = CampPanel.class.getDeclaredMethod(name, Graphics2D.class, double.class);
            draw.setAccessible(true); draw.invoke(panel, graphics, offset);
        } finally { graphics.dispose(); }
        return image;
    }
    private static int differences(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < 540; y += 2) for (int x = 0; x < 960; x += 2) if (a.getRGB(x, y) != b.getRGB(x, y)) count++;
        return count;
    }
    public static void main(String[] args) throws Exception {
        for (String resource : new String[]{"/assets/backgrounds/camp-sky-v1.png", "/assets/backgrounds/camp-distant-v1.png",
                "/assets/backgrounds/camp-courtyard-layer-v2.png", "/assets/backgrounds/camp-near-layer-v1.png", "/assets/sprites/camp-fire-v1.png"}) {
            var url = CampPanel.class.getResource(resource); check(url != null && ImageIO.read(url) != null, "asset " + resource);
        }
        CampPanel camp = new CampPanel(p -> {}, () -> {});
        check(differences(render(camp, "drawScenery", -25), render(camp, "drawScenery", 25)) > 500, "scenery follows camera");
        check(differences(render(camp, "drawForeground", -25), render(camp, "drawForeground", 25)) > 100, "foreground follows camera");
        BufferedImage character = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D characterGraphics = character.createGraphics();
        Method drawCharacter = CampPanel.class.getDeclaredMethod("drawCharacter", Graphics2D.class, Student.class, int.class);
        drawCharacter.setAccessible(true); drawCharacter.invoke(camp, characterGraphics, Student.create(Student.ZACK), 480);
        characterGraphics.dispose();
        BufferedImage fire = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fireGraphics = fire.createGraphics();
        Method drawFire = CampPanel.class.getDeclaredMethod("drawFire", Graphics2D.class, int.class);
        drawFire.setAccessible(true); drawFire.invoke(camp, fireGraphics, 0); fireGraphics.dispose();
        BufferedImage combined = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D combinedGraphics = combined.createGraphics(); combinedGraphics.drawImage(character, 0, 0, null);
        drawFire.invoke(camp, combinedGraphics, 0); combinedGraphics.dispose();
        int covered = 0;
        for (int y = 300; y < 447; y++) for (int x = 400; x < 560; x++) {
            if ((character.getRGB(x, y) >>> 24) > 0 && (fire.getRGB(x, y) >>> 24) > 32
                    && combined.getRGB(x, y) != character.getRGB(x, y)) covered++;
        }
        check(covered > 100, "fire draws in front of an overlapping character");
        BufferedImage shifted = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D shiftedGraphics = shifted.createGraphics(); drawFire.invoke(camp, shiftedGraphics, 15); shiftedGraphics.dispose();
        int matching = 0;
        for (int y = 300; y < 447; y++) for (int x = 400; x < 545; x++) {
            if ((fire.getRGB(x, y) >>> 24) > 0 && fire.getRGB(x, y) == shifted.getRGB(x + 15, y)) matching++;
        }
        check(matching > 100, "fire moves with the character layer offset");
        var current = CampPanel.class.getDeclaredField("cameraOffset"); current.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                camp.open(new GameProgress(List.of(0, 2, 3)), false);
                BufferedImage pixelScene = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
                Graphics2D pixelGraphics = pixelScene.createGraphics();
                Method drawPixelScene = CampPanel.class.getDeclaredMethod("drawPixelScene", Graphics2D.class);
                drawPixelScene.setAccessible(true); drawPixelScene.invoke(camp, pixelGraphics); pixelGraphics.dispose();
                for (int y = 100; y < 450; y += 10) for (int x = 100; x < 860; x += 10) {
                    int pixel = pixelScene.getRGB(x, y);
                    check(pixel == pixelScene.getRGB(x + 1, y) && pixel == pixelScene.getRGB(x, y + 1),
                            "scene and sprites scale as crisp pixel blocks");
                }
                double before = current.getDouble(camp);
                camp.getActionMap().get("camp-right").actionPerformed(new ActionEvent(camp, 0, ""));
                double after = current.getDouble(camp);
                check(before > after, "focus moves from Zack toward Charice");
                camp.getActionMap().get("camp-allocate").actionPerformed(new ActionEvent(camp, 0, ""));
                camp.getActionMap().get("camp-right").actionPerformed(new ActionEvent(camp, 0, ""));
                check(current.getDouble(camp) == after, "choosing stats does not move the camera");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Camp selection parallax checks passed");
    }
}
