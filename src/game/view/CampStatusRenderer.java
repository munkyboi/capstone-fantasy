package game.view;

import game.model.Student;
import java.awt.Color;
import java.awt.Graphics2D;

/** Renders camp status and stat allocation overlays. */
final class CampStatusRenderer {
    private static final int WIDTH = 960;
    private static final Color HP_COLOR = new Color(224, 73, 79);
    private static final Color SP_COLOR = new Color(76, 155, 245);
    private static final Color XP_COLOR = new Color(250, 218, 75);
    private CampStatusRenderer() {}
    static void drawAllocation(Graphics2D g, Student student, int statCursor) {
        int x = 306, y = 205, w = 348, h = 245;
        g.setColor(new Color(8, 18, 54, 245)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(15)); g.drawString(student.getName() + "  •  Points: " + student.getUnspentStatPoints(), x + 15, y + 24);
        String[] options = {"HP +5", "SP +3", "ATK +1", "DEF +1", "SPD +1", "AGI +1", "ACC +1"};
        g.setFont(ClassicUi.numbers(14));
        for (int i = 0; i < options.length; i++) {
            int row = y + 45 + i * 25;
            g.setColor(i == statCursor ? new Color(250, 218, 143) : Color.WHITE);
            if (i == statCursor) { g.drawImage(PixelPointer.ART, x + 18, row - 12, PixelPointer.WIDTH, PixelPointer.HEIGHT, null); }
            g.drawString(options[i], x + 52, row + 2);
        }
    }
    private static String formatStat(double value) {
        return Long.toString((long) Math.ceil(value - 1e-9));
    }
    static void drawStatus(Graphics2D g, Student student, int center) {
        int x = Math.max(8, Math.min(WIDTH - 246, center - 123)), y = 57, w = 238, h = 174;
        g.setColor(new Color(8, 18, 54, 235)); g.fillRect(x, y, w, h);
        g.setColor(new Color(241, 213, 143)); g.drawRect(x, y, w, h); g.drawRect(x + 3, y + 3, w - 6, h - 6);
        g.setFont(ClassicUi.numbers(15)); g.setColor(new Color(250, 218, 143));
        g.drawString(student.getName() + " L" + student.getLevel() + " P:" + student.getUnspentStatPoints()
                + (student.isAlive() ? "" : " KO"), x + 12, y + 23);
        drawResourceBar(g, x, y + 39, w, "HP", student.getHp(), student.getMaxHp(), HP_COLOR);
        drawResourceBar(g, x, y + 63, w, "SP", student.getSp(), student.getMaxSp(), SP_COLOR);
        long requiredXp = student.getXpRequired();
        drawResourceBar(g, x, y + 87, w, "XP", requiredXp == 0 ? 1 : student.getXpProgress(),
                requiredXp == 0 ? 1 : requiredXp, XP_COLOR);
        g.setColor(new Color(120, 149, 182)); g.drawLine(x + 12, y + 109, x + w - 12, y + 109);
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        g.drawString("ATK " + student.getAttack(), x + 12, y + 131);
        g.drawString("DEF " + student.getDefense(), x + 87, y + 131);
        g.drawString("SPD " + formatStat(student.getEffectiveSpeed()), x + 162, y + 131);
        g.drawString("AGI " + formatStat(student.getEffectiveAgility()), x + 12, y + 151);
        g.drawString("ACC " + formatStat(student.getEffectiveAccuracy()), x + 87, y + 151);
    }
    private static void drawResourceBar(Graphics2D g, int x, int row, int width, String label,
            long value, long maximum, Color color) {
        g.setFont(ClassicUi.numbers(12)); g.setColor(Color.WHITE); g.drawString(label, x + 12, row + 9);
        g.setColor(new Color(38, 49, 76)); g.fillRect(x + 48, row, 113, 9);
        g.setColor(color); g.fillRect(x + 48, row, (int) (113L * value / Math.max(1, maximum)), 9);
        String text = label.equals("XP") && maximum == 1 ? "MAX" : value + "/" + maximum;
        if (label.equals("XP") && g.getFontMetrics().stringWidth(text) > width - 173) {
            text = compactXp(value) + "/" + compactXp(maximum);
        }
        g.setFont(ClassicUi.numbers(10)); g.setColor(Color.WHITE);
        if (g.getFontMetrics().stringWidth(text) > width - 173) { g.setFont(ClassicUi.numbers(8)); }
        g.drawString(text, x + width - 10 - g.getFontMetrics().stringWidth(text), row + 9);
    }
    private static String compactXp(long value) {
        if (value >= 1_000_000_000) return String.format(java.util.Locale.ROOT, "%.1fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (value >= 10_000) return String.format(java.util.Locale.ROOT, "%.1fK", value / 1_000.0);
        return Long.toString(value);
    }
}
