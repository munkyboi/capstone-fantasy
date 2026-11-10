package game.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The title screen and entry point to the game's menu. */
public final class MainMenuPanel extends JPanel {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final Color GOLD = new Color(250, 218, 143);
    private static final Color INK = new Color(24, 30, 58);
    private int cursor;

    /** Creates the menu for the given game window.
     * @param window window to close when the player quits
     * @param startNewGame opens character selection
     */
    public MainMenuPanel(Window window, Runnable startNewGame) {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("CAPSTONE FANTASY", SwingConstants.CENTER);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 42));
        title.setForeground(GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(55, 10, 0, 10));
        add(title, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 260, 44, 260));
        JPanel menu = new JPanel(new GridLayout(3, 1, 0, 8));
        menu.setOpaque(false);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 3),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JButton newGame = menuButton("New Game");
        JButton loadGame = menuButton("Load Game");
        JButton quit = menuButton("Quit");
        newGame.addActionListener(event -> startNewGame.run());
        loadGame.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "Saving and loading will be added in a future update.", "Load Game",
                JOptionPane.INFORMATION_MESSAGE));
        quit.addActionListener(event -> window.dispose());
        menu.add(newGame);
        menu.add(loadGame);
        menu.add(quit);
        JButton[] buttons = {newGame, loadGame, quit};
        Runnable refresh = () -> {
            String[] labels = {"New Game", "Load Game", "Quit"};
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setText((i == cursor ? "▶  " : "     ") + labels[i]);
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
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        subtitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        bottom.add(subtitle, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton menuButton(String label) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setForeground(Color.WHITE);
        button.setBackground(INK);
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(107, 130, 172)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return button;
    }

    /** Draws a simple pixel-art campus and crystal behind the menu.
     * @param graphics drawing context
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.scale(getWidth() / 160.0, getHeight() / 120.0);
            g.setColor(new Color(35, 46, 86));
            g.fillRect(0, 0, 160, 120);
            g.setColor(new Color(59, 73, 112));
            g.fillRect(0, 40, 160, 29);
            g.setColor(new Color(42, 52, 78));
            g.fillRect(0, 64, 160, 56);
            g.setColor(new Color(71, 78, 98));
            g.fillRect(8, 43, 40, 28);
            g.fillRect(112, 43, 40, 28);
            g.setColor(new Color(98, 110, 130));
            for (int x = 12; x <= 140; x += 8) {
                if (x > 42 && x < 112) { continue; }
                g.fillRect(x, 48, 4, 6);
                g.fillRect(x, 59, 4, 6);
            }
            g.setColor(new Color(24, 34, 61));
            g.fillRect(65, 65, 30, 8);
            g.fillRect(70, 61, 20, 4);
            g.setColor(new Color(96, 219, 220));
            g.fillPolygon(new int[] {80, 88, 80, 72},
                    new int[] {32, 46, 62, 46}, 4);
            g.setColor(new Color(192, 255, 242));
            g.fillPolygon(new int[] {80, 84, 80, 77},
                    new int[] {34, 45, 58, 45}, 4);
        } finally {
            g.dispose();
        }
    }
}
