package game.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

/** Renders the in-game interface at reduced resolution for a crisp pixel-art look. */
public final class PixelShell extends LayerUI<JPanel> {
    private static final double PIXEL_SCALE = 1.25;
    private BufferedImage pixels;

    /** Wraps the existing screens without changing their input handling.
     * @param screens interactive game screens
     * @return pixel-rendered game layer
     */
    public static JLayer<JPanel> wrap(JPanel screens) {
        return new JLayer<>(screens, new PixelShell());
    }

    /** Paints the game to a low-resolution buffer and enlarges it without smoothing.
     * @param graphics destination graphics
     * @param component game layer
     */
    @Override public void paint(Graphics graphics, JComponent component) {
        int width = component.getWidth(), height = component.getHeight();
        if (width <= 0 || height <= 0) { return; }
        int pixelWidth = Math.max(1, (int) Math.ceil(width / PIXEL_SCALE));
        int pixelHeight = Math.max(1, (int) Math.ceil(height / PIXEL_SCALE));
        if (pixels == null || pixels.getWidth() != pixelWidth || pixels.getHeight() != pixelHeight) {
            pixels = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D low = pixels.createGraphics();
        try {
            low.setComposite(java.awt.AlphaComposite.Clear);
            low.fillRect(0, 0, pixelWidth, pixelHeight);
            low.setComposite(java.awt.AlphaComposite.SrcOver);
            low.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            low.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            low.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            low.scale((double) pixelWidth / width, (double) pixelHeight / height);
            super.paint(low, component);
        } finally { low.dispose(); }
        Graphics2D output = (Graphics2D) graphics.create();
        try {
            output.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            output.drawImage(pixels, 0, 0, width, height, null);
        } finally { output.dispose(); }
    }
}
