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
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Lets the player choose three distinct students. */
public final class SelectionPanel extends JPanel {
    private static final int PARTY_SIZE = 3;
    private static final Color GOLD = new Color(246, 213, 139);
    private static final Color LIGHT = new Color(238, 242, 255);
    private static final String[] NAMES = {"Zack", "Bogart", "Charice", "Angel"};
    private static final String[] CLASSES = {"Mage", "Warrior", "Hunter", "Monk"};
    private static final String[] STATS = {
        "HP 80  ATK 18  DEF 6  SPD 10", "HP 120  ATK 15  DEF 14  SPD 7",
        "HP 90  ATK 14  DEF 8  SPD 16", "HP 105  ATK 10  DEF 10  SPD 9"
    };
    private static final String[] ABILITIES = {
        "Arcane Burst", "Cadet Strike", "Twin Shot", "Healing Kiss"
    };
    private static final String[] EFFECTS = {
        "Damages all enemies", "Strong sword attack", "Attacks twice", "Heals an ally"
    };
    private final Set<Integer> selected = new LinkedHashSet<>();
    private final JButton[] choices = new JButton[NAMES.length];
    private final JLabel count = new JLabel("", SwingConstants.CENTER);
    private final JButton continueButton = new JButton("Continue");
    private final BufferedImage background = loadImage("/assets/backgrounds/campus-courtyard-v1.png");
    private final BufferedImage sprites = loadImage("/assets/sprites/student-party-idle-v1.png");

    /** Creates the selection screen.
     * @param goBack returns to the main menu
     */
    public SelectionPanel(Runnable goBack) {
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel heading = new JLabel("CHOOSE YOUR PARTY", SwingConstants.CENTER);
        heading.setForeground(GOLD);
        heading.setFont(new Font(Font.SERIF, Font.BOLD, 30));
        add(heading, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        for (int i = 0; i < NAMES.length; i++) {
            final int index = i;
            choices[i] = new CharacterCard(i);
            choices[i].addActionListener(event -> toggle(index));
            cards.add(choices[i]);
        }
        add(cards, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setOpaque(false);
        count.setForeground(LIGHT);
        count.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        footer.add(count, BorderLayout.CENTER);
        JButton back = new JButton("Back");
        back.addActionListener(event -> goBack.run());
        continueButton.addActionListener(event -> JOptionPane.showMessageDialog(this,
                "Your party: " + partyNames() + "\nThe first battle is coming soon.",
                "Party ready", JOptionPane.INFORMATION_MESSAGE));
        footer.add(back, BorderLayout.WEST);
        footer.add(continueButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        reset();
    }

    /** Clears the party when a new game starts. */
    public void reset() {
        selected.clear();
        refresh();
    }

    private void toggle(int index) {
        if (selected.contains(index)) {
            selected.remove(index);
        } else if (selected.size() < PARTY_SIZE) {
            selected.add(index);
        }
        refresh();
    }

    private void refresh() {
        count.setText("Choose 3 students  •  " + selected.size() + " / " + PARTY_SIZE);
        continueButton.setEnabled(selected.size() == PARTY_SIZE);
        for (int i = 0; i < choices.length; i++) {
            choices[i].getAccessibleContext().setAccessibleName(NAMES[i] + ", " + CLASSES[i]
                    + (selected.contains(i) ? ", selected" : ", not selected"));
            choices[i].repaint();
        }
    }

    private String partyNames() {
        StringBuilder names = new StringBuilder();
        for (int index : selected) {
            if (names.length() > 0) { names.append(", "); }
            names.append(NAMES[index]);
        }
        return names.toString();
    }

    private static BufferedImage loadImage(String path) {
        try {
            java.net.URL resource = SelectionPanel.class.getResource(path);
            return resource == null ? null : ImageIO.read(resource);
        } catch (IOException ex) {
            return null;
        }
    }

    /** Paints the adapted campus background.
     * @param graphics drawing context
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(new Color(26, 37, 68));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (background != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
            }
            g.setColor(new Color(10, 19, 40, 120));
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally { g.dispose(); }
    }

    private final class CharacterCard extends JButton {
        private final int index;

        CharacterCard(int index) {
            this.index = index;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(true);
            setToolTipText("Select or deselect " + NAMES[index]);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int w = getWidth(), h = getHeight();
                g.setColor(new Color(17, 28, 52, 224));
                g.fillRect(0, 0, w, h);
                g.setColor(selected.contains(index) ? GOLD : new Color(120, 149, 182));
                g.drawRect(1, 1, w - 3, h - 3);
                g.drawRect(3, 3, w - 7, h - 7);
                if (sprites != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int slice = sprites.getWidth() / 4;
                    int top = 60, bottom = Math.max(top + 40, h - 135);
                    g.drawImage(sprites, 8, top, w - 8, bottom,
                            index * slice, 0, (index + 1) * slice, sprites.getHeight(), null);
                }
                g.setColor(GOLD);
                g.setFont(new Font(Font.SERIF, Font.BOLD, 21));
                drawCentered(g, NAMES[index], w / 2, 30);
                g.setColor(LIGHT);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                drawCentered(g, CLASSES[index], w / 2, 49);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                drawCentered(g, STATS[index], w / 2, h - 102);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                drawCentered(g, ABILITIES[index], w / 2, h - 72);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                drawCentered(g, EFFECTS[index], w / 2, h - 53);
                g.setColor(selected.contains(index) ? GOLD : LIGHT);
                drawCentered(g, selected.contains(index) ? "SELECTED" : "Select", w / 2, h - 32);
                if (hasFocus()) { g.drawRect(6, 6, w - 13, h - 13); }
            } finally { g.dispose(); }
        }

        private void drawCentered(Graphics2D g, String text, int center, int y) {
            g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, y);
        }
    }
}
