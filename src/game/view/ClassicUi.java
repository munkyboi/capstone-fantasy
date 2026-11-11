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
    private static final Font PIXEL_FONT = loadFont();
    private ClassicUi() { }

    private static Font loadFont() {
        String path = System.getProperty("capstone.font", new File(System.getProperty("user.home"),
                "Library/Fonts/EightBitDragon-anqx.ttf").getPath());
        File file = new File(path);
        try { return Font.createFont(Font.TRUETYPE_FONT, file); }
        catch (Exception unavailable) { return new Font(Font.MONOSPACED, Font.BOLD, 12); }
    }

    static Font font(int size) { return PIXEL_FONT.deriveFont(Font.PLAIN, (float) size); }

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
