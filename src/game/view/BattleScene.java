package game.view;

import game.model.Battle;
import game.model.BattleEffect;
import game.model.Enemy;
import game.model.Equipment;
import game.model.Student;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** Draws the battlefield, target cursors and transient HP changes. */
final class BattleScene extends JPanel {
    private static final double EFFECT_DURATION_MILLIS = 1600.0;
    private static final double HOP_DURATION_MILLIS = 180.0;
    private static final int VIRTUAL_WIDTH = 320;
    private static final int VIRTUAL_HEIGHT = 180;
    private static final int[] PARTY_CENTERS_X = {44, 69, 27, 95};
    private static final int[] PARTY_FEET_Y = {119, 130, 141, 141};
    private static final int[] ENEMY_CENTERS_X = {299, 250, 300, 200, 210, 185};
    private static final int[] ENEMY_FEET_Y = {119, 130, 141, 119, 141, 130};
    /** Supplies battle state and animation details to the scene. */
    interface State {
        Battle battle();
        default int encounterStage() { return 0; }
        boolean partyTarget(int index);
        boolean enemyTarget(int index);
        default java.util.List<String> rewardItems() { return java.util.List.of(); }
        default int rewardPetot() { return 0; }
        default int rewardExperience() { return 0; }
        default java.util.List<String> leveledStudents() { return java.util.List.of(); }
        default java.util.List<String> bonusXpStudents() { return java.util.List.of(); }
        boolean showEffects();
        boolean showAmbush();
        default Battle.AbilityAnnouncement abilityAnnouncement() { return null; }
        default boolean choosingItem() { return false; }
        default int itemChoice() { return 0; }
        default int kits() { return 0; }
        default int flowers() { return 0; }
        double victoryProgress();
        double enemyPoints(Enemy enemy);
        Object attackingSprite();
        default Student blockingStudent() { return null; }
        default Student protectedStudent() { return null; }
        double effectProgress();
    }
    private final State state;
    BattleScene(State state) { this.state = state; }
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

    private static int fittedHeight(BufferedImage image, int sourceWidth, int width, int height) {
        double scale = Math.min((double) width / sourceWidth, (double) height / image.getHeight());
        return Math.max(1, (int) Math.round(image.getHeight() * scale));
    }

    private static void drawFit(Graphics2D g, BufferedImage image, int sourceX, int sourceWidth,
            int x, int y, int width, int height) {
        double scale = Math.min((double) width / sourceWidth, (double) height / image.getHeight());
        int drawnWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int drawnHeight = fittedHeight(image, sourceWidth, width, height);
        int left = x + (width - drawnWidth) / 2;
        int top = y + height - drawnHeight;
        g.drawImage(image, left, top, left + drawnWidth, top + drawnHeight,
                sourceX, 0, sourceX + sourceWidth, image.getHeight(), null);
    }


        private final BufferedImage fallbackBackground = image("/assets/backgrounds/campus-courtyard-v1.png");
        private final BufferedImage[] stageBackgrounds = {
            image("/assets/backgrounds/stage-1-campus-entrance-v2.png"),
            image("/assets/backgrounds/stage-2-library-hall-v2.png"),
            image("/assets/backgrounds/stage-3-training-ground-v3.png"),
            image("/assets/backgrounds/stage-4-old-campus-v2.png"),
            image("/assets/backgrounds/stage-5-crystal-chamber-v3.png")
        };
        private final BufferedImage sprites = image("/assets/sprites/student-party-idle-v1.png");
        private final BufferedImage wisp = image("/assets/sprites/library-wisp-v3.png");
        private final BufferedImage sentinel = image("/assets/sprites/stone-sentinel-v1.png");
        private final BufferedImage boss = image("/assets/sprites/crystal-guardian-v1.png");
        private final BufferedImage rewardIcons = image("/assets/ui/reward-icons-v1.png");
        private final BufferedImage uniformIcon = trimIcon(image("/assets/ui/reward-uniform-v1.png"));
        private final BufferedImage[] rewardArt = rewardArt();

        private BufferedImage[] rewardArt() {
            BufferedImage[] icons = new BufferedImage[5];
            if (rewardIcons != null) {
                int cellWidth = rewardIcons.getWidth() / icons.length;
                for (int i = 0; i < icons.length; i++) {
                    icons[i] = trimIcon(rewardIcons.getSubimage(i * cellWidth, 0, cellWidth, rewardIcons.getHeight()));
                }
            }
            return icons;
        }

        private BufferedImage trimIcon(BufferedImage image) {
            if (image == null) { return null; }
            int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) < 128) { continue; }
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
            }
            return right < left ? image : image.getSubimage(left, top, right - left + 1, bottom - top + 1);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Battle battle = state.battle();
            if (getWidth() <= 0 || getHeight() <= 0) { return; }
            int virtualWidth = VIRTUAL_WIDTH;
            int virtualHeight = Math.max(1, getHeight() * virtualWidth / getWidth());
            BufferedImage pixels = new BufferedImage(virtualWidth * 2, virtualHeight * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = pixels.createGraphics();
            try {
                int w = virtualWidth, h = virtualHeight;
                g.scale(2.0, 2.0);
                g.setColor(new Color(30, 43, 71)); g.fillRect(0, 0, w, h);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int stage = state.encounterStage();
                BufferedImage background = stage >= 0 && stage < stageBackgrounds.length ? stageBackgrounds[stage] : null;
                if (background == null) { background = fallbackBackground; }
                if (background != null) { drawCover(g, background, w, h); }
                if (battle == null) { return; }
                int slotWidth = Math.max(27, w / 7);
                int spriteWidth = Math.max(20, slotWidth * 3 / 4);
                int spriteHeight = Math.max(28, h * 3 / 8);
                java.util.List<Student> drawParty = new java.util.ArrayList<>(battle.getParty());
                if (state.showEffects() && state.blockingStudent() != null) {
                    drawParty.remove(state.blockingStudent());
                    drawParty.add(state.blockingStudent());
                }
                for (Student student : drawParty) {
                    int index = battle.getParty().indexOf(student);
                    int x = partyX(index, slotWidth), y = partyFootY(h, index) - spriteHeight;
                    if (!battle.isOver() && student.isAlive() && student == battle.getCurrent()) {
                        drawActiveMarker(g, x + slotWidth / 2, partyFootY(h, index));
                    }
                    BufferedImage fallen = student.isAlive() ? null : FallenSprites.forStudent(student.getId());
                    if (fallen != null) {
                        int fallenWidth = spriteWidth + 4;
                        int left = x + (slotWidth - fallenWidth) / 2;
                        drawFit(g, fallen, 0, fallen.getWidth(), left, y, fallenWidth, spriteHeight);
                    } else if (sprites != null) {
                        int slice = sprites.getWidth() / 4, id = student.getId();
                        int left = x + (slotWidth - spriteWidth) / 2 + attackOffset(student, 1);
                        int lift = battle.isVictory() ? victoryLift(student, index) : attackLift(student);
                        if (student == state.blockingStudent() && state.protectedStudent() != null && state.showEffects()) {
                            int targetIndex = battle.getParty().indexOf(state.protectedStudent());
                            if (targetIndex >= 0) {
                                double movement = guardMovement();
                                left += (int) Math.round((partyX(targetIndex, slotWidth) - x + 7) * movement);
                                lift += (int) Math.round((partyFootY(h, targetIndex) - partyFootY(h, index)) * movement);
                            }
                        }
                        java.awt.Composite previousComposite = g.getComposite();
                        if (student.hasEtherealCharge()) g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.5f));
                        drawFit(g, sprites, id * slice, slice, left, y + lift, spriteWidth, spriteHeight);
                        g.setComposite(previousComposite);
                    }
                    if (state.partyTarget(index)) {
                        int slice = sprites != null ? sprites.getWidth() / 4 : 0;
                        int drawnHeight = sprites != null ? fittedHeight(sprites, slice, spriteWidth, spriteHeight) : spriteHeight;
                        drawCursor(g, x + slotWidth / 2, partyFootY(h, index) - drawnHeight - 3);
                    }
                }
                java.util.List<Integer> drawOrder = new java.util.ArrayList<>();
                for (int index = 0; index < battle.getEnemies().size(); index++) { drawOrder.add(index); }
                drawOrder.sort(java.util.Comparator.comparingInt(index -> enemyFootY(h, enemySlot(index, battle.getEnemies()))));
                for (int enemyIndex : drawOrder) {
                    Enemy enemy = battle.getEnemies().get(enemyIndex);
                    int slot = enemySlot(enemyIndex, battle.getEnemies());
                    int x = enemyX(slot, slotWidth);
                    BufferedImage enemyImage = enemy.isBoss() ? boss : enemy.isSentinel() ? sentinel : wisp;
                    if (enemyImage != null && enemy.isAlive()) {
                        int enemyWidth = enemy.isBoss() ? slotWidth + (state.encounterStage() == 4 ? 40 : 25) : enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                        int enemyHeight = enemy.isBoss() ? spriteHeight * 2 : enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                        int enemyX = x + (slotWidth - enemyWidth) / 2 + attackOffset(enemy, -1);
                        int enemyY = enemyFootY(h, slot) - enemyHeight + attackLift(enemy);
                        drawFit(g, enemyImage, 0, enemyImage.getWidth(), enemyX, enemyY,
                                enemyWidth, enemyHeight);
                        if (enemy.isBoss()) {
                            int drawnHeight = fittedHeight(enemyImage, enemyImage.getWidth(), enemyWidth, enemyHeight);
                            int textX = x + slotWidth / 2 + 10;
                            int textY = enemyFootY(h, slot) - drawnHeight - 5;
                            g.setFont(ClassicUi.numbers(12));
                            g.setColor(Color.BLACK); g.drawString(Integer.toString(enemy.getBossCountdown()), textX + 1, textY + 1);
                            g.setColor(new Color(250, 218, 143)); g.drawString(Integer.toString(enemy.getBossCountdown()), textX, textY);
                        }
                        if (state.enemyTarget(enemyIndex)) {
                            int drawnHeight = fittedHeight(enemyImage, enemyImage.getWidth(), enemyWidth, enemyHeight);
                            drawCursor(g, x + slotWidth / 2, enemyFootY(h, slot) - drawnHeight - 3);
                        }
                    }
                }
            } finally { g.dispose(); }
            Graphics2D output = (Graphics2D) graphics.create();
            try {
                output.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                output.drawImage(pixels, 0, 0, getWidth(), getHeight(), null);
                output.scale(hudScale(), hudScale());
                int columns = Math.min(3, battle.getEnemies().size());
                int columnWidth = 200;
                int listWidth = columns * columnWidth;
                int listLeft = 960 - listWidth - 24;
                int listRows = (battle.getEnemies().size() + columns - 1) / columns;
                int listHeight = listRows * 31 + 8;
                output.setColor(new Color(15, 25, 47, 204));
                output.fillRect(listLeft, 12, listWidth, listHeight);
                output.setFont(ClassicUi.numbers(12));
                for (int i = 0; i < battle.getEnemies().size(); i++) {
                    Enemy enemy = battle.getEnemies().get(i);
                    int column = i % columns;
                    int row = i / columns;
                    int left = listLeft + column * columnWidth;
                    int y = 27 + row * 31;
                    boolean targeted = enemy.isAlive() && state.enemyTarget(i);
                    if (targeted) {
                        output.setColor(new Color(100, 83, 40, 180));
                        output.fillRect(left + 1, y - 13, columnWidth - 2, 29);
                        output.setColor(new Color(250, 218, 143));
                        output.drawRect(left + 1, y - 13, columnWidth - 3, 28);
                    }
                    output.setColor(targeted ? new Color(255, 232, 160) : Color.WHITE);
                    output.drawString((enemy.isBoss() ? "Guardian" : enemy.getName().replace("Stone ", "").replace("Library ", "")) + "  " + enemy.getHp() + " / "
                            + enemy.getMaxHp() + " HP" + (enemy.isAlive() ? "" : " KO"), left + 7, y);
                    output.setColor(new Color(52, 68, 101));
                    output.fillRect(left + 7, y + 5, 122, 4);
                    output.setColor(new Color(239, 157, 102));
                    output.fillRect(left + 7, y + 5,
                            enemy.isAlive() ? (int) (122 * state.enemyPoints(enemy) / enemy.getActionThreshold()) : 0, 4);
                }
                if (state.showEffects()) { drawEffects(output, virtualWidth, virtualHeight); }
                if (state.abilityAnnouncement() != null) drawAbilityAlert(output, state.abilityAnnouncement());
                if (state.choosingItem()) drawItemPicker(output);
                if (battle.isOver()) {
                    drawBattleAlert(output, battle.isVictory() ? "Victory!" : "You lost!");
                    if (battle.isVictory()) { drawRewards(output); }
                }
                else if (state.showAmbush()) { drawBattleAlert(output, "You have been ambushed!"); }
            } finally { output.dispose(); }
        }

        private double hudScale() { return getWidth() / 960.0; }
        private int hudHeight() { return (int) Math.round(getHeight() / hudScale()); }

        private void drawRewards(Graphics2D g) {
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            for (String id : state.rewardItems()) { counts.merge(id, 1, Integer::sum); }
            java.util.List<String> items = new java.util.ArrayList<>(counts.keySet());
            if (items.isEmpty() && state.rewardPetot() == 0 && state.rewardExperience() == 0) { return; }
            int width = 360, x = (960 - width) / 2;
            int y = Math.max(18, hudHeight() / 5) + 52;
            int height = 43 + (items.size() + (state.rewardPetot() > 0 ? 1 : 0)
                    + (state.rewardExperience() > 0 ? 1 : 0) + (state.leveledStudents().isEmpty() ? 0 : 1)
                    + (state.bonusXpStudents().isEmpty() ? 0 : 1)) * 24;
            g.setColor(new Color(12, 25, 48, 224));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(247, 204, 114));
            g.drawRect(x, y, width - 1, height - 1);
            g.drawRect(x + 3, y + 3, width - 7, height - 7);
            g.setFont(ClassicUi.body(17));
            g.drawString("You gain:", x + 16, y + 27);
            g.setFont(ClassicUi.numbers(14));
            int offset = 0;
            if (state.rewardExperience() > 0) {
                int cy = y + 48;
                g.setColor(new Color(175, 133, 245)); g.fillPolygon(new int[] {x + 27, x + 35, x + 27, x + 19}, new int[] {cy - 10, cy - 2, cy + 6, cy - 2}, 4);
                g.setColor(Color.WHITE); g.drawString(state.rewardExperience() + " XP each", x + 50, cy + 4);
                offset++;
            }
            if (!state.bonusXpStudents().isEmpty()) {
                int cy = y + 48 + offset * 24;
                g.setColor(Color.WHITE); g.drawString("Bonus XP: " + String.join(", ", state.bonusXpStudents()), x + 16, cy + 4);
                offset++;
            }
            if (state.rewardPetot() > 0) {
                int cy = y + 48 + offset * 24;
                g.setColor(new Color(240, 189, 73)); g.fillOval(x + 18, cy - 10, 18, 18);
                g.setColor(new Color(255, 232, 154)); g.drawOval(x + 20, cy - 8, 14, 14);
                g.setColor(Color.WHITE); g.drawString(state.rewardPetot() + " Petot", x + 50, cy + 6);
                offset++;
            }
            for (int i = 0; i < items.size(); i++) {
                String id = items.get(i);
                int rowY = y + 36 + (i + offset) * 24;
                drawRewardIcon(g, id, x + 16, rowY, 24);
                g.setColor(Color.WHITE);
                String name = "first_aid_kit".equals(id) ? "First-Aid Kit"
                        : "white_flower".equals(id) ? "White Flower" : Equipment.fromId(id).getName();
                g.drawString(counts.get(id) + "x " + name, x + 50, rowY + 18);
            }
            if (!state.leveledStudents().isEmpty()) {
                g.setColor(new Color(250, 218, 143));
                g.drawString("Level up: " + String.join(", ", state.leveledStudents()), x + 16,
                        y + 36 + (offset + items.size()) * 24 + 17);
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
                drawFit(g, uniformIcon, 0, uniformIcon.getWidth(), x, y, size, size);
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
            if (icon != null) { drawFit(g, icon, 0, icon.getWidth(), x, y, size, size); }
        }

        private void drawItemPicker(Graphics2D g) {
            int width = 320, height = 105, x = (960 - width) / 2, y = Math.max(60, hudHeight() / 4);
            g.setColor(new Color(12, 25, 48, 240)); g.fillRect(x, y, width, height);
            g.setColor(new Color(247, 204, 114)); g.drawRect(x, y, width - 1, height - 1); g.drawRect(x + 3, y + 3, width - 7, height - 7);
            g.setFont(ClassicUi.numbers(16)); g.setColor(new Color(250, 218, 143)); g.drawString("Choose an item", x + 16, y + 24);
            String[] labels = {"First-Aid Kit  × " + state.kits(), "White Flower   × " + state.flowers()};
            for (int i = 0; i < labels.length; i++) {
                int row = y + 50 + i * 28;
                g.setColor(i == state.itemChoice() ? new Color(250, 218, 143) : Color.WHITE);
                if (i == state.itemChoice()) g.drawImage(PixelPointer.ART, x + 12, row - 16, 25, 19, null);
                g.drawString(labels[i], x + 42, row);
            }
        }
        private void drawAbilityAlert(Graphics2D g, Battle.AbilityAnnouncement announcement) {
            java.awt.Font plain = ClassicUi.body(17), bold = plain.deriveFont(java.awt.Font.BOLD);
            g.setFont(bold); int casterWidth = g.getFontMetrics().stringWidth(announcement.caster());
            int skillWidth = g.getFontMetrics().stringWidth(announcement.skill());
            g.setFont(plain); int usedWidth = g.getFontMetrics().stringWidth(" used ");
            int width = casterWidth + usedWidth + skillWidth + 28;
            int x = (960 - width) / 2, y = Math.max(8, hudHeight() / 5 - 50);
            g.setColor(new Color(12, 25, 48, 224)); g.fillRect(x, y, width, 40);
            g.setColor(new Color(247, 204, 114)); g.drawRect(x, y, width - 1, 39); g.drawRect(x + 3, y + 3, width - 7, 33);
            g.setColor(Color.WHITE); g.setFont(bold); g.drawString(announcement.caster(), x + 14, y + 26);
            g.setFont(plain); g.drawString(" used ", x + 14 + casterWidth, y + 26);
            g.setFont(bold); g.drawString(announcement.skill(), x + 14 + casterWidth + usedWidth, y + 26);
        }
        private void drawBattleAlert(Graphics2D g, String text) {
            g.setFont(ClassicUi.body(17));
            int width = g.getFontMetrics().stringWidth(text) + 28;
            int x = (960 - width) / 2;
            int y = Math.max(18, hudHeight() / 5);
            g.setColor(new Color(12, 25, 48, 204));
            g.fillRect(x, y, width, 42);
            g.setColor(new Color(247, 204, 114));
            g.drawRect(x, y, width - 1, 41);
            g.drawRect(x + 3, y + 3, width - 7, 35);
            g.setColor(Color.WHITE);
            g.drawString(text, x + 14, y + 27);
        }

        private void drawEffects(Graphics2D g, int w, int h) {
            Battle battle = state.battle();
            Map<Object, int[]> changes = new java.util.LinkedHashMap<>();
            Map<Object, java.util.List<String>> labels = new java.util.LinkedHashMap<>();
            for (BattleEffect effect : battle.getEffects()) {
                Object target = effect.getStudent() != null ? effect.getStudent() : effect.getEnemy();
                int[] amounts = changes.computeIfAbsent(target, ignored -> new int[2]);
                if (effect.getLabel() != null) labels.computeIfAbsent(target, ignored -> new java.util.ArrayList<>()).add(effect.getLabel());
                else amounts[effect.getAmount() > 0 ? 0 : 1] += effect.getAmount();
            }
            int slotWidth = Math.max(27, w / 7);
            int spriteHeight = Math.max(28, h * 3 / 8);
            g.setFont(ClassicUi.numbers(18));
            for (Map.Entry<Object, int[]> entry : changes.entrySet()) {
                int index = battle.getParty().indexOf(entry.getKey());
                int x;
                int spriteWidth = Math.max(20, slotWidth * 3 / 4);
                int height = sprites != null ? fittedHeight(sprites, sprites.getWidth() / 4, spriteWidth, spriteHeight) : spriteHeight;
                int ground = 0;
                if (index >= 0) {
                    x = partyX(index, slotWidth) + slotWidth / 2;
                    ground = partyFootY(h, index);
                }
                else {
                    index = battle.getEnemies().indexOf(entry.getKey());
                    if (index < 0) { continue; }
                    int slot = enemySlot(index, battle.getEnemies());
                    x = enemyX(slot, slotWidth) + slotWidth / 2;
                    ground = enemyFootY(h, slot);
                    Enemy enemy = (Enemy) entry.getKey();
                    BufferedImage enemyImage = enemy.isBoss() ? boss : enemy.isSentinel() ? sentinel : wisp;
                    int boxWidth = enemy.isBoss() ? slotWidth + (state.encounterStage() == 4 ? 40 : 25) : enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                    int boxHeight = enemy.isBoss() ? spriteHeight * 2 : enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                    height = enemyImage != null ? fittedHeight(enemyImage, enemyImage.getWidth(), boxWidth, boxHeight) : boxHeight;
                }
                double progress = state.effectProgress();
                int alpha = (int) Math.round(255 * Math.min(1.0, Math.max(0.0, (1.0 - progress) * 2.0)));
                int py = Math.max(22, Math.round((float) (ground - 4 - height) * hudHeight() / h) - (int) (progress * 28));
                for (int amount : entry.getValue()) {
                    if (amount == 0) { continue; }
                    String text = (amount > 0 ? "+" : "") + amount + " HP";
                    int px = Math.round((float) x * 960 / w) - g.getFontMetrics().stringWidth(text) / 2;
                    g.setColor(new Color(0, 0, 0, alpha)); g.drawString(text, px + 2, py + 2);
                    g.setColor(new Color(255, 255, 255, alpha));
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx != 0 || dy != 0) { g.drawString(text, px + dx, py + dy); }
                        }
                    }
                    g.setColor(amount > 0 ? new Color(92, 255, 133, alpha) : new Color(255, 100, 100, alpha));
                    g.drawString(text, px, py);
                    py = Math.max(20, py - 22);
                }
                for (String label : labels.getOrDefault(entry.getKey(), java.util.List.of())) {
                    int px = Math.round((float) x * 960 / w) - g.getFontMetrics().stringWidth(label) / 2;
                    g.setColor(new Color(0, 0, 0, alpha)); g.drawString(label, px + 2, py + 2);
                    g.setColor(new Color(255, 255, 255, alpha));
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0) g.drawString(label, px + dx, py + dy);
                    g.setColor(new Color(250, 218, 143, alpha)); g.drawString(label, px, py);
                    py = Math.max(20, py - 22);
                }
            }
        }

        private int partyX(int index, int slotWidth) {
            return PARTY_CENTERS_X[index] - slotWidth / 2;
        }

        private int partyFootY(int height, int index) {
            return PARTY_FEET_Y[index] * height / VIRTUAL_HEIGHT;
        }

        private int enemySlot(int index, java.util.List<Enemy> enemies) {
            int bossIndex = -1;
            for (int i = 0; i < enemies.size(); i++) {
                if (enemies.get(i).isBoss()) { bossIndex = i; break; }
            }
            if (bossIndex < 0) {
                for (int i = 0; i < enemies.size(); i++) {
                    if (enemies.get(i).isSentinel()) { bossIndex = i; break; }
                }
            }
            if (bossIndex < 0) { return index; }
            if (index == bossIndex) { return 1; }
            int otherIndex = index < bossIndex ? index : index - 1;
            return otherIndex == 0 ? 0 : otherIndex + 1;
        }

        private int enemyX(int index, int slotWidth) {
            int[] centers = state.encounterStage() == 4 ? new int[]{294, 250, 294, 172, 185, 210} : ENEMY_CENTERS_X;
            return centers[index] - slotWidth / 2;
        }

        private int enemyFootY(int height, int index) {
            int[] feet = state.encounterStage() == 4 ? new int[]{110, 130, 145, 110, 145, 128} : ENEMY_FEET_Y;
            return feet[index] * height / VIRTUAL_HEIGHT;
        }

        private int victoryLift(Student student, int index) {
            if (!student.isAlive()) { return 0; }
            double progress = state.victoryProgress();
            if (progress >= 1.0) { return 0; }
            double phase = Math.max(0, progress * 3.0 - index * 0.12);
            return -(int) Math.round(4 * Math.max(0, Math.sin(phase * 2 * Math.PI)));
        }

        private double guardMovement() {
            double elapsed = state.effectProgress() * EFFECT_DURATION_MILLIS;
            if (elapsed < 140) return elapsed / 140.0;
            if (elapsed < 550) return 1.0;
            if (elapsed < 800) return (800.0 - elapsed) / 250.0;
            return 0.0;
        }
        private int attackOffset(Object participant, int direction) {
            return direction * (int) Math.round(4 * attackHop(participant));
        }

        private int attackLift(Object participant) {
            return -(int) Math.round(3 * attackHop(participant));
        }

        private double attackHop(Object participant) {
            if (state.attackingSprite() != participant) { return 0.0; }
            double phase = Math.min(1.0, state.effectProgress() * EFFECT_DURATION_MILLIS / HOP_DURATION_MILLIS);
            return Math.sin(Math.PI * phase);
        }

        private void drawActiveMarker(Graphics2D g, int x, int footY) {
            // Fixed pixel-art light rays sit behind the selected student.
            int[] offsets = {-14, -11, -8, 8, 11, 14};
            int[] heights = {12, 19, 11, 15, 21, 12};
            g.setColor(new Color(255, 174, 57, 30));
            g.fillOval(x - 17, footY - 6, 34, 13);
            for (int i = 0; i < offsets.length; i++) {
                int px = x + offsets[i];
                int height = heights[i];
                g.setColor(new Color(255, 166, 55, 45));
                g.fillRect(px - 1, footY - height, 3, height - 2);
                g.setColor(new Color(255, 202, 96, 105));
                g.fillRect(px, footY - height + 4, 1, height - 4);
                g.setColor(new Color(255, 232, 160, 175));
                g.fillRect(px, footY - 6, 1, 4);
            }
            g.setColor(new Color(211, 133, 48, 150));
            g.drawOval(x - 15, footY - 5, 30, 10);
            g.setColor(new Color(250, 218, 143));
            g.drawOval(x - 14, footY - 4, 28, 8);
        }

        private void drawCursor(Graphics2D g, int x, int y) {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[] {x - 5, x + 5, x}, new int[] {y - 7, y - 7, y + 1}, 3);
            g.setColor(new Color(255, 232, 160));
            g.fillPolygon(new int[] {x - 4, x + 4, x}, new int[] {y - 6, y - 6, y}, 3);
        }
}
