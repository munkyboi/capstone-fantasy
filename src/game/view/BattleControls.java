package game.view;

import game.model.Battle;
import game.model.Student;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicProgressBarUI;

/** Builds the battle command and party-status layout. */
final class BattleControls {
    private static final int AP_DISPLAY_SCALE = 100;
    private BattleControls() { }

    static void build(JPanel owner, BattleScene scene, JLabel status, JLabel[] partyStatus,
            JProgressBar[] apBars, JTextArea log, JScrollPane logScroll, JButton attack,
            JButton ability, JButton item, JButton defend, JButton finish) {
        owner.setPreferredSize(new Dimension(960, 540));
        owner.setFocusable(true);
        owner.setLayout(new BorderLayout());
        JPanel bottom = new JPanel(new BorderLayout(4, 2));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        status.setForeground(new Color(250, 218, 143));
        status.setFont(ClassicUi.numbers(13));
        status.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        JPanel statusFrame = translucent(new Color(8, 14, 38));
        statusFrame.setLayout(new BorderLayout());
        statusFrame.add(status, BorderLayout.CENTER);
        bottom.add(statusFrame, BorderLayout.NORTH);
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        log.setFocusable(false);
        log.setRows(1); log.setOpaque(false);
        log.setForeground(Color.WHITE); log.setFont(ClassicUi.numbers(13));
        log.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        JPanel info = frame();
        info.setLayout(new BorderLayout());
        logScroll.setBorder(null);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setFocusable(false);
        logScroll.getVerticalScrollBar().setFocusable(false);
        info.add(logScroll, BorderLayout.CENTER);
        JPanel members = new JPanel(new GridLayout(3, 1));
        members.setOpaque(false);
        for (int i = 0; i < partyStatus.length; i++) {
            JLabel member = partyStatus[i];
            member.setForeground(Color.WHITE);
            member.setFont(ClassicUi.numbers(13));
            member.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
            JProgressBar bar = apBars[i]; bar.setPreferredSize(new Dimension(160, 15));
            ViewportFonts.scalePreferredSize(bar);
            bar.setUI(new BasicProgressBarUI());
            bar.setBackground(new Color(15, 26, 61));
            bar.setStringPainted(true); bar.setFont(ClassicUi.numbers(11));
            row.add(member, BorderLayout.CENTER); row.add(bar, BorderLayout.EAST);
            members.add(row);
        }
        info.add(members, BorderLayout.SOUTH);
        bottom.add(info, BorderLayout.CENTER);
        JPanel actions = frame();
        actions.setLayout(new GridLayout(5, 1, 0, 1));
        actions.setPreferredSize(new Dimension(190, 160));
        ViewportFonts.scalePreferredSize(actions);
        for (JButton button : new JButton[] {attack, ability, item, defend, finish}) {
            button.setFocusable(false);
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(18, 39, 111));
            button.setFont(ClassicUi.body(13));
            button.setHorizontalAlignment(JButton.LEFT);
            button.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
        }
        actions.add(attack); actions.add(ability); actions.add(item); actions.add(defend); actions.add(finish);
        bottom.add(actions, BorderLayout.WEST);
        owner.add(new AspectRatioPanel(new BattleOverlay(scene, bottom)), BorderLayout.CENTER);
    }

    static void updateCommands(JButton[] commands, int commandIndex) {
        String[] names = {"Attack", "Ability", "Item", "Defend"};
        for (int i = 0; i < commands.length; i++) {
            commands[i].setText(names[i]);
            ViewportFonts.setScaledIcon(commands[i], i == commandIndex && commands[i].isEnabled() ? PixelPointer.HAND : PixelPointer.EMPTY);
        }
    }

    static void updateParty(Battle battle, JLabel[] partyStatus) {
        boolean over = battle.isOver();
        for (int i = 0; i < partyStatus.length; i++) {
            Student student = battle.getParty().get(i);
            partyStatus[i].setForeground(!over && student == battle.getCurrent()
                    ? new Color(250, 218, 143) : Color.WHITE);
            partyStatus[i].setText(String.format("%-9s  HP %3d / %3d%s",
                    student.getName(), student.getHp(), student.getMaxHp(), student.isAlive() ? "" : "  KO"));
        }
    }

    static void updateBars(Battle battle, JProgressBar[] apBars, Map<Student, Double> actionPoints) {
        for (int i = 0; i < apBars.length; i++) {
            Student student = battle.getParty().get(i);
            double value = actionPoints.get(student);
            apBars[i].setMaximum(student.getActionThreshold() * AP_DISPLAY_SCALE);
            apBars[i].setValue((int) (value * AP_DISPLAY_SCALE));
            float progress = (float) Math.max(0, Math.min(1, value / student.getActionThreshold()));
            apBars[i].setForeground(Color.getHSBColor(progress / 3.0f, 0.75f, 0.9f));
            apBars[i].setString(!student.isAlive() ? "KO" : (int) value + " / " + student.getActionThreshold() + " AP");
        }
    }

    private static JPanel translucent(Color color) {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                super.paintComponent(graphics);
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                try {
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 204));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } finally { g.dispose(); }
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel frame() {
        return ClassicUi.panel();
    }
}
