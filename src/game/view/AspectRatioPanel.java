package game.view;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;

/** Centers the game screens in a 16:9 area when the window is resized. */
public final class AspectRatioPanel extends JPanel {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private final JPanel screens;

    /** Wraps the game screens with a black letterbox.
     * @param screens game screen container
     */
    public AspectRatioPanel(JPanel screens) {
        this.screens = screens;
        setLayout(null);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(screens);
    }

    /** Positions the game area without distorting its aspect ratio. */
    @Override public void doLayout() {
        int width = getWidth();
        int height = (int) ((long) width * HEIGHT / WIDTH);
        if (height > getHeight()) {
            height = getHeight();
            width = (int) ((long) height * WIDTH / HEIGHT);
        }
        screens.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
        if (width > 0) { ViewportFonts.apply(screens, ViewportFonts.factor(width)); }
    }
}
