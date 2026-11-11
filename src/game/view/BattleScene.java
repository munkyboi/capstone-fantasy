package game.view;

import game.model.Battle;
import game.model.BattleEffect;
import game.model.Enemy;
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
    private static final int[] PARTY_CENTERS_X = {44, 69, 27};
    private static final int[] PARTY_FEET_Y = {101, 120, 139};
    private static final int[] ENEMY_CENTERS_X = {280, 260, 295, 226, 234, 198};
    private static final int[] ENEMY_FEET_Y = {101, 120, 139, 101, 139, 120};
    /** Supplies battle state and animation details to the scene. */
    interface State {
        Battle battle();
        boolean partyTarget(int index);
        boolean enemyTarget(int index);
        boolean showEffects();
        boolean showAmbush();
        double victoryProgress();
        double enemyPoints(Enemy enemy);
        Object attackingSprite();
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


        private final BufferedImage background = image("/assets/backgrounds/campus-courtyard-v1.png");
        private final BufferedImage sprites = image("/assets/sprites/student-party-idle-v1.png");
        private final BufferedImage wisp = image("/assets/sprites/library-wisp-v2.png");
        private final BufferedImage sentinel = image("/assets/sprites/stone-sentinel-v1.png");
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Battle battle = state.battle();
            if (getWidth() <= 0 || getHeight() <= 0) { return; }
            int virtualWidth = VIRTUAL_WIDTH;
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
                    int x = partyX(index, slotWidth), y = partyFootY(h, index) - spriteHeight;
                    if (sprites != null) {
                        int slice = sprites.getWidth() / 4, id = student.getId();
                        int left = x + (slotWidth - spriteWidth) / 2 + attackOffset(student, 1);
                        int lift = battle.isVictory() ? victoryLift(student, index) : attackLift(student);
                        drawFit(g, sprites, id * slice, slice, left, y + lift, spriteWidth, spriteHeight);
                    }
                    if (state.partyTarget(index)) {
                        int slice = sprites != null ? sprites.getWidth() / 4 : 0;
                        int drawnHeight = sprites != null ? fittedHeight(sprites, slice, spriteWidth, spriteHeight) : spriteHeight;
                        drawCursor(g, x + slotWidth / 2, partyFootY(h, index) - drawnHeight - 3);
                    }
                }
                for (int enemyIndex = 0; enemyIndex < battle.getEnemies().size(); enemyIndex++) {
                    Enemy enemy = battle.getEnemies().get(enemyIndex);
                    int slot = enemySlot(enemyIndex, battle.getEnemies());
                    int x = enemyX(slot, slotWidth);
                    BufferedImage enemyImage = enemy.isSentinel() ? sentinel : wisp;
                    if (enemyImage != null && enemy.isAlive()) {
                        int enemyWidth = enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                        int enemyHeight = enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                        int enemyX = x + (slotWidth - enemyWidth) / 2 + attackOffset(enemy, -1);
                        int enemyY = enemyFootY(h, slot) - enemyHeight + attackLift(enemy);
                        drawFit(g, enemyImage, 0, enemyImage.getWidth(), enemyX, enemyY,
                                enemyWidth, enemyHeight);
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
                boolean twoColumns = battle.getEnemies().size() > 3;
                int listWidth = twoColumns ? 400 : 220;
                int listLeft = getWidth() - listWidth - 24;
                int listRows = twoColumns ? 3 : battle.getEnemies().size();
                int listHeight = Math.max(82, listRows * 36 + 10);
                int columnWidth = listWidth / (twoColumns ? 2 : 1);
                output.setColor(new Color(15, 25, 47, 225));
                output.fillRect(listLeft, 12, listWidth, listHeight);
                output.setColor(Color.WHITE);
                output.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                for (int i = 0; i < battle.getEnemies().size(); i++) {
                    Enemy enemy = battle.getEnemies().get(i);
                    int column = twoColumns ? i / 3 : 0;
                    int row = twoColumns ? i % 3 : i;
                    int left = listLeft + column * columnWidth;
                    int y = 27 + row * 36;
                    boolean targeted = enemy.isAlive() && state.enemyTarget(i);
                    if (targeted) {
                        output.setColor(new Color(100, 83, 40, 180));
                        output.fillRect(left + 1, y - 13, columnWidth - 2, 34);
                        output.setColor(new Color(250, 218, 143));
                        output.drawRect(left + 1, y - 13, columnWidth - 3, 33);
                    }
                    output.setColor(targeted ? new Color(255, 232, 160) : Color.WHITE);
                    output.drawString((enemy.isSentinel() ? "Sentinel" : enemy.getName().replace("Library ", "")) + "  " + enemy.getHp() + " / "
                            + enemy.getMaxHp() + " HP" + (enemy.isAlive() ? "" : " KO"), left + 7, y);
                    output.setColor(new Color(52, 68, 101));
                    output.fillRect(left + 7, y + 5, 122, 8);
                    output.setColor(new Color(239, 157, 102));
                    output.fillRect(left + 7, y + 5,
                            enemy.isAlive() ? (int) (122 * state.enemyPoints(enemy) / enemy.getActionThreshold()) : 0, 8);
                    output.setColor(Color.WHITE);
                    output.drawString(enemy.isAlive() ? (int) state.enemyPoints(enemy) + "/" + enemy.getActionThreshold() + " AP" : "KO",
                            left + 135, y + 13);
                }
                if (state.showEffects()) { drawEffects(output, virtualWidth, virtualHeight); }
                if (battle.isOver()) { drawBattleAlert(output, battle.isVictory() ? "Victory!" : "You lost!"); }
                else if (state.showAmbush()) { drawBattleAlert(output, "You have been ambushed!"); }
            } finally { output.dispose(); }
        }

        private void drawBattleAlert(Graphics2D g, String text) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            int width = g.getFontMetrics().stringWidth(text) + 28;
            int x = (getWidth() - width) / 2;
            int y = Math.max(18, getHeight() / 5);
            g.setColor(new Color(12, 25, 48, 235));
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
            for (BattleEffect effect : battle.getEffects()) {
                Object target = effect.getStudent() != null ? effect.getStudent() : effect.getEnemy();
                int[] amounts = changes.computeIfAbsent(target, ignored -> new int[2]);
                amounts[effect.getAmount() > 0 ? 0 : 1] += effect.getAmount();
            }
            int slotWidth = Math.max(27, w / 7);
            int spriteHeight = Math.max(28, h * 3 / 8);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
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
                    BufferedImage enemyImage = enemy.isSentinel() ? sentinel : wisp;
                    int boxWidth = enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                    int boxHeight = enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                    height = enemyImage != null ? fittedHeight(enemyImage, enemyImage.getWidth(), boxWidth, boxHeight) : boxHeight;
                }
                double progress = state.effectProgress();
                int alpha = (int) Math.round(255 * Math.min(1.0, Math.max(0.0, (1.0 - progress) * 2.0)));
                int py = Math.max(22, Math.round((float) (ground - 4 - height) * getHeight() / h) - (int) (progress * 28));
                for (int amount : entry.getValue()) {
                    if (amount == 0) { continue; }
                    String text = (amount > 0 ? "+" : "") + amount + " HP";
                    int px = Math.round((float) x * getWidth() / w) - g.getFontMetrics().stringWidth(text) / 2;
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
            if (bossIndex < 0) { return index; }
            if (index == bossIndex) { return 1; }
            int otherIndex = index < bossIndex ? index : index - 1;
            return otherIndex == 0 ? 0 : otherIndex + 1;
        }

        private int enemyX(int index, int slotWidth) {
            return ENEMY_CENTERS_X[index] - slotWidth / 2;
        }

        private int enemyFootY(int height, int index) {
            return ENEMY_FEET_Y[index] * height / VIRTUAL_HEIGHT;
        }

        private int victoryLift(Student student, int index) {
            if (!student.isAlive()) { return 0; }
            double progress = state.victoryProgress();
            if (progress >= 1.0) { return 0; }
            double phase = Math.max(0, progress * 3.0 - index * 0.12);
            return -(int) Math.round(4 * Math.max(0, Math.sin(phase * 2 * Math.PI)));
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

        private void drawCursor(Graphics2D g, int x, int y) {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[] {x - 5, x + 5, x}, new int[] {y - 7, y - 7, y + 1}, 3);
            g.setColor(new Color(255, 232, 160));
            g.fillPolygon(new int[] {x - 4, x + 4, x}, new int[] {y - 6, y - 6, y}, 3);
        }
}
