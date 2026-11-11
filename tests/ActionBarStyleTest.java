import game.view.BattlePanel;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** Checks the character AP bars retain their frame and correctly show progress. */
public final class ActionBarStyleTest {
    private static BufferedImage paint(JProgressBar bar) {
        bar.setSize(160, 16);
        BufferedImage image = new BufferedImage(160, 16, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); try { bar.paint(g); } finally { g.dispose(); }
        return image;
    }
    private static int coloredPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int p = image.getRGB(x, y), r = (p >>> 16) & 255, g = (p >>> 8) & 255;
            if (g > 80 && g > r * 1.3) { count++; }
        }
        return count;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> {}); panel.start(List.of(0, 1, 2));
                Field f = BattlePanel.class.getDeclaredField("apBars"); f.setAccessible(true);
                JProgressBar bar = ((JProgressBar[]) f.get(panel))[0];
                bar.setForeground(new java.awt.Color(30, 220, 30));
                bar.setValue(bar.getMaximum()); BufferedImage full = paint(bar);
                bar.setValue(bar.getMaximum() / 2); BufferedImage half = paint(bar);
                bar.setValue(0); BufferedImage empty = paint(bar);
                if (coloredPixels(full) <= coloredPixels(half) || coloredPixels(half) <= 0 || coloredPixels(empty) != 0) {
                    throw new AssertionError("AP fill must represent full, partial and empty values");
                }
                if ((full.getRGB(3, 2) >>> 24) == 0 || bar.isStringPainted()) {
                    throw new AssertionError("AP frame should be visible without text");
                }
                System.out.println("Action bar style checks passed");
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
    }
}
