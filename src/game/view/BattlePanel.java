package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
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
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Displays the campus entrance encounter and its controls. */
public final class BattlePanel extends JPanel {
    private final Runnable returnToMenu;
    private final Consumer<GameProgress> onVictory;
    private final JLabel status = new JLabel();
    private final JLabel[] partyStatus = {new JLabel(), new JLabel(), new JLabel()};
    private final JTextArea log = new JTextArea();
    private final JScrollPane logScroll = new JScrollPane(log);
    private final JButton attack = new JButton("Attack");
    private final JButton ability = new JButton("Ability");
    private final JButton item = new JButton("Item");
    private final JButton defend = new JButton("Defend");
    private final JButton finish = new JButton("Return to Menu");
    private final Scene scene = new Scene();
    private Battle battle;
    private GameProgress progress;
    private int commandIndex;
    private final JButton[] commands = {attack, ability, item, defend};
    private enum TargetAction { NONE, ATTACK, ABILITY, ITEM }
    private TargetAction targeting = TargetAction.NONE;
    private int targetIndex;
    private String notice;

    /** Creates the battle screen.
     * @param returnToMenu returns to the title screen
     */
    public BattlePanel(Runnable returnToMenu) {
        this(returnToMenu, ignored -> returnToMenu.run());
    }

    /** Creates a battle screen with a victory transition.
     * @param returnToMenu returns after defeat
     * @param onVictory opens the next screen after victory
     */
    public BattlePanel(Runnable returnToMenu, Consumer<GameProgress> onVictory) {
        this.returnToMenu = returnToMenu;
        this.onVictory = onVictory;
        setPreferredSize(new Dimension(960, 540));
        setFocusable(true);
        setLayout(new BorderLayout());
        add(scene, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBackground(new Color(8, 14, 38));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        status.setForeground(new Color(250, 218, 143));
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        bottom.add(status, BorderLayout.NORTH);
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        log.setFocusable(false);
        log.setRows(2); log.setBackground(new Color(18, 39, 111));
        log.setForeground(Color.WHITE); log.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        log.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel info = frame();
        info.setLayout(new BorderLayout());
        logScroll.setBorder(null);
        logScroll.setFocusable(false);
        logScroll.getVerticalScrollBar().setFocusable(false);
        info.add(logScroll, BorderLayout.CENTER);
        JPanel members = new JPanel(new GridLayout(3, 1));
        members.setOpaque(false);
        for (JLabel member : partyStatus) {
            member.setForeground(Color.WHITE);
            member.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            member.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
            members.add(member);
        }
        info.add(members, BorderLayout.SOUTH);
        bottom.add(info, BorderLayout.CENTER);
        JPanel actions = frame();
        actions.setLayout(new GridLayout(5, 1, 0, 1));
        actions.setPreferredSize(new Dimension(190, 160));
        for (JButton button : new JButton[] {attack, ability, item, defend, finish}) {
            button.setFocusable(false);
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(18, 39, 111));
            button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            button.setHorizontalAlignment(JButton.LEFT);
            button.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 8));
            button.setOpaque(true);
        }
        actions.add(attack); actions.add(ability); actions.add(item); actions.add(defend); actions.add(finish);
        bottom.add(actions, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
        attack.addActionListener(e -> beginTargeting(TargetAction.ATTACK));
        defend.addActionListener(e -> { notice = null; battle.defend(); refresh(); });
        ability.addActionListener(e -> beginTargeting(TargetAction.ABILITY));
        item.addActionListener(e -> {
            if (battle.getKits() == 0) {
                notice = "No First-Aid Kits remain."; refresh(); return;
            }
            beginTargeting(TargetAction.ITEM);
        });
        finish.addActionListener(e -> { if (battle.isVictory()) { this.onVictory.accept(progress); } else { this.returnToMenu.run(); } });
        Keys.bind(this, "UP", "battle-up", () -> moveCursor(-1));
        Keys.bind(this, "DOWN", "battle-down", () -> moveCursor(1));
        Keys.bind(this, "LEFT", "battle-left", () -> moveTarget(-1));
        Keys.bind(this, "RIGHT", "battle-right", () -> moveTarget(1));
        Keys.bind(this, "ENTER", "battle-confirm", this::confirmCommand);
        Keys.bind(this, "SPACE", "battle-confirm-space", this::confirmCommand);
        Keys.bind(this, "ESCAPE", "battle-cancel", this::cancelTargeting);
        Keys.bind(this, "PAGE_DOWN", "log-down", () -> scrollLog(1));
        Keys.bind(this, "PAGE_UP", "log-up", () -> scrollLog(-1));
    }

    private static JPanel frame() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(18, 39, 111));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 239), 3),
                BorderFactory.createLineBorder(new Color(63, 100, 166), 2)));
        return panel;
    }

    private void scrollLog(int direction) {
        javax.swing.JScrollBar bar = logScroll.getVerticalScrollBar();
        bar.setValue(bar.getValue() + direction * bar.getBlockIncrement(direction));
    }

    /** Begins a fresh first encounter with the selected students.
     * @param ids selected student indices
     */
    public void start(List<Integer> ids) {
        start(new GameProgress(ids));
    }

    /** Begins an encounter with the existing party and supplies.
     * @param progress party and encounter progress
     */
    public void start(GameProgress progress) {
        this.progress = progress;
        battle = new Battle(progress, new Random()); commandIndex = 0;
        targeting = TargetAction.NONE; targetIndex = 0; notice = null; refresh();
    }

    private void moveCursor(int direction) {
        if (battle == null || battle.isOver()) { return; }
        if (targeting != TargetAction.NONE) { moveTarget(direction); return; }
        commandIndex = Math.floorMod(commandIndex + direction, commands.length);
        updateCommands();
    }

    private boolean targetsParty() {
        return targeting == TargetAction.ITEM
                || (targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 3);
    }

    private boolean targetsAllEnemies() {
        return targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 0;
    }

    private void beginTargeting(TargetAction action) {
        if (battle == null || battle.isOver() || targeting != TargetAction.NONE) { return; }
        notice = null; targeting = action; targetIndex = 0;
        if (targetsParty()) {
            while (!battle.getParty().get(targetIndex).isAlive()) { targetIndex++; }
        } else {
            while (!battle.getEnemies().get(targetIndex).isAlive()) { targetIndex++; }
        }
        refresh();
    }

    private void moveTarget(int direction) {
        if (battle == null || battle.isOver() || targeting == TargetAction.NONE || targetsAllEnemies()) { return; }
        int size = targetsParty() ? battle.getParty().size() : battle.getEnemies().size();
        do { targetIndex = Math.floorMod(targetIndex + direction, size); }
        while (targetsParty() ? !battle.getParty().get(targetIndex).isAlive() : !battle.getEnemies().get(targetIndex).isAlive());
        refresh();
    }

    private void cancelTargeting() {
        if (targeting != TargetAction.NONE) { targeting = TargetAction.NONE; notice = null; refresh(); }
    }

    private void confirmCommand() {
        if (battle == null) { return; }
        if (battle.isOver()) { finish.doClick(); }
        else if (targeting != TargetAction.NONE) {
            Student target = targetsParty() ? battle.getParty().get(targetIndex) : null;
            Enemy enemy = !targetsParty() ? battle.getEnemies().get(targetIndex) : null;
            TargetAction action = targeting;
            targeting = TargetAction.NONE;
            notice = null;
            if (action == TargetAction.ATTACK) { battle.attack(enemy); }
            else if (action == TargetAction.ABILITY) { battle.ability(target, enemy); }
            else if (!battle.useKit(target)) { notice = target.getName() + " already has full HP."; }
            refresh();
        }
        else { commands[commandIndex].doClick(); }
    }

    private void updateCommands() {
        String[] names = {"Attack", "Ability", "Item", "Defend"};
        for (int i = 0; i < commands.length; i++) {
            commands[i].setText((i == commandIndex ? "▶ " : "  ") + names[i]);
        }
    }

    private void refresh() {
        boolean over = battle.isOver();
        String hint = targeting == TargetAction.NONE ? "↑ ↓ Move  •  Enter Select"
                : targetsParty() ? "← → Choose ally  •  Enter Confirm  •  Esc Cancel"
                : targetsAllEnemies() ? "Enter Confirm  •  Esc Cancel" : "← → Choose enemy  •  Enter Confirm  •  Esc Cancel";
        status.setText(over ? (battle.isVictory() ? "VICTORY  •  Enter: Continue" : "GAME OVER  •  Enter: Return to Menu")
                : battle.getCurrent().getName() + "'s turn  |  First-Aid Kits: " + battle.getKits() + "  |  " + hint);
        String targetMessage = targeting == TargetAction.NONE ? null : targetsParty()
                ? "Choose " + battle.getParty().get(targetIndex).getName() + " as the target."
                : targeting == TargetAction.ABILITY && battle.getCurrent().getId() == 0
                ? "Arcane Burst targets all enemies." : "Choose " + battle.getEnemies().get(targetIndex).getName() + " as the target.";
        log.setText(targetMessage != null ? targetMessage : notice != null ? notice : battle.getMessage());
        log.setCaretPosition(0);
        for (int i = 0; i < partyStatus.length; i++) {
            Student student = battle.getParty().get(i);
            String marker = !over && student == battle.getCurrent() ? "▶ " : "  ";
            partyStatus[i].setText(String.format("%s%-9s  HP %3d / %3d%s", marker,
                    student.getName(), student.getHp(), student.getMaxHp(), student.isAlive() ? "" : "  KO"));
        }
        boolean canChoose = !over && targeting == TargetAction.NONE;
        attack.setEnabled(canChoose); ability.setEnabled(canChoose); item.setEnabled(canChoose); defend.setEnabled(canChoose);
        finish.setVisible(over);
        finish.setText(battle.isVictory() ? "Continue" : "Return to Menu");
        updateCommands();
        scene.repaint();
    }

    private static BufferedImage image(String path) {
        try { java.net.URL url = BattlePanel.class.getResource(path); return url == null ? null : ImageIO.read(url); }
        catch (IOException ex) { return null; }
    }

    private static void drawCover(Graphics2D g, BufferedImage image, int width, int height) {
        double scale = Math.max((double) width / image.getWidth(), (double) height / image.getHeight());
        int scaledWidth = (int) Math.ceil(image.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(image.getHeight() * scale);
        g.drawImage(image, (width - scaledWidth) / 2, (height - scaledHeight) / 2,
                scaledWidth, scaledHeight, null);
    }

    private static void drawFit(Graphics2D g, BufferedImage image, int sourceX, int sourceWidth,
            int x, int y, int width, int height) {
        double scale = Math.min((double) width / sourceWidth, (double) height / image.getHeight());
        int drawnWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int drawnHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int left = x + (width - drawnWidth) / 2;
        int top = y + height - drawnHeight;
        g.drawImage(image, left, top, left + drawnWidth, top + drawnHeight,
                sourceX, 0, sourceX + sourceWidth, image.getHeight(), null);
    }

    private final class Scene extends JPanel {
        private final BufferedImage background = image("/assets/backgrounds/campus-courtyard-v1.png");
        private final BufferedImage sprites = image("/assets/sprites/student-party-idle-v1.png");
        private final BufferedImage wisp = image("/assets/sprites/library-wisp-v2.png");
        private final BufferedImage sentinel = image("/assets/sprites/stone-sentinel-v1.png");
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getWidth() <= 0 || getHeight() <= 0) { return; }
            int virtualWidth = 320;
            int virtualHeight = Math.max(1, getHeight() * virtualWidth / getWidth());
            BufferedImage pixels = new BufferedImage(virtualWidth, virtualHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = pixels.createGraphics();
            try {
                int w = virtualWidth, h = virtualHeight;
                g.setColor(new Color(30, 43, 71)); g.fillRect(0, 0, w, h);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                if (background != null) { drawCover(g, background, w, h); }
                if (battle == null) { return; }
                int slotWidth = Math.max(27, w / 7);
                int spriteWidth = Math.max(20, slotWidth * 3 / 4);
                int spriteHeight = Math.max(28, h * 3 / 8);
                int i = 0;
                for (Student student : battle.getParty()) {
                    int index = i++;
                    int x = 10 + index * (slotWidth + 3), y = h - 10 - spriteHeight;
                    if (sprites != null) {
                        int slice = sprites.getWidth() / 4, id = student.getId();
                        int left = x + (slotWidth - spriteWidth) / 2;
                        drawFit(g, sprites, id * slice, slice, left, y, spriteWidth, spriteHeight);
                    }
                    if (targeting != TargetAction.NONE && targetsParty() && index == targetIndex) {
                        drawCursor(g, x + slotWidth / 2, y - 3);
                    }
                }
                for (int enemyIndex = 0; enemyIndex < battle.getEnemies().size(); enemyIndex++) {
                    Enemy enemy = battle.getEnemies().get(enemyIndex);
                    int x = w - 18 - (enemyIndex + 1) * (slotWidth + 3);
                    BufferedImage enemyImage = enemy.isSentinel() ? sentinel : wisp;
                    if (enemyImage != null && enemy.isAlive()) {
                        int enemyWidth = enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                        int enemyHeight = enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                        int enemyX = x + (slotWidth - enemyWidth) / 2;
                        int enemyY = h - 10 - enemyHeight;
                        drawFit(g, enemyImage, 0, enemyImage.getWidth(), enemyX, enemyY,
                                enemyWidth, enemyHeight);
                        if (targeting != TargetAction.NONE && !targetsParty()
                                && (targetsAllEnemies() || enemyIndex == targetIndex)) {
                            drawCursor(g, x + slotWidth / 2, enemyY - 3);
                        }
                    }
                }
            } finally { g.dispose(); }
            Graphics2D output = (Graphics2D) graphics.create();
            try {
                output.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                output.drawImage(pixels, 0, 0, getWidth(), getHeight(), null);
                output.setColor(new Color(15, 25, 47, 225));
                output.fillRect(getWidth() - 224, 12, 200, 52);
                output.setColor(Color.WHITE);
                output.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                for (int i = 0; i < battle.getEnemies().size(); i++) {
                    Enemy enemy = battle.getEnemies().get(i);
                    output.drawString((enemy.isSentinel() ? "Sentinel" : "Wisp " + (char) ('A' + i)) + "  " + enemy.getHp() + " / "
                            + enemy.getMaxHp() + " HP" + (enemy.isAlive() ? "" : " KO"), getWidth() - 217, 30 + i * 18);
                }
            } finally { output.dispose(); }
        }

        private void drawCursor(Graphics2D g, int x, int y) {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[] {x - 5, x + 5, x}, new int[] {y - 7, y - 7, y + 1}, 3);
            g.setColor(new Color(255, 232, 160));
            g.fillPolygon(new int[] {x - 4, x + 4, x}, new int[] {y - 6, y - 6, y}, 3);
        }
    }
}
