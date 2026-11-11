package game.view;

import java.awt.image.BufferedImage;

/** Shared, transparent knocked-out poses for the four students. */
final class FallenSprites {
    private static final String[] NAMES = {"zack", "bogart", "charice", "angel"};
    private static final BufferedImage[] ART = load();
    private FallenSprites() { }
    static BufferedImage forStudent(int id) { return ART[id]; }
    private static BufferedImage[] load() {
        BufferedImage[] images = new BufferedImage[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            BufferedImage image = ScreenArt.load("/assets/sprites/fallen-" + NAMES[i] + "-v1.png");
            if (image == null) { continue; }
            int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= 128) {
                    left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                }
            }
            if (right >= left) images[i] = image.getSubimage(left, top, right - left + 1, bottom - top + 1);
        }
        return images;
    }
}
