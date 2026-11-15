package game.view;

import game.model.Battle;
import game.model.Equipment;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.IntSupplier;

/** Draws battle alerts, item choices and the victory reward panel. */
final class BattleHudOverlays {
    private final BattleScene.State state;
    private final IntSupplier hudHeight;
    private final BufferedImage uniformIcon;
    private final BufferedImage[] rewardArt;

    BattleHudOverlays(BattleScene.State state, IntSupplier hudHeight, BufferedImage uniformIcon, BufferedImage[] rewardArt) {
        this.state = state;
        this.hudHeight = hudHeight;
        this.uniformIcon = uniformIcon;
        this.rewardArt = rewardArt;
    }
        private int rewardEntryCount() {
            return (int) state.rewardItems().stream().distinct().count()
                    + (state.rewardPetot() > 0 ? 1 : 0) + (state.rewardExperience() > 0 ? 1 : 0);
        }
        private int rewardRowCount() {
            return (rewardEntryCount() + 1) / 2 + (state.leveledStudents().isEmpty() ? 0 : 1)
                    + (state.bonusXpStudents().isEmpty() ? 0 : 1);
        }
        private int rewardTop() { return Math.max(18, hudHeight.getAsInt() / 5) + 52; }
        private int rewardRowHeight() {
            int available = (int) Math.floor(hudHeight.getAsInt() * 0.77) - rewardTop() - 82;
            return Math.max(9, Math.min(24, available / Math.max(1, rewardRowCount())));
        }
        int rewardButtonY() { return rewardTop() + 53 + rewardRowCount() * rewardRowHeight(); }

        void drawRewards(Graphics2D g) {
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (String id : state.rewardItems()) counts.merge(id, 1, Integer::sum);
            int width = 360, x = (960 - width) / 2, y = rewardTop();
            int rowHeight = rewardRowHeight(), height = 82 + rewardRowCount() * rowHeight;
            g.setColor(new Color(12, 25, 48, 224)); g.fillRect(x, y, width, height);
            g.setColor(new Color(247, 204, 114));
            g.drawRect(x, y, width - 1, height - 1); g.drawRect(x + 3, y + 3, width - 7, height - 7);
            g.setFont(ClassicUi.body(17)); g.drawString("You gain:", x + 16, y + 27);
            int entry = 0;
            if (state.rewardExperience() > 0) {
                int left = x + 16 + (entry % 2) * 170, top = y + 36 + (entry / 2) * rowHeight;
                int size = Math.min(18, rowHeight), cx = left + size / 2, cy = top + rowHeight / 2;
                int radius = Math.max(3, size / 2 - 1);
                g.setColor(new Color(175, 133, 245));
                g.fillPolygon(new int[] {cx, cx + radius, cx, cx - radius}, new int[] {cy - radius, cy, cy + radius, cy}, 4);
                drawRewardText(g, state.rewardExperience() + " XP each", left + 29, top, rowHeight, 137);
                entry++;
            }
            if (state.rewardPetot() > 0) {
                int left = x + 16 + (entry % 2) * 170, top = y + 36 + (entry / 2) * rowHeight;
                int size = Math.min(18, rowHeight), iconY = top + (rowHeight - size) / 2;
                g.setColor(new Color(240, 189, 73)); g.fillOval(left, iconY, size, size);
                g.setColor(new Color(255, 232, 154)); g.drawOval(left + 2, iconY + 2, Math.max(1, size - 4), Math.max(1, size - 4));
                drawRewardText(g, state.rewardPetot() + " Petot", left + 29, top, rowHeight, 137);
                entry++;
            }
            for (java.util.Map.Entry<String, Integer> reward : counts.entrySet()) {
                int left = x + 16 + (entry % 2) * 170, top = y + 36 + (entry / 2) * rowHeight;
                String id = reward.getKey();
                drawRewardIcon(g, id, left, top, Math.min(24, rowHeight));
                String name = "first_aid_kit".equals(id) ? "First-Aid Kit"
                        : "white_flower".equals(id) ? "White Flower" : Equipment.fromId(id).getName();
                drawRewardText(g, reward.getValue() + "x " + name, left + 29, top, rowHeight, 137,
                        "campus_vest".equals(id) || "campus_medal".equals(id) ? new Color(168, 230, 163) : Color.WHITE);
                entry++;
            }
            int row = (entry + 1) / 2;
            if (!state.bonusXpStudents().isEmpty()) {
                drawSummary(g, "Bonus XP: " + String.join(", ", state.bonusXpStudents()), x + 16, y + 36 + row++ * rowHeight, rowHeight);
            }
            if (!state.leveledStudents().isEmpty()) {
                drawSummary(g, "Level up: " + String.join(", ", state.leveledStudents()), x + 16, y + 36 + row * rowHeight, rowHeight);
            }
            int buttonY = rewardButtonY();
            String[] labels = state.canExplore() ? new String[]{"Explore", "Camp"} : new String[]{"Camp"};
            for (int i = 0; i < labels.length; i++) {
                int choice = state.canExplore() ? i : 1;
                int bx = state.canExplore() ? x + 40 + i * 155 : x + 115;
                boolean selected = state.victoryChoice() == choice;
                g.setColor(selected ? new Color(250, 218, 143) : Color.WHITE);
                g.setFont(ClassicUi.numbers(16)); g.drawString(labels[i], bx + 38, buttonY + 16);
                if (selected) g.drawImage(PixelPointer.ART, bx + 5, buttonY, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
            }
        }

        private void drawRewardText(Graphics2D g, String text, int x, int y, int rowHeight, int width) {
            drawRewardText(g, text, x, y, rowHeight, width, Color.WHITE);
        }
        private void drawRewardText(Graphics2D g, String text, int x, int y, int rowHeight, int width, Color color) {
            int fontSize = Math.min(14, Math.max(7, rowHeight - 2));
            g.setFont(ClassicUi.numbers(fontSize));
            while (fontSize > 7 && g.getFontMetrics().stringWidth(text) > width) g.setFont(ClassicUi.numbers(--fontSize));
            g.setColor(color); g.drawString(text, x, y + rowHeight - 3);
        }

        private void drawSummary(Graphics2D g, String text, int x, int y, int rowHeight) {
            drawRewardText(g, text, x, y, rowHeight, 328);
        }

        private void drawRewardIcon(Graphics2D g, String id, int x, int y, int size) {
            if ("first_aid_kit".equals(id)) {
                g.setColor(new Color(242, 239, 220)); g.fillRect(x + 3, y + 5, size - 6, size - 10);
                g.setColor(new Color(202, 57, 65)); g.fillRect(x + size / 2 - 2, y + 7, 4, size - 14);
                g.fillRect(x + 7, y + size / 2 - 2, size - 14, 4); return;
            }
            if ("white_flower".equals(id)) {
                g.setColor(new Color(67, 142, 83)); g.fillRect(x + 11, y + 12, 3, 10); g.fillRect(x + 7, y + 16, 5, 3);
                g.setColor(new Color(245, 245, 235)); g.fillRect(x + 9, y + 2, 6, 13); g.fillRect(x + 5, y + 6, 14, 5);
                g.setColor(new Color(246, 207, 78)); g.fillRect(x + 10, y + 7, 4, 4); return;
            }
            if ("training_uniform".equals(id) && uniformIcon != null) {
                BattleScene.drawFit(g, uniformIcon, 0, uniformIcon.getWidth(), x, y, size, size);
                return;
            }
            int cell = switch (id) {
                case "scholar_staff" -> 1;
                case "cadet_blade" -> 2;
                case "hunter_crossbows" -> 3;
                case "champion_wraps" -> 4;
                default -> 0;
            };
            BufferedImage icon = rewardArt[cell];
            if (icon != null) { BattleScene.drawFit(g, icon, 0, icon.getWidth(), x, y, size, size); }
        }

        void drawItemPicker(Graphics2D g) {
            int width = 320, height = 105, x = (960 - width) / 2, y = Math.max(60, hudHeight.getAsInt() / 4);
            g.setColor(new Color(12, 25, 48, 240)); g.fillRect(x, y, width, height);
            g.setColor(new Color(247, 204, 114)); g.drawRect(x, y, width - 1, height - 1); g.drawRect(x + 3, y + 3, width - 7, height - 7);
            g.setFont(ClassicUi.numbers(16)); g.setColor(new Color(250, 218, 143)); g.drawString("Choose an item", x + 16, y + 24);
            String[] labels = {"First-Aid Kit  × " + state.kits(), "White Flower   × " + state.flowers()};
            for (int i = 0; i < labels.length; i++) {
                int row = y + 50 + i * 28;
                g.setColor(i == state.itemChoice() ? new Color(250, 218, 143) : Color.WHITE);
                if (i == state.itemChoice()) g.drawImage(PixelPointer.ART, x + 12, row - 16, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
                g.drawString(labels[i], x + 42, row);
            }
        }
        void drawAbilityAlert(Graphics2D g, Battle.AbilityAnnouncement announcement) {
            java.awt.Font plain = ClassicUi.body(17), bold = plain.deriveFont(java.awt.Font.BOLD);
            g.setFont(bold); int casterWidth = g.getFontMetrics().stringWidth(announcement.caster());
            int skillWidth = g.getFontMetrics().stringWidth(announcement.skill());
            g.setFont(plain); int usedWidth = g.getFontMetrics().stringWidth(" used ");
            int width = casterWidth + usedWidth + skillWidth + 28;
            int x = (960 - width) / 2, y = Math.max(8, hudHeight.getAsInt() / 5 - 50);
            g.setColor(new Color(12, 25, 48, 224)); g.fillRect(x, y, width, 40);
            g.setColor(new Color(247, 204, 114)); g.drawRect(x, y, width - 1, 39); g.drawRect(x + 3, y + 3, width - 7, 33);
            g.setColor(Color.WHITE); g.setFont(bold); g.drawString(announcement.caster(), x + 14, y + 26);
            g.setFont(plain); g.drawString(" used ", x + 14 + casterWidth, y + 26);
            g.setFont(bold); g.drawString(announcement.skill(), x + 14 + casterWidth + usedWidth, y + 26);
        }
        void drawBattleAlert(Graphics2D g, String text) {
            g.setFont(ClassicUi.body(17));
            int width = g.getFontMetrics().stringWidth(text) + 28;
            int x = (960 - width) / 2;
            int y = Math.max(18, hudHeight.getAsInt() / 5);
            g.setColor(new Color(12, 25, 48, 204));
            g.fillRect(x, y, width, 42);
            g.setColor(new Color(247, 204, 114));
            g.drawRect(x, y, width - 1, 41);
            g.drawRect(x + 3, y + 3, width - 7, 35);
            g.setColor(Color.WHITE);
            g.drawString(text, x + 14, y + 27);
        }

}
