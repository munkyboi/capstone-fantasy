package game.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Loads and renders the title and camp artwork with crisp pixel scaling. */
final class ScreenArt {
    private static final int PIXEL_WIDTH = 320;
    private static final int PIXEL_HEIGHT = 180;
    private ScreenArt() { }

    static BufferedImage load(String path) {
        try {
            java.net.URL resource = ScreenArt.class.getResource(path);
            return resource == null ? null : ImageIO.read(resource);
        } catch (IOException ex) {
            return null;
        }
    }

    static BufferedImage prepareBackground(String path) {
        BufferedImage source = load(path);
        if (source == null) { return null; }
        BufferedImage pixels = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = pixels.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double scale = Math.max((double) PIXEL_WIDTH / source.getWidth(), (double) PIXEL_HEIGHT / source.getHeight());
            int width = (int) Math.ceil(source.getWidth() * scale);
            int height = (int) Math.ceil(source.getHeight() * scale);
            g.drawImage(source, (PIXEL_WIDTH - width) / 2, (PIXEL_HEIGHT - height) / 2, width, height, null);
        } finally { g.dispose(); }
        return pixels;
    }

    static void drawBackground(Graphics2D graphics, BufferedImage background, int width, int height, int tint) {
        graphics.setColor(new Color(18, 31, 62));
        graphics.fillRect(0, 0, width, height);
        if (background != null && width > 0 && height > 0) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double scale = Math.max((double) width / PIXEL_WIDTH, (double) height / PIXEL_HEIGHT);
            int targetWidth = (int) Math.ceil(PIXEL_WIDTH * scale);
            int targetHeight = (int) Math.ceil(PIXEL_HEIGHT * scale);
            graphics.drawImage(background, (width - targetWidth) / 2, (height - targetHeight) / 2,
                    targetWidth, targetHeight, null);
        }
        graphics.setColor(new Color(8, 18, 38, tint));
        graphics.fillRect(0, 0, width, height);
    }
}
