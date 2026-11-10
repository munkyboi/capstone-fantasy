package game.view;

import game.model.Battle;
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
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Displays the campus entrance encounter and its controls. */
public final class BattlePanel extends JPanel {
    private final Runnable returnToMenu;
    private final JLabel status = new JLabel();
    private final JTextArea log = new JTextArea();
    private final JButton attack = new JButton("Attack");
    private final JButton ability = new JButton("Ability");
    private final JButton item = new JButton("Item");
    private final JButton defend = new JButton("Defend");
    private final JButton finish = new JButton("Return to Menu");
    private final Scene scene = new Scene();
    private Battle battle;

    /** Creates the battle screen.
     * @param returnToMenu returns to the title screen
     */
    public BattlePanel(Runnable returnToMenu) {
        this.returnToMenu = returnToMenu;
        setPreferredSize(new Dimension(960, 540));
        setLayout(new BorderLayout());
        add(scene, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(new Color(20, 29, 53));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        status.setForeground(new Color(250, 218, 143));
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        bottom.add(status, BorderLayout.NORTH);
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        log.setRows(3); log.setBackground(new Color(20, 29, 53));
        log.setForeground(Color.WHITE); log.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        bottom.add(log, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 5, 6, 0));
        actions.setOpaque(false);
        actions.add(attack); actions.add(ability); actions.add(item); actions.add(defend); actions.add(finish);
        bottom.add(actions, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        attack.addActionListener(e -> { battle.attack(); refresh(); });
        defend.addActionListener(e -> { battle.defend(); refresh(); });
        ability.addActionListener(e -> {
            Student target = battle.getCurrent().getId() == 3 ? chooseTarget("Healing Kiss") : null;
            if (battle.getCurrent().getId() != 3 || target != null) { battle.ability(target); refresh(); }
        });
        item.addActionListener(e -> {
            if (battle.getKits() == 0) {
                JOptionPane.showMessageDialog(this, "No First-Aid Kits remain."); return;
            }
            Student target = chooseTarget("First-Aid Kit");
            if (target != null) { battle.useKit(target); refresh(); }
        });
        finish.addActionListener(e -> this.returnToMenu.run());
    }

    /** Begins a fresh first encounter with the selected students.
     * @param ids selected student indices
     */
    public void start(List<Integer> ids) { battle = new Battle(ids, new Random()); refresh(); }

    private Student chooseTarget(String action) {
        List<Student> living = battle.getParty().stream().filter(Student::isAlive).toList();
        String[] names = living.stream().map(s -> s.getName() + " (" + s.getHp() + "/" + s.getMaxHp() + " HP)").toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(this, "Choose a student:", action,
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (choice == null) { return null; }
        for (int i = 0; i < names.length; i++) { if (names[i].equals(choice)) { return living.get(i); } }
        return null;
    }

    private void refresh() {
        boolean over = battle.isOver();
        status.setText(over ? (battle.isVictory() ? "VICTORY" : "GAME OVER")
                : battle.getCurrent().getName() + "'s turn  |  First-Aid Kits: " + battle.getKits());
        log.setText(battle.getMessage());
        attack.setEnabled(!over); ability.setEnabled(!over); item.setEnabled(!over); defend.setEnabled(!over);
        finish.setVisible(over);
        scene.repaint();
    }

    private static BufferedImage image(String path) {
        try { java.net.URL url = BattlePanel.class.getResource(path); return url == null ? null : ImageIO.read(url); }
        catch (IOException ex) { return null; }
    }

    private final class Scene extends JPanel {
        private final BufferedImage background = image("/assets/backgrounds/campus-courtyard-v1.png");
        private final BufferedImage sprites = image("/assets/sprites/student-party-idle-v1.png");
        private final BufferedImage wisp = image("/assets/sprites/library-wisp-v2.png");
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int w = getWidth(), h = getHeight();
                g.setColor(new Color(30, 43, 71)); g.fillRect(0, 0, w, h);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                if (background != null) { g.drawImage(background, 0, 0, w, h, null); }
                if (battle == null) { return; }
                int slotWidth = Math.max(80, w / 7);
                int spriteWidth = Math.max(60, slotWidth * 3 / 4);
                int spriteHeight = Math.max(85, h * 3 / 8);
                int i = 0;
                for (Student student : battle.getParty()) {
                    int x = 20 + i++ * (slotWidth + 8), y = h - 55 - spriteHeight;
                    if (sprites != null) {
                        int slice = sprites.getWidth() / 4, id = student.getId();
                        int left = x + (slotWidth - spriteWidth) / 2;
                        g.drawImage(sprites, left, y, left + spriteWidth, y + spriteHeight,
                                id * slice, 0, (id + 1) * slice, sprites.getHeight(), null);
                    }
                    g.setColor(new Color(15, 25, 47, 225)); g.fillRect(x, h - 46, slotWidth, 40);
                    g.setColor(student.isAlive() ? Color.WHITE : Color.LIGHT_GRAY);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                    g.drawString(student.getName(), x + 5, h - 29);
                    g.drawString(student.getHp() + " / " + student.getMaxHp() + " HP", x + 5, h - 12);
                }
                int x = w - slotWidth - 55;
                if (wisp != null && battle.getWispHp() > 0) {
                    g.drawImage(wisp, x + (slotWidth - spriteWidth) / 2, h - 55 - spriteHeight,
                            spriteWidth, spriteHeight, null);
                }
                g.setColor(new Color(15, 25, 47, 225)); g.fillRect(x - 10, h - 46, slotWidth + 20, 40);
                g.setColor(Color.WHITE); g.drawString("Library Wisp", x - 4, h - 29);
                g.drawString(battle.getWispHp() + " / " + battle.getWispMaxHp() + " HP", x - 4, h - 12);
            } finally { g.dispose(); }
        }
    }
}
