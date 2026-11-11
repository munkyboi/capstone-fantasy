package game.view;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/** Creates small pixel-art portraits from the party's existing sprite sheet. */
final class CharacterPortraits {
    private static final int PORTRAIT_PIXELS = 28;
    private static final int DISPLAY_SIZE = 56;
    private static final int[] HEAD_CENTERS = {230, 245, 240, 230};
    private CharacterPortraits() { }

    static ImageIcon[] load() {
        ImageIcon[] portraits = new ImageIcon[HEAD_CENTERS.length];
        BufferedImage sheet = ScreenArt.load("/assets/sprites/student-party-idle-v1.png");
        if (sheet == null) { return portraits; }
        int slice = sheet.getWidth() / portraits.length;
        for (int i = 0; i < portraits.length; i++) {
            BufferedImage small = new BufferedImage(PORTRAIT_PIXELS, PORTRAIT_PIXELS, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pixels = small.createGraphics();
            try {
                pixels.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int left = i * slice + HEAD_CENTERS[i] - 150;
                pixels.drawImage(sheet, 0, 0, PORTRAIT_PIXELS, PORTRAIT_PIXELS,
                        left, 12, left + 300, 312, null);
            } finally { pixels.dispose(); }
            BufferedImage portrait = new BufferedImage(DISPLAY_SIZE, DISPLAY_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D scaled = portrait.createGraphics();
            try {
                scaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                scaled.drawImage(small, 0, 0, DISPLAY_SIZE, DISPLAY_SIZE, null);
            } finally { scaled.dispose(); }
            portraits[i] = new ImageIcon(portrait);
        }
        return portraits;
    }
}
