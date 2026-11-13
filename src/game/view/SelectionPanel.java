package game.view;

import game.model.Student;
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
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Lets the player choose three distinct students. */
public final class SelectionPanel extends JPanel {
    /** Serialized state: party size.
     * @serial
     */
    private int partySize = 3;
    private static final Color GOLD = new Color(246, 213, 139);
    private static final Color LIGHT = new Color(238, 242, 255);
    private static final String[] NAMES = {"Zack", "Bogart", "Charice", "Angel"};
    private static final String[] CLASSES = {"Mage", "Warrior", "Hunter", "Monk"};
    private static final String[] STAT_LABELS = {"HP", "SP", "ATK", "DEF", "SPD", "AGI", "ACC"};
    private static final Color[] STAT_COLORS = {
        new Color(224, 73, 79), new Color(76, 155, 245), new Color(246, 149, 62),
        new Color(192, 202, 215), new Color(240, 110, 178), new Color(91, 207, 177), new Color(198, 151, 231)
    };
    // The bars compare the four starting characters within each stat.
    private static final int[] STAT_MAXIMA = {150, 180, 25, 16, 19, 13, 12};
    private static final String[] ABILITIES = {
        "Arcane Burst", "Cadet Strike", "Rain of Arrows", "Angel Hug"
    };
    private static final String[] EFFECTS = {
        "Damages all enemies", "Strong sword attack", "12 arrows; may apply DOT", "Heals or revives an ally"
    };
    /** Serialized state: selected.
     * @serial
     */
    private final Set<Integer> selected = new LinkedHashSet<>();
    /** Serialized state: choices.
     * @serial
     */
    private final JButton[] choices = new JButton[NAMES.length];
    /** Serialized state: count.
     * @serial
     */
    private final JLabel count = new JLabel("", SwingConstants.CENTER);
    /** Serialized state: continue button.
     * @serial
     */
    private final JButton continueButton = new JButton("Continue");
    /** Serialized state: back button.
     * @serial
     */
    private final JButton backButton = new JButton("Back");
    /** Serialized state: cursor.
     * @serial
     */
    private int cursor;
    /** Serialized state: background.
     * @serial
     */
    private final BufferedImage background = loadImage("/assets/backgrounds/campus-courtyard-v1.png");
    /** Serialized state: sprites.
     * @serial
     */
    private final BufferedImage sprites = loadImage("/assets/sprites/student-party-idle-v1.png");

    /** Creates the selection screen.
     * @param goBack returns to the main menu
     * @param startBattle receives the selected student indices
     */
    public SelectionPanel(Runnable goBack, Consumer<java.util.List<Integer>> startBattle) {
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel heading = new JLabel("CHOOSE YOUR PARTY", SwingConstants.CENTER);
        heading.setForeground(GOLD);
        heading.setFont(ClassicUi.title(30));
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
        JPanel footer = new JPanel(new BorderLayout(12, 8)) {
            @Override protected void paintChildren(Graphics graphics) {
                super.paintChildren(graphics);
                if (cursor != 4 && cursor != 5) { return; }
                JButton target = cursor == 4 ? continueButton : backButton;
                int pointerWidth = ViewportFonts.pixels(PixelPointer.WIDTH, ViewportFonts.factor(SelectionPanel.this.getWidth()));
                int pointerHeight = ViewportFonts.pixels(PixelPointer.HEIGHT, ViewportFonts.factor(SelectionPanel.this.getWidth()));
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(PixelPointer.ART, target.getX() - pointerWidth - 3,
                            target.getY() + (target.getHeight() - pointerHeight) / 2, pointerWidth, pointerHeight, null);
                } finally { g.dispose(); }
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
        count.setForeground(LIGHT);
        count.setFont(ClassicUi.numbers(15));
        footer.add(count, BorderLayout.CENTER);
        backButton.setFocusable(false);
        continueButton.setFocusable(false);
        continueButton.setOpaque(true);
        continueButton.setContentAreaFilled(true);
        continueButton.setBorderPainted(true);
        backButton.addActionListener(event -> goBack.run());
        continueButton.addActionListener(event -> startBattle.accept(new java.util.ArrayList<>(selected)));
        footer.add(backButton, BorderLayout.WEST);
        footer.add(continueButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        Keys.bind(this, "LEFT", "party-left", () -> moveCursor(-1));
        Keys.bind(this, "RIGHT", "party-right", () -> moveCursor(1));
        Keys.bind(this, "DOWN", "party-continue", () -> { cursor = 4; refresh(); });
        Keys.bind(this, "UP", "party-cards", () -> { if (cursor >= 4) { cursor = 0; refresh(); } });
        Keys.bind(this, "ENTER", "party-confirm", this::confirmCursor);
        Keys.bind(this, "SPACE", "party-confirm-space", this::confirmCursor);
        Keys.bind(this, "ESCAPE", "party-back", goBack);
        reset();
    }

    /** Clears the party when a new game starts. */
    public void reset() {
        selected.clear();
        cursor = 0;
        refresh();
    }
    /** Configures how many students must be selected, optionally starting with an existing party.
     * @param size the size
     * @param initial the initial
     */
    public void configure(int size, java.util.List<Integer> initial) {
        if (size < 1 || size > 4) throw new IllegalArgumentException("Invalid party size");
        partySize = size; selected.clear(); selected.addAll(initial); cursor = 0; refresh();
    }

    private void moveCursor(int direction) {
        if (cursor >= 4) { cursor = cursor == 4 ? 5 : 4; }
        else { cursor = Math.floorMod(cursor + direction, choices.length); }
        refresh();
    }

    private void confirmCursor() {
        if (cursor == 4) { if (continueButton.isEnabled()) { continueButton.doClick(); } }
        else if (cursor == 5) { backButton.doClick(); }
        else { toggle(cursor); }
    }

    private void toggle(int index) {
        if (selected.contains(index)) {
            selected.remove(index);
        } else if (selected.size() < partySize) {
            selected.add(index);
        }
        refresh();
    }

    private void refresh() {
        count.setText("← → Select  •  Enter Choose  •  ↓ Buttons  •  " + selected.size() + " / " + partySize);
        continueButton.setEnabled(selected.size() == partySize);
        continueButton.setText("Continue");
        boolean focused = cursor == 4 && continueButton.isEnabled();
        continueButton.setForeground(focused ? new Color(20, 30, 55) : Color.WHITE);
        continueButton.setBackground(focused ? GOLD : new Color(27, 43, 75));
        continueButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(focused ? Color.WHITE : new Color(120, 149, 182), 2),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        for (int i = 0; i < choices.length; i++) {
            choices[i].getAccessibleContext().setAccessibleName(NAMES[i] + ", " + CLASSES[i]
                    + (selected.contains(i) ? ", selected" : ", not selected"));
            choices[i].repaint();
        }
        repaint();
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

    /** Draws and selects one student on the party selection screen. */
    private final class CharacterCard extends JButton {
        private final int index;

        CharacterCard(int index) {
            this.index = index;
            setFocusable(false);
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
                double scale = ViewportFonts.factor(SelectionPanel.this.getWidth());
                g.setColor(new Color(17, 28, 52, 224));
                g.fillRect(0, 0, w, h);
                g.setColor(index == cursor ? Color.WHITE : selected.contains(index) ? GOLD : new Color(120, 149, 182));
                g.drawRect(1, 1, w - 3, h - 3);
                g.drawRect(3, 3, w - 7, h - 7);
                if (sprites != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int slice = sprites.getWidth() / 4;
                    int top = ViewportFonts.pixels(60, scale), bottom = Math.max(top + ViewportFonts.pixels(40, scale), h - ViewportFonts.pixels(205, scale));
                    int height = Math.min(bottom - top, (w - ViewportFonts.pixels(16, scale)) * sprites.getHeight() / slice);
                    int width = height * slice / sprites.getHeight();
                    BufferedImage pixels = new BufferedImage(96, 128, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D pixelGraphics = pixels.createGraphics();
                    try {
                        pixelGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        pixelGraphics.drawImage(sprites, 0, 0, 96, 128,
                                index * slice, 0, (index + 1) * slice, sprites.getHeight(), null);
                    } finally { pixelGraphics.dispose(); }
                    int left = (w - width) / 2;
                    g.drawImage(pixels, left, top, width, height, null);
                    if (index == cursor) {
                        int size = ViewportFonts.pixels(PixelPointer.WIDTH, scale);
                        int pointerX = Math.max(ViewportFonts.pixels(6, scale), left - size / 2);
                        int pointerHeight = ViewportFonts.pixels(PixelPointer.HEIGHT, scale);
                        int pointerY = top + height / 2 - pointerHeight / 2;
                        g.drawImage(PixelPointer.ART, pointerX, pointerY, size, pointerHeight, null);
                    }
                }
                g.setColor(GOLD);
                g.setFont(ClassicUi.title(ViewportFonts.pixels(21, scale)));
                drawCentered(g, NAMES[index], w / 2, ViewportFonts.pixels(30, scale));
                g.setColor(LIGHT);
                g.setFont(ClassicUi.body(ViewportFonts.pixels(13, scale)));
                drawCentered(g, CLASSES[index], w / 2, ViewportFonts.pixels(49, scale));
                drawStats(g, w, h, scale);
                g.setFont(ClassicUi.body(ViewportFonts.pixels(13, scale)));
                drawCentered(g, ABILITIES[index], w / 2, h - ViewportFonts.pixels(48, scale));
                g.setFont(ClassicUi.body(ViewportFonts.pixels(11, scale)));
                drawCentered(g, EFFECTS[index], w / 2, h - ViewportFonts.pixels(31, scale));
                g.setFont(ClassicUi.numbers(ViewportFonts.pixels(10, scale)));
                drawCentered(g, "SP " + Student.create(index).getAbilitySpCost(), w / 2, h - ViewportFonts.pixels(20, scale));
                if (selected.contains(index)) {
                    g.setColor(GOLD);
                    drawCentered(g, "SELECTED", w / 2, h - ViewportFonts.pixels(7, scale));
                }
                if (hasFocus()) { g.drawRect(6, 6, w - 13, h - 13); }
            } finally { g.dispose(); }
        }

        private void drawStats(Graphics2D g, int width, int height, double scale) {
            Student student = Student.create(index);
            int[] values = {student.getMaxHp(), student.getMaxSp(), student.getBaseAttack(), student.getBaseDefense(), student.getSpeed(), student.getAgility(), student.getAccuracy()};
            int left = ViewportFonts.pixels(12, scale);
            int barLeft = ViewportFonts.pixels(48, scale);
            int valueRight = width - ViewportFonts.pixels(7, scale);
            int barWidth = Math.max(1, valueRight - barLeft - ViewportFonts.pixels(32, scale));
            int rowHeight = ViewportFonts.pixels(17, scale);
            int top = height - ViewportFonts.pixels(192, scale);
            int barHeight = ViewportFonts.pixels(7, scale);
            g.setFont(ClassicUi.numbers(ViewportFonts.pixels(10, scale)));
            for (int i = 0; i < STAT_LABELS.length; i++) {
                int y = top + i * rowHeight;
                g.setColor(LIGHT);
                g.drawString(STAT_LABELS[i], left, y + barHeight);
                g.setColor(new Color(38, 49, 76));
                g.fillRect(barLeft, y, barWidth, barHeight);
                if (i < values.length) {
                    g.setColor(STAT_COLORS[i]);
                    g.fillRect(barLeft, y, Math.max(1, barWidth * values[i] / STAT_MAXIMA[i]), barHeight);
                } else {
                    g.setColor(STAT_COLORS[i]);
                    g.drawRect(barLeft, y, Math.max(0, barWidth - 1), Math.max(0, barHeight - 1));
                }
                String value = i < values.length ? Integer.toString(values[i]) : "--";
                g.setColor(LIGHT);
                g.drawString(value, valueRight - g.getFontMetrics().stringWidth(value), y + barHeight);
            }
        }

        private void drawCentered(Graphics2D g, String text, int center, int y) {
            g.drawString(text, center - g.getFontMetrics().stringWidth(text) / 2, y);
        }
    }
}
