package game.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
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
        this(window, startNewGame, openLoad, null);
    }
    /** Creates a main menu including an optional settings action. */
    public MainMenuPanel(Window window, Runnable startNewGame, Runnable openLoad, Runnable openSettings) {
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
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 16, 44, 16));
        JPanel menu = new JPanel(new GridLayout(openSettings == null ? 3 : 4, 1, 0, 8)) {
            @Override public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(440, size.height);
            }
        };
        menu.setOpaque(true);
        menu.setBackground(new Color(15, 25, 48, 230));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 3),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JButton newGame = menuButton("New Game");
        JButton loadGame = menuButton("Load Game");
        JButton settings = menuButton("Settings");
        JButton quit = menuButton("Quit");
        newGame.addActionListener(event -> startNewGame.run());
        loadGame.addActionListener(event -> openLoad.run());
        if (openSettings != null) { settings.addActionListener(event -> openSettings.run()); }
        quit.addActionListener(event -> window.dispose());
        menu.add(newGame);
        menu.add(loadGame);
        if (openSettings != null) { menu.add(settings); }
        menu.add(quit);
        JButton[] buttons = openSettings == null ? new JButton[]{newGame, loadGame, quit} : new JButton[]{newGame, loadGame, settings, quit};
        Runnable refresh = () -> {
            String[] labels = openSettings == null ? new String[]{"New Game", "Load Game", "Quit"} : new String[]{"New Game", "Load Game", "Settings", "Quit"};
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setText(labels[i]);
                buttons[i].putClientProperty("menu.selected", i == cursor);
                buttons[i].repaint();
            }
        };
        Keys.bind(this, "UP", "menu-up", () -> { cursor = Math.floorMod(cursor - 1, buttons.length); refresh.run(); });
        Keys.bind(this, "DOWN", "menu-down", () -> { cursor = (cursor + 1) % buttons.length; refresh.run(); });
        Keys.bind(this, "ENTER", "menu-confirm", () -> buttons[cursor].doClick());
        Keys.bind(this, "SPACE", "menu-confirm-space", () -> buttons[cursor].doClick());
        refresh.run();
        JPanel menuHolder = new JPanel(new GridBagLayout());
        menuHolder.setOpaque(false);
        menuHolder.add(menu);
        bottom.add(menuHolder, BorderLayout.CENTER);
        JLabel subtitle = new JLabel("↑ ↓ MOVE   •   ENTER CONFIRM", SwingConstants.CENTER);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(ClassicUi.body(12));
        subtitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        bottom.add(subtitle, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton menuButton(String label) {
        JButton button = new JButton(label) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!Boolean.TRUE.equals(getClientProperty("menu.selected"))) { return; }
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    double scale = getFont().getSize2D() / 17.0;
                    int width = Math.max(1, (int) Math.round(PixelPointer.HAND.getIconWidth() * scale));
                    int height = Math.max(1, (int) Math.round(PixelPointer.HAND.getIconHeight() * scale));
                    int textWidth = g.getFontMetrics(getFont()).stringWidth(getText());
                    int x = (getWidth() - textWidth) / 2 - width - Math.max(2, (int) Math.round(4 * scale));
                    g.drawImage(PixelPointer.HAND.getImage(), x, (getHeight() - height) / 2, width, height, null);
                } finally { g.dispose(); }
            }
        };
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFocusable(false);
        button.setFont(ClassicUi.body(17));
        button.setForeground(Color.WHITE);
        button.setBackground(INK);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(107, 130, 172), 2),
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
