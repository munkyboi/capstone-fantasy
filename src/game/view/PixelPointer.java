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
            if ((source.getRGB(0, 0) >>> 24) >= 128) { removeBackground(source); }
            return cropHand(source);
        } catch (Exception error) {
            return new BufferedImage(310, 232, BufferedImage.TYPE_INT_ARGB);
        }
    }

    /** Finds the largest visible shape, excluding a detached shadow or transparent margins. */
    private static BufferedImage cropHand(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int largest = 0, bestLeft = 0, bestTop = 0, bestRight = width - 1, bestBottom = height - 1;
        for (int start = 0; start < visited.length; start++) {
            if (visited[start] || (image.getRGB(start % width, start / width) >>> 24) < 128) { continue; }
            visited[start] = true; queue.add(start);
            int count = 0, left = width, right = 0, top = height, bottom = 0;
            while (!queue.isEmpty()) {
                int pixel = queue.removeFirst(), x = pixel % width, y = pixel / width;
                count++; left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height) { continue; }
                        int next = ny * width + nx;
                        if (!visited[next] && (image.getRGB(nx, ny) >>> 24) >= 128) {
                            visited[next] = true; queue.addLast(next);
                        }
                    }
                }
            }
            if (count > largest) {
                largest = count; bestLeft = left; bestRight = right; bestTop = top; bestBottom = bottom;
            }
        }
        return image.getSubimage(bestLeft, bestTop, bestRight - bestLeft + 1, bestBottom - bestTop + 1);
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
