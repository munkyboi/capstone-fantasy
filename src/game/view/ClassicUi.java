package game.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

/** Shared, translucent classic RPG panels and the locally licensed display font. */
final class ClassicUi {
    private static final Font TITLE_FONT = loadFont("capstone.font", "EightBitDragon-anqx.ttf");
    private static final Font BODY_FONT = loadFont("capstone.bodyFont", "GomePixel-Regular.otf");
    private ClassicUi() { }

    private static Font loadFont(String property, String name) {
        String path = System.getProperty(property, new File(System.getProperty("user.home"),
                "Library/Fonts/" + name).getPath());
        try { return Font.createFont(Font.TRUETYPE_FONT, new File(path)); }
        catch (Exception unavailable) { return new Font(Font.MONOSPACED, Font.BOLD, 12); }
    }

    static Font title(int size) { return TITLE_FONT.deriveFont(Font.PLAIN, (float) size); }
    static Font body(int size) { return BODY_FONT.deriveFont(Font.PLAIN, (float) size); }
    // The Gome Pixel demo replaces digits with placeholders, so numbers need a legible font.
    static Font numbers(int size) { return new Font(Font.MONOSPACED, Font.BOLD, size); }

    static Border border() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(7, 12, 32), 2),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(241, 244, 255), 2),
                BorderFactory.createLineBorder(new Color(104, 132, 195), 2)));
    }

    static JPanel panel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setPaint(new GradientPaint(0, 0, new Color(35, 60, 176, 204),
                            0, Math.max(1, getHeight()), new Color(6, 18, 88, 204)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } finally { g.dispose(); }
            }
        };
        panel.setOpaque(false);
        panel.setBorder(border());
        return panel;
    }
}
