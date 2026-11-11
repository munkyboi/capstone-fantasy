import game.model.GameProgress;
import game.model.Student;
import game.view.CampPanel;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Checks that all party combinations have artwork and selection remains interactive. */
public final class CampSceneTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static BufferedImage render(CampPanel panel) {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { panel.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
    public static void main(String[] args) throws Exception {
        for (String name : List.of("zack", "bogart", "charice", "angel")) {
            var url = CampPanel.class.getResource("/assets/sprites/camp-" + name + "-v1.png");
            check(url != null, name + " camp sprite is packaged");
            BufferedImage sprite = ImageIO.read(url);
            check(sprite.getColorModel().hasAlpha(), name + " camp sprite has transparency");
        }
        check(CampPanel.class.getResource("/assets/backgrounds/camp-bonfire-v1.png") != null, "camp background packaged");
        SwingUtilities.invokeAndWait(() -> {
            for (int omitted = 0; omitted < 4; omitted++) {
                final int missing = omitted;
                List<Integer> ids = java.util.stream.IntStream.range(0, 4).filter(i -> i != missing).boxed().toList();
                GameProgress progress = new GameProgress(ids);
                CampPanel panel = new CampPanel(p -> {}, () -> {}); panel.setSize(960, 540); panel.open(progress, false);
                BufferedImage before = render(panel);
                panel.getActionMap().get("camp-right").actionPerformed(new ActionEvent(panel, 0, ""));
                BufferedImage after = render(panel);
                int changes = 0;
                for (int y = 55; y < 235; y += 3) for (int x = 0; x < 960; x += 3) {
                    if (before.getRGB(x, y) != after.getRGB(x, y)) { changes++; }
                }
                check(changes > 200, "selection moves the status popup for party " + ids);
                Student selected = progress.getParty().get(1); selected.takeDamage(20);
                panel.getActionMap().get("camp-use").actionPerformed(new ActionEvent(panel, 0, ""));
                check(selected.getHp() == selected.getMaxHp() && progress.getKits() == 2,
                        "kit heals the selected student for party " + ids);
            }
        });
        System.out.println("Camp scene and selection checks passed");
    }
}
