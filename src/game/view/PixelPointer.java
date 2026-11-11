package game.view;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Pixel pointer based on the supplied reference image. */
final class PixelPointer {
    static final BufferedImage ART = loadArt();
    static final ImageIcon HAND = createHand();
    static final ImageIcon EMPTY = new ImageIcon(new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB));
    private PixelPointer() { }

    private static BufferedImage loadArt() {
        try {
            BufferedImage source = ImageIO.read(PixelPointer.class.getResource("/assets/ui/hand-pointer-reference.png"));
            removeBackground(source);
            return source.getSubimage(115, 98, 310, 232);
        } catch (Exception error) {
            return new BufferedImage(310, 232, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private static ImageIcon createHand() {
        BufferedImage icon = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(ART, 0, 0, 32, 24, null);
        } finally { g.dispose(); }
        return new ImageIcon(icon);
    }

    /** Remove edge-connected white pixels without erasing the enclosed white glove. */
    private static void removeBackground(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(0); visited[0] = true;
        while (!queue.isEmpty()) {
            int index = queue.removeFirst(), x = index % width, y = index / width;
            int pixel = image.getRGB(x, y);
            int red = (pixel >>> 16) & 255, green = (pixel >>> 8) & 255, blue = pixel & 255;
            if (red < 245 || green < 245 || blue < 245) { continue; }
            image.setRGB(x, y, 0);
            if (x > 0) { enqueue(index - 1, visited, queue); }
            if (x + 1 < width) { enqueue(index + 1, visited, queue); }
            if (y > 0) { enqueue(index - width, visited, queue); }
            if (y + 1 < height) { enqueue(index + width, visited, queue); }
        }
    }

    private static void enqueue(int index, boolean[] visited, ArrayDeque<Integer> queue) {
        if (!visited[index]) { visited[index] = true; queue.addLast(index); }
    }
}
