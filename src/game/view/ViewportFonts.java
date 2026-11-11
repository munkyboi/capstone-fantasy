package game.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

/** Scales in-game text and marked control dimensions with the 16:9 viewport. */
final class ViewportFonts {
    private static final String BASE_FONT = "viewport.baseFont";
    private static final String BASE_SIZE = "viewport.baseSize";
    private static final String BASE_ICON = "viewport.baseIcon";
    private static final String ICON_SOURCE = "viewport.iconSource";
    private static final String CURRENT_SCALE = "viewport.currentScale";
    private ViewportFonts() { }

    static double factor(int width) { return Math.max(0.5, width / 960.0); }
    static int pixels(int value, double scale) { return Math.max(1, (int) Math.round(value * scale)); }

    static void scalePreferredSize(JComponent component) {
        component.putClientProperty(BASE_SIZE, component.getPreferredSize());
    }

    static void setScaledIcon(JLabel label, ImageIcon icon) {
        label.putClientProperty(BASE_ICON, icon);
        Object current = label.getClientProperty(CURRENT_SCALE);
        updateIcon(label, current instanceof Double ? (Double) current : 1.0);
    }

    private static void updateIcon(JLabel label, double scale) {
        ImageIcon source = (ImageIcon) label.getClientProperty(BASE_ICON);
        if (source == null) { label.setIcon(null); return; }
        int width = pixels(source.getIconWidth(), scale), height = pixels(source.getIconHeight(), scale);
        if (label.getClientProperty(ICON_SOURCE) == source && label.getIcon() != null
                && label.getIcon().getIconWidth() == width && label.getIcon().getIconHeight() == height) { return; }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source.getImage(), 0, 0, width, height, null);
        } finally { graphics.dispose(); }
        label.putClientProperty(ICON_SOURCE, source);
        label.setIcon(new ImageIcon(image));
    }

    static void apply(Component component, double scale) {
        if (component instanceof JComponent) {
            JComponent ui = (JComponent) component;
            ui.putClientProperty(CURRENT_SCALE, scale);
            if (ui instanceof JLabel && ui.getClientProperty(BASE_ICON) != null) { updateIcon((JLabel) ui, scale); }
            if (ui instanceof JLabel || ui instanceof AbstractButton || ui instanceof JTextArea || ui instanceof JProgressBar) {
                Font base = (Font) ui.getClientProperty(BASE_FONT);
                if (base == null) { base = ui.getFont(); ui.putClientProperty(BASE_FONT, base); }
                if (base != null) {
                    float target = (float) Math.max(1.0, base.getSize2D() * scale);
                    if (Math.abs(ui.getFont().getSize2D() - target) > 0.1f) { ui.setFont(base.deriveFont(target)); }
                }
            }
            Dimension base = (Dimension) ui.getClientProperty(BASE_SIZE);
            if (base != null) {
                Dimension target = new Dimension(pixels(base.width, scale), pixels(base.height, scale));
                if (!target.equals(ui.getPreferredSize())) { ui.setPreferredSize(target); }
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) { apply(child, scale); }
        }
    }
}
