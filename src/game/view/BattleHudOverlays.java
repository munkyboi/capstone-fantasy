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
        private int rewardRowCount() {
            return (int) state.rewardItems().stream().distinct().count() + (state.rewardPetot() > 0 ? 1 : 0)
                    + (state.rewardExperience() > 0 ? 1 : 0) + (state.leveledStudents().isEmpty() ? 0 : 1)
                    + (state.bonusXpStudents().isEmpty() ? 0 : 1);
        }
        int rewardButtonY() {
            int y = Math.max(18, hudHeight.getAsInt() / 5) + 52;
            int rows = rewardRowCount();
            int rowHeight = Math.max(12, Math.min(24, (hudHeight.getAsInt() - y - 97) / Math.max(1, rows)));
            return y + 53 + rows * rowHeight;
        }

        void drawRewards(Graphics2D g) {
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (String id : state.rewardItems()) { counts.merge(id, 1, Integer::sum); }
            java.util.List<String> items = new java.util.ArrayList<>(counts.keySet());
            int width = 360, x = (960 - width) / 2;
            int y = Math.max(18, hudHeight.getAsInt() / 5) + 52;
            int rows = items.size() + (state.rewardPetot() > 0 ? 1 : 0)
                    + (state.rewardExperience() > 0 ? 1 : 0) + (state.leveledStudents().isEmpty() ? 0 : 1)
                    + (state.bonusXpStudents().isEmpty() ? 0 : 1);
            int rowHeight = Math.max(12, Math.min(24, (hudHeight.getAsInt() - y - 97) / Math.max(1, rows)));
            int height = 82 + rows * rowHeight;
            g.setColor(new Color(12, 25, 48, 224));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(247, 204, 114));
            g.drawRect(x, y, width - 1, height - 1);
            g.drawRect(x + 3, y + 3, width - 7, height - 7);
            g.setFont(ClassicUi.body(17));
            g.drawString("You gain:", x + 16, y + 27);
            g.setFont(ClassicUi.numbers(Math.min(14, rowHeight - 2)));
            int offset = 0;
            if (state.rewardExperience() > 0) {
                int cy = y + 48;
                g.setColor(new Color(175, 133, 245)); g.fillPolygon(new int[] {x + 27, x + 35, x + 27, x + 19}, new int[] {cy - 10, cy - 2, cy + 6, cy - 2}, 4);
                g.setColor(Color.WHITE); g.drawString(state.rewardExperience() + " XP each", x + 50, cy + 4);
                offset++;
            }
            if (!state.bonusXpStudents().isEmpty()) {
                int cy = y + 48 + offset * rowHeight;
                g.setColor(Color.WHITE); g.drawString("Bonus XP: " + String.join(", ", state.bonusXpStudents()), x + 16, cy + 4);
                offset++;
            }
            if (state.rewardPetot() > 0) {
                int cy = y + 48 + offset * rowHeight;
                g.setColor(new Color(240, 189, 73)); g.fillOval(x + 18, cy - 10, 18, 18);
                g.setColor(new Color(255, 232, 154)); g.drawOval(x + 20, cy - 8, 14, 14);
                g.setColor(Color.WHITE); g.drawString(state.rewardPetot() + " Petot", x + 50, cy + 6);
                offset++;
            }
            for (int i = 0; i < items.size(); i++) {
                String id = items.get(i);
                int rowY = y + 36 + (i + offset) * rowHeight;
                drawRewardIcon(g, id, x + 16, rowY, Math.min(24, rowHeight));
                g.setColor(Color.WHITE);
                String name = "first_aid_kit".equals(id) ? "First-Aid Kit"
                        : "white_flower".equals(id) ? "White Flower" : Equipment.fromId(id).getName();
                g.drawString(counts.get(id) + "x " + name, x + 50, rowY + rowHeight - 3);
            }
            if (!state.leveledStudents().isEmpty()) {
                g.setColor(new Color(250, 218, 143));
                g.drawString("Level up: " + String.join(", ", state.leveledStudents()), x + 16,
                        y + 36 + (offset + items.size()) * rowHeight + rowHeight - 3);
            }
            int buttonY = y + height - 29;
            String[] labels = state.canExplore() ? new String[]{"Explore", "Camp"} : new String[]{"Camp"};
            for (int i = 0; i < labels.length; i++) {
                int choice = state.canExplore() ? i : 1;
                int bx = state.canExplore() ? x + 40 + i * 155 : x + 115;
                boolean selected = state.victoryChoice() == choice;
                g.setColor(selected ? new Color(250, 218, 143) : Color.WHITE);
                g.setFont(ClassicUi.numbers(16));
                g.drawString(labels[i], bx + 38, buttonY + 16);
                if (selected) g.drawImage(PixelPointer.ART, bx + 5, buttonY, PixelPointer.WIDTH, PixelPointer.HEIGHT, null);
            }
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
