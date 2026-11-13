package game.view;

import game.model.CarbonMarket;
import game.model.GameProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Keyboard-operated Carbon Market. */
public final class ShopPanel extends JPanel {
    private final Runnable back;
    private final JLabel heading = new JLabel("CARBON MARKET", SwingConstants.CENTER);
    private final JLabel balance = new JLabel("", SwingConstants.CENTER);
    private final JLabel[] offers = {new JLabel(), new JLabel(), new JLabel()};
    private int firstVisible;
    private final JLabel message = new JLabel("", SwingConstants.CENTER);
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/carbon-market-battle-v2.png");
    private GameProgress progress;
    private int cursor;

    /** Creates a shop and a route return action. */
    public ShopPanel(Runnable back) {
        this.back = back; setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        setLayout(new BorderLayout(12, 12)); setBorder(BorderFactory.createEmptyBorder(26, 135, 26, 135));
        heading.setForeground(new Color(250, 218, 143)); heading.setFont(ClassicUi.title(30));
        balance.setForeground(Color.WHITE); balance.setFont(ClassicUi.numbers(17));
        JPanel top = new JPanel(new GridLayout(2, 1)); top.setOpaque(false); top.add(heading); top.add(balance); add(top, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 10)); rows.setOpaque(false);
        for (JLabel row : offers) {
            row.setOpaque(true); row.setBackground(new Color(12, 25, 48, 235));
            row.setFont(ClassicUi.numbers(16)); row.setForeground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(ClassicUi.border(), BorderFactory.createEmptyBorder(10, 16, 10, 16)));
            rows.add(row);
        }
        add(rows, BorderLayout.CENTER);
        JPanel footer = new JPanel(new GridLayout(2, 1)); footer.setOpaque(false);
        message.setForeground(new Color(250, 218, 143)); message.setFont(ClassicUi.numbers(14));
        JLabel help = new JLabel("Up/Down: Select   •   Enter: Buy   •   Esc: Back", SwingConstants.CENTER);
        help.setForeground(Color.WHITE); help.setFont(ClassicUi.body(14));
        footer.add(message); footer.add(help); add(footer, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "shop-up", () -> move(-1)); Keys.bind(this, "DOWN", "shop-down", () -> move(1));
        Keys.bind(this, "ENTER", "shop-buy", this::buy); Keys.bind(this, "SPACE", "shop-buy-space", this::buy);
        Keys.bind(this, "ESCAPE", "shop-back", back);
        // Give the active shop priority over window-wide Escape shortcuts on other screens.
        getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "shop-back");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "shop-back");
    }
    /** Opens the market for the supplied party. */
    public void open(GameProgress progress) { this.progress = progress; cursor = 0; firstVisible = 0; refresh("Welcome to Carbon Market!"); }
    private void move(int direction) { if (progress == null) { return; } cursor = Math.floorMod(cursor + direction, CarbonMarket.offers().size());
        if (cursor < firstVisible) firstVisible = cursor;
        if (cursor >= firstVisible + offers.length) firstVisible = cursor - offers.length + 1;
        refresh("Choose an item."); }
    private void buy() { if (progress != null) { refresh(CarbonMarket.buy(progress, cursor)); } }
    private void refresh(String text) {
        balance.setText("Petot: " + progress.getPetot());
        for (int i = 0; i < offers.length; i++) {
            int index = firstVisible + i;
            CarbonMarket.Offer item = CarbonMarket.offers().get(index);
            ViewportFonts.setScaledIcon(offers[i], index == cursor ? PixelPointer.HAND : PixelPointer.EMPTY);
            int spRegen = "first_aid_kit".equals(item.id()) ? 0 : game.model.Equipment.fromId(item.id()).getSpRegenPercent();
            String spText = spRegen == 0 || item.description().contains("restore " + spRegen + "%")
                    ? "" : "  •  AP fill: 25% chance to restore " + spRegen + "% SP.";
            offers[i].setText("<html>" + item.name() + "  •  " + item.price() + " Petot<br>" + item.description() + spText + "</html>");
            offers[i].setForeground(index == cursor ? new Color(250, 218, 143) : Color.WHITE);
        }
        message.setText(text); repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try { ScreenArt.drawBackground(g, background, getWidth(), getHeight(), 75); }
        finally { g.dispose(); }
    }
}
