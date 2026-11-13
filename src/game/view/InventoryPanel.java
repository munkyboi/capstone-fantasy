package game.view;

import game.model.Equipment;
import game.model.GameProgress;
import game.model.Student;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultHighlighter;

/** Keyboard-operated shared bag and party equipment screen. */
public final class InventoryPanel extends JPanel {
    private static final Color GOLD = new Color(250, 218, 143);
    private static final Color INK = new Color(12, 25, 48);
    private static final Color SELECTED = new Color(50, 78, 169);
    private static final Color LINE = new Color(134, 162, 192);
    private final Runnable back;
    private final ImageIcon[] portraits = CharacterPortraits.load();
    private final JLabel heading = new JLabel("INVENTORY & EQUIPMENT", SwingConstants.CENTER);
    private final JLabel help = new JLabel("", SwingConstants.CENTER);
    private final JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel[] partyRows = {new JLabel(), new JLabel(), new JLabel(), new JLabel()};
    private final JLabel profile = new JLabel();
    private final JLabel stats = new JLabel();
    private final JLabel[] slots = {new JLabel(), new JLabel(), new JLabel()};
    private final JTextArea bagContents = textArea();
    private final JTextArea details = choiceArea();
    private int choiceCursor = -1;
    private final JScrollPane bagScroll;
    private final JLabel choiceHeading = new JLabel("EQUIPMENT DETAILS");
    private final BufferedImage background = ScreenArt.prepareBackground("/assets/backgrounds/camp-courtyard-v1.png");
    private GameProgress progress;
    private int studentIndex;
    private int slotIndex;
    private int choiceIndex;
    private boolean choosing;
    private String message;

    /** Creates the equipment screen.
     * @param back returns to camp
     */
    public InventoryPanel(Runnable back) {
        this.back = back;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        heading.setFont(ClassicUi.title(27)); heading.setForeground(GOLD);
        heading.setOpaque(true); heading.setBackground(INK); heading.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 0)); center.setOpaque(false);
        JPanel partyCard = panel("PARTY"); partyCard.setPreferredSize(new Dimension(205, 300));
        ViewportFonts.scalePreferredSize(partyCard);
        JPanel partyList = new JPanel(new GridLayout(0, 1, 0, 6)); partyList.setOpaque(false);
        for (int i = 0; i < partyRows.length; i++) {
            partyRows[i] = row(); partyRows[i].setVerticalAlignment(SwingConstants.CENTER);
            partyList.add(partyRows[i]);
        }
        partyCard.add(partyList, BorderLayout.CENTER); center.add(partyCard, BorderLayout.WEST);

        JPanel equipmentCard = panel("EQUIPPED"); equipmentCard.setPreferredSize(new Dimension(300, 300));
        ViewportFonts.scalePreferredSize(equipmentCard);
        JPanel equipmentBody = new JPanel(new BorderLayout(0, 8)); equipmentBody.setOpaque(false);
        profile.setFont(ClassicUi.body(16)); profile.setForeground(GOLD); profile.setIconTextGap(8);
        stats.setFont(ClassicUi.numbers(13)); stats.setForeground(Color.WHITE);
        JPanel summary = new JPanel(new BorderLayout(0, 5)); summary.setOpaque(false);
        summary.add(profile, BorderLayout.CENTER); summary.add(stats, BorderLayout.SOUTH);
        equipmentBody.add(summary, BorderLayout.NORTH);
        JPanel slotList = new JPanel(new GridLayout(3, 1, 0, 6)); slotList.setOpaque(false);
        for (int i = 0; i < slots.length; i++) { slots[i] = row(); slotList.add(slots[i]); }
        equipmentBody.add(slotList, BorderLayout.CENTER); equipmentCard.add(equipmentBody, BorderLayout.CENTER);
        center.add(equipmentCard, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 10)); right.setOpaque(false);
        right.setPreferredSize(new Dimension(330, 300)); ViewportFonts.scalePreferredSize(right);
        JPanel bagCard = panel("SHARED BAG");
        bagScroll = scroll(bagContents); bagCard.add(bagScroll, BorderLayout.CENTER);
        JPanel choiceCard = panel(null); choiceHeading.setFont(ClassicUi.title(14));
        choiceHeading.setForeground(GOLD); choiceHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
        choiceCard.add(choiceHeading, BorderLayout.NORTH);
        JScrollPane choiceScroll = scroll(details);
        choiceScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        choiceCard.add(choiceScroll, BorderLayout.CENTER);
        right.add(bagCard); right.add(choiceCard); center.add(right, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 2)); footer.setOpaque(false);
        messageLabel.setFont(ClassicUi.body(13)); messageLabel.setForeground(GOLD);
        messageLabel.setOpaque(true); messageLabel.setBackground(INK);
        help.setFont(ClassicUi.body(13)); help.setForeground(Color.WHITE);
        help.setOpaque(true); help.setBackground(INK);
        footer.add(messageLabel); footer.add(help); add(footer, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "inventory-up", () -> move(-1));
        Keys.bind(this, "DOWN", "inventory-down", () -> move(1));
        Keys.bind(this, "LEFT", "inventory-left", () -> changeSlot(-1));
        Keys.bind(this, "RIGHT", "inventory-right", () -> changeSlot(1));
        Keys.bind(this, "ENTER", "inventory-confirm", this::confirm);
        Keys.bind(this, "SPACE", "inventory-confirm-space", this::confirm);
        Keys.bind(this, "ESCAPE", "inventory-back", this::cancel);
        Keys.bind(this, "PAGE_UP", "inventory-bag-up", () -> scrollBag(-1));
        Keys.bind(this, "PAGE_DOWN", "inventory-bag-down", () -> scrollBag(1));
    }
    private static JPanel panel(String title) {
        JPanel panel = ClassicUi.panel(); panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(ClassicUi.border(),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        if (title != null) {
            JLabel label = new JLabel(title); label.setFont(ClassicUi.title(14));
            label.setForeground(GOLD); label.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
            panel.add(label, BorderLayout.NORTH);
        }
        return panel;
    }
    private static JLabel row() {
        JLabel label = new JLabel(); label.setOpaque(true); label.setFont(ClassicUi.numbers(13));
        label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8)); return label;
    }
    private static JTextArea textArea() {
        JTextArea text = new JTextArea(); text.setEditable(false); text.setFocusable(false);
        text.setFont(ClassicUi.numbers(13)); text.setForeground(Color.WHITE);
        text.setOpaque(false); text.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return text;
    }
    private JTextArea choiceArea() {
        JTextArea area = new JTextArea() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (choiceCursor < 0) { return; }
                try {
                    java.awt.geom.Rectangle2D line = modelToView2D(choiceCursor);
                    if (line == null) { return; }
                    int width = Math.max(1, Math.round(PixelPointer.WIDTH * getFont().getSize2D() / 13));
                    int height = Math.max(1, Math.round(PixelPointer.HEIGHT * getFont().getSize2D() / 13));
                    graphics.drawImage(PixelPointer.HAND.getImage(), (int) line.getX(),
                            (int) line.getCenterY() - height / 2, width, height, null);
                } catch (javax.swing.text.BadLocationException ignored) { /* selection is being refreshed */ }
            }
        };
        area.setEditable(false); area.setFocusable(false); area.setOpaque(false);
        area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setFont(ClassicUi.numbers(13)); area.setForeground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return area;
    }
    private static JScrollPane scroll(JTextArea area) {
        JScrollPane pane = new JScrollPane(area); pane.setBorder(null); pane.setFocusable(false); pane.setOpaque(false); pane.getViewport().setOpaque(false);
        javax.swing.JScrollBar bar = pane.getVerticalScrollBar();
        bar.setFocusable(false); bar.setPreferredSize(new Dimension(10, 0));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                trackColor = new Color(18, 32, 54); thumbColor = new Color(93, 122, 154);
            }
            @Override protected javax.swing.JButton createDecreaseButton(int orientation) { return hiddenButton(); }
            @Override protected javax.swing.JButton createIncreaseButton(int orientation) { return hiddenButton(); }
            private javax.swing.JButton hiddenButton() {
                javax.swing.JButton button = new javax.swing.JButton();
                button.setPreferredSize(new Dimension(0, 0)); button.setFocusable(false); return button;
            }
        });
        return pane;
    }
    /** Opens inventory for the current party.
     * @param progress shared party state
     */
    public void open(GameProgress progress) {
        this.progress = progress; studentIndex = 0; slotIndex = 0; choiceIndex = 0; choosing = false;
        message = "Choose a student and slot."; refresh();
    }
    private Student student() { return progress.getParty().get(studentIndex); }
    private Equipment.Slot slot() { return Equipment.Slot.values()[slotIndex]; }
    private List<String> choices() {
        List<String> ids = new ArrayList<>(); ids.add(null);
        for (String id : progress.getInventory().getEquipmentIds()) {
            Equipment item = Equipment.fromId(id);
            if (item.getSlot() == slot() && item.canEquip(student().getId())) { ids.add(id); }
        }
        return ids;
    }
    private void move(int delta) {
        if (progress == null) { return; }
        if (choosing) { choiceIndex = Math.floorMod(choiceIndex + delta, choices().size()); }
        else { studentIndex = Math.floorMod(studentIndex + delta, progress.getParty().size()); }
        refresh();
    }
    private void scrollBag(int direction) {
        javax.swing.JScrollBar bar = bagScroll.getVerticalScrollBar();
        bar.setValue(bar.getValue() + direction * bar.getBlockIncrement(direction));
    }
    private void changeSlot(int delta) {
        if (progress == null || choosing) { return; }
        slotIndex = Math.floorMod(slotIndex + delta, Equipment.Slot.values().length);
        message = "Choose a student and slot."; refresh();
    }
    private void confirm() {
        if (progress == null) { return; }
        if (!choosing) { choosing = true; choiceIndex = 0; message = "Select equipment or Unequip."; }
        else {
            String itemId = choices().get(choiceIndex);
            boolean changed = progress.equip(student(), slot(), itemId);
            message = changed ? (itemId == null ? "Slot unequipped." : Equipment.fromId(itemId).getName() + " equipped.")
                    : "That item is unavailable.";
            choosing = false;
        }
        refresh();
    }
    private void cancel() {
        if (choosing) { choosing = false; message = "Equipment change cancelled."; refresh(); }
        else { back.run(); }
    }
    private static String name(String id) { return id == null ? "Empty" : Equipment.fromId(id).getName(); }
    private static String formatStat(double value) {
        return Long.toString((long) Math.ceil(value - 1e-9));
    }
    private static String formatDelta(double value) {
        return (value >= -0.000001 ? "+" : "") + formatStat(Math.abs(value) < 0.000001 ? 0 : value);
    }
    private static String role(int id) {
        return switch (id) { case Student.ZACK -> "Mage"; case Student.BOGART -> "Warrior";
            case Student.CHARICE -> "Hunter"; case Student.ANGEL -> "Monk"; default -> "Student"; };
    }
    private static void styleRow(JLabel row, boolean selected) {
        row.setBackground(selected ? SELECTED : new Color(12, 29, 106));
        row.setForeground(selected ? GOLD : Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? GOLD : new Color(51, 72, 96), selected ? 2 : 1),
                BorderFactory.createEmptyBorder(4, 7, 4, 7)));
    }
    private void refresh() {
        Student current = student();
        for (int i = 0; i < partyRows.length; i++) {
            partyRows[i].setVisible(i < progress.getParty().size());
            if (i >= progress.getParty().size()) { continue; }
            Student member = progress.getParty().get(i);
            ViewportFonts.setScaledIcon(partyRows[i], i == studentIndex ? PixelPointer.HAND : PixelPointer.EMPTY);
            partyRows[i].setText("<html>" + member.getName()
                    + "<br>&nbsp;&nbsp; HP " + member.getHp() + "/" + member.getMaxHp() + "<br>&nbsp;&nbsp; SP " + member.getSp() + "/" + member.getMaxSp() + "</html>");
            styleRow(partyRows[i], i == studentIndex);
        }
        ViewportFonts.setScaledIcon(profile, portraits[current.getId()]);
        profile.setText(current.getName() + "  •  " + role(current.getId()));
        stats.setText("<html>ATK " + current.getAttack() + " &nbsp; DEF " + current.getDefense() + " &nbsp; SPD " + formatStat(current.getEffectiveSpeed())
                + "<br>AGI " + formatStat(current.getEffectiveAgility()) + " &nbsp; ACC " + formatStat(current.getEffectiveAccuracy()) + "</html>");
        for (int i = 0; i < slots.length; i++) {
            Equipment.Slot gearSlot = Equipment.Slot.values()[i];
            ViewportFonts.setScaledIcon(slots[i], i == slotIndex ? PixelPointer.HAND : PixelPointer.EMPTY);
            slots[i].setText("<html>" + gearSlot.name()
                    + "<br>&nbsp;&nbsp; " + name(current.getEquipmentId(gearSlot)) + "</html>");
            styleRow(slots[i], i == slotIndex);
        }
        StringBuilder bag = new StringBuilder("First-Aid Kits  × ").append(progress.getKits())
                .append("\nWhite Flowers   × ").append(progress.getInventory().getWhiteFlowers())
                .append("\nCampus Pass  [key]\n\nGEAR IN BAG\n");
        List<String> items = progress.getInventory().getEquipmentIds();
        if (items.isEmpty()) { bag.append("  (none)\n"); }
        else { for (String id : items) { bag.append("  ").append(name(id)).append('\n'); } }
        bagContents.setText(bag.toString()); bagContents.setCaretPosition(0);

        choiceHeading.setText(choosing ? "CHOOSE " + slot().name() : "EQUIPMENT DETAILS");
        StringBuilder choiceText = new StringBuilder();
        int selectedStart = -1, selectedEnd = -1;
        if (choosing) {
            List<String> options = choices();
            for (int i = 0; i < options.size(); i++) {
                String id = options.get(i);
                if (i == choiceIndex) { selectedStart = choiceText.length(); }
                choiceText.append("    ").append(id == null ? "Unequip" : name(id)).append('\n');
                if (i == choiceIndex) { selectedEnd = choiceText.length() - 1; }
            }
            String selected = options.get(choiceIndex);
            String old = current.getEquipmentId(slot());
            int hpDelta = current.statWithEquipment(Student.Stat.HP, slot(), selected) - current.getMaxHp();
            int spDelta = current.maxSpWithEquipment(slot(), selected) - current.getMaxSp();
            int attackDelta = current.statWithEquipment(Student.Stat.ATK, slot(), selected) - current.getAttack();
            int defenseDelta = current.statWithEquipment(Student.Stat.DEF, slot(), selected) - current.getDefense();
            double speedDelta = Math.ceil(current.effectiveSpeedWithEquipment(slot(), selected) - 1e-9) - Math.ceil(current.getEffectiveSpeed() - 1e-9);
            double agilityDelta = Math.ceil(current.agilityWithEquipment(slot(), selected) - 1e-9) - Math.ceil(current.getEffectiveAgility() - 1e-9);
            double accuracyDelta = Math.ceil(current.accuracyWithEquipment(slot(), selected) - 1e-9) - Math.ceil(current.getEffectiveAccuracy() - 1e-9);
            choiceText.append("\nPreview:\n");
            choiceText.append(String.format("HP %+d   SP %+d\nATK %+d  DEF %+d\nSPD %s  AGI %s\nACC %s\n",
                    hpDelta, spDelta, attackDelta, defenseDelta, formatDelta(speedDelta), formatDelta(agilityDelta), formatDelta(accuracyDelta)));
            if (selected != null && !Equipment.fromId(selected).getSpecialDescription().isEmpty()) {
                choiceText.append(Equipment.fromId(selected).getSpecialDescription()).append('\n');
            }
        } else {
            String id = current.getEquipmentId(slot());
            choiceText.append(name(id)).append('\n');
            if (id != null) {
                Equipment item = Equipment.fromId(id);
                choiceText.append(String.format("ATK %+d   DEF %+d\n", item.getAttackBonus(), item.getDefenseBonus()));
                if (!item.getSpecialDescription().isEmpty()) choiceText.append(item.getSpecialDescription()).append('\n');
            }
            choiceText.append("\nEnter to change this slot.");
        }
        choiceCursor = selectedStart;
        details.setText(choiceText.toString());
        details.getHighlighter().removeAllHighlights();
        if (selectedStart >= 0) {
            try { details.getHighlighter().addHighlight(selectedStart, selectedEnd,
                    new DefaultHighlighter.DefaultHighlightPainter(SELECTED)); }
            catch (javax.swing.text.BadLocationException error) {
                throw new IllegalStateException("Could not highlight the selected item", error);
            }
        }
        details.setCaretPosition(selectedStart < 0 ? 0 : selectedStart);
        messageLabel.setText(message);
        help.setText(choosing ? "Up/Down Item  •  Enter Equip  •  Esc Cancel  •  PgUp/PgDn Bag"
                : "Up/Down Student  •  Left/Right Slot  •  Enter Change  •  PgUp/PgDn Bag  •  Esc Camp");
    }
    /** Draws the campus artwork behind the inventory.
     * @param graphics drawing context
     */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try { ScreenArt.drawBackground(g, background, getWidth(), getHeight(), 85); }
        finally { g.dispose(); }
    }
}
