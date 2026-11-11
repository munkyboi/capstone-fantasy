import game.view.PixelShell;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Verifies pixel-shell rendering and access to the original interactive screen. */
public final class PixelShellTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    /** Runs pixel rendering checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel screen = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(Color.RED); g.fillRect(0, 0, getWidth() / 2, getHeight());
                    g.setColor(Color.BLUE); g.fillRect(getWidth() / 2, 0, getWidth(), getHeight());
                }
            };
            JLayer<JPanel> layer = PixelShell.wrap(screen);
            layer.setSize(120, 90); layer.doLayout();
            check(layer.getView() == screen && screen.getWidth() == 120, "original screen retains its layout and input target");
            BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try { layer.paint(graphics); } finally { graphics.dispose(); }
            check((image.getRGB(10, 20) & 0xffffff) == 0xff0000
                    && (image.getRGB(100, 20) & 0xffffff) == 0x0000ff, "screen content is rendered");
            layer.setSize(121, 91); layer.doLayout();
            BufferedImage resized = new BufferedImage(121, 91, BufferedImage.TYPE_INT_ARGB);
            graphics = resized.createGraphics();
            try { layer.paint(graphics); } finally { graphics.dispose(); }
            check((resized.getRGB(100, 20) & 0xffffff) == 0x0000ff, "render buffer adjusts after resize");
        });
        System.out.println("Pixel shell checks passed");
    }
}
