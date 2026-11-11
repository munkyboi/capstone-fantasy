package game.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The title screen and entry point to the game's menu. */
public final class MainMenuPanel extends JPanel {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final Color GOLD = new Color(250, 218, 143);
    private static final Color INK = new Color(24, 30, 58);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/title-campus-v1.png");
    private final BufferedImage logo = ScreenArt.load("/assets/ui/title-logo-v1.png");
    private int cursor;

    /** Creates the menu for the given game window.
     * @param window window to close when the player quits
     * @param startNewGame opens character selection
     */
    public MainMenuPanel(Window window, Runnable startNewGame) {
        this(window, startNewGame, () -> { });
    }

    /** Creates the main menu with a load action.
     * @param window window to close
     * @param startNewGame opens character selection
     * @param openLoad opens saved-game slots
     */
    public MainMenuPanel(Window window, Runnable startNewGame, Runnable openLoad) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setLayout(new BorderLayout());

        JPanel title = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    if (logo == null) {
                        g.setFont(ClassicUi.title(42));
                        g.setColor(GOLD);
                        String text = "CAPSTONE FANTASY";
                        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, 100);
                    } else {
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        double scale = Math.min(460.0 / logo.getWidth(), 205.0 / logo.getHeight());
                        int width = (int) Math.round(logo.getWidth() * scale);
                        int height = (int) Math.round(logo.getHeight() * scale);
                        g.drawImage(logo, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
                    }
                } finally { g.dispose(); }
            }
        };
        title.setOpaque(false);
        title.setPreferredSize(new Dimension(WIDTH, 220));
        title.getAccessibleContext().setAccessibleName("Capstone Fantasy");
        add(title, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 260, 44, 260));
        JPanel menu = new JPanel(new GridLayout(3, 1, 0, 8));
        menu.setOpaque(true);
        menu.setBackground(new Color(15, 25, 48, 230));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 3),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JButton newGame = menuButton("New Game");
        JButton loadGame = menuButton("Load Game");
        JButton quit = menuButton("Quit");
        newGame.addActionListener(event -> startNewGame.run());
        loadGame.addActionListener(event -> openLoad.run());
        quit.addActionListener(event -> window.dispose());
        menu.add(newGame);
        menu.add(loadGame);
        menu.add(quit);
        JButton[] buttons = {newGame, loadGame, quit};
        Runnable refresh = () -> {
            String[] labels = {"New Game", "Load Game", "Quit"};
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setText(labels[i]);
                ViewportFonts.setScaledIcon(buttons[i], i == cursor ? PixelPointer.HAND : PixelPointer.EMPTY);
            }
        };
        Keys.bind(this, "UP", "menu-up", () -> { cursor = Math.floorMod(cursor - 1, buttons.length); refresh.run(); });
        Keys.bind(this, "DOWN", "menu-down", () -> { cursor = (cursor + 1) % buttons.length; refresh.run(); });
        Keys.bind(this, "ENTER", "menu-confirm", () -> buttons[cursor].doClick());
        Keys.bind(this, "SPACE", "menu-confirm-space", () -> buttons[cursor].doClick());
        refresh.run();
        bottom.add(menu, BorderLayout.CENTER);
        JLabel subtitle = new JLabel("↑ ↓ MOVE   •   ENTER CONFIRM", SwingConstants.CENTER);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(ClassicUi.body(12));
        subtitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        bottom.add(subtitle, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton menuButton(String label) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.setFont(ClassicUi.body(17));
        button.setForeground(Color.WHITE);
        button.setBackground(INK);
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(107, 130, 172)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return button;
    }

    /** Draws the campus artwork behind the menu.
     * @param graphics drawing context
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            ScreenArt.drawBackground(g, background, getWidth(), getHeight(), 65);
        } finally {
            g.dispose();
        }
    }
}
