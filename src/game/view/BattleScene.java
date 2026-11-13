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
    /** A group of indicators created together with its own animation clock. */
    record EffectBatch(java.util.List<BattleEffect> effects, long startedNanos, int lane) { }
    static final double EFFECT_DURATION_MILLIS = 1600.0;
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
        default int victoryChoice() { return 1; }
        default boolean canExplore() { return false; }
        default void chooseVictory(int choice) { }
        default java.util.List<String> leveledStudents() { return java.util.List.of(); }
        default java.util.List<String> bonusXpStudents() { return java.util.List.of(); }
        boolean showEffects();
        default java.util.List<EffectBatch> effectBatches() { return java.util.List.of(); }
        boolean showAmbush();
        default Battle.AbilityAnnouncement abilityAnnouncement() { return null; }
        default boolean choosingItem() { return false; }
        default int itemChoice() { return 0; }
        default int kits() { return 0; }
        default int flowers() { return 0; }
        double victoryProgress();
        double enemyPoints(Enemy enemy);
        Object attackingSprite();
        default Student rainShooter() { return null; }
        default java.util.List<Enemy> rainTargets() { return java.util.List.of(); }
        default double rainElapsedMillis() { return 800; }
        default Student blockingStudent() { return null; }
        default Student protectedStudent() { return null; }
        double effectProgress();
        default boolean escaping() { return false; }
    }
    private final State state;
    BattleScene(State state) {
        this.state = state;
        this.overlays = new BattleHudOverlays(state, this::hudHeight, uniformIcon, rewardArt);
        this.spritesRenderer = new BattleSpritesRenderer(this, sprites, stunIcon, dotIcon);
        this.hudRenderer = new BattleHudRenderer(state, effectRenderer, overlays);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (state.battle() == null || !state.battle().isVictory() || getWidth() <= 0) return;
                int x = (int) (event.getX() / hudScale()), y = (int) (event.getY() / hudScale());
                int boxX = 300, buttonY = rewardButtonY();
                if (y < buttonY || y > buttonY + 22) return;
                if (state.canExplore() && x >= boxX + 40 && x <= boxX + 165) state.chooseVictory(0);
                else if (state.canExplore() && x >= boxX + 195 && x <= boxX + 320) state.chooseVictory(1);
                else if (!state.canExplore() && x >= boxX + 115 && x <= boxX + 240) state.chooseVictory(1);
            }
        });
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

    static void drawFlippedFit(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
        int drawnWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawnHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int left = x + (width - drawnWidth) / 2, top = y + height - drawnHeight;
        g.drawImage(image, left + drawnWidth, top, left, top + drawnHeight,
                0, 0, image.getWidth(), image.getHeight(), null);
    }

    static int fittedHeight(BufferedImage image, int sourceWidth, int width, int height) {
        double scale = Math.min((double) width / sourceWidth, (double) height / image.getHeight());
        return Math.max(1, (int) Math.round(image.getHeight() * scale));
    }

    static void drawFit(Graphics2D g, BufferedImage image, int sourceX, int sourceWidth,
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
        private final BufferedImage stunIcon = trimIcon(image("/assets/ui/stunned-icon-v1.png"));
        private final BufferedImage dotIcon = trimIcon(image("/assets/ui/damage-over-time-icon-v1.png"));
        private final BufferedImage wisp = image("/assets/sprites/library-wisp-v3.png");
        private final BufferedImage sentinel = image("/assets/sprites/stone-sentinel-v1.png");
        private final BufferedImage attackSentinel = image("/assets/sprites/attack-sentinel-v1.png");
        private final BufferedImage boss = image("/assets/sprites/crystal-guardian-v1.png");
        private final BufferedImage goblin = trimIcon(image("/assets/sprites/itmigims-v1.png"));
        private final BufferedImage merchant = trimIcon(image("/assets/sprites/mysterious-merchant-v1.png"));
        private final BufferedImage marketBackground = image("/assets/backgrounds/carbon-market-battle-v2.png");
        private final BufferedImage rewardIcons = image("/assets/ui/reward-icons-v1.png");
        private final BufferedImage uniformIcon = trimIcon(image("/assets/ui/reward-uniform-v1.png"));
        private final BufferedImage[] rewardArt = rewardArt();
        private final BattleHudOverlays overlays;
        private final BattleSpritesRenderer spritesRenderer;
        private final BattleHudRenderer hudRenderer;
        private final BattleEffectsRenderer effectRenderer = new BattleEffectsRenderer(this);

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
                BufferedImage background = battle != null && battle.isMerchantEncounter() ? marketBackground : stage >= 0 && stage < stageBackgrounds.length ? stageBackgrounds[stage] : null;
                if (background == null) { background = fallbackBackground; }
                if (background != null) { drawCover(g, background, w, h); }
                if (battle == null) { return; }
                int slotWidth = Math.max(27, w / 7);
                int spriteWidth = Math.max(20, slotWidth * 3 / 4);
                int spriteHeight = Math.max(28, h * 3 / 8);
                spritesRenderer.draw(g, battle, w, h, slotWidth, spriteWidth, spriteHeight);
                if (state.rainShooter() != null) {
                    Student shooter = state.rainShooter();
                    double elapsed = state.rainElapsedMillis();
                    int shooterIndex = battle.getParty().indexOf(shooter);
                    if (shooterIndex >= 0 && elapsed < 800) {
                        int startX = partyX(shooterIndex, slotWidth) + slotWidth / 2;
                        int startY = partyFootY(h, shooterIndex) - spriteHeight / 2;
                        java.util.List<Enemy> arrows = state.rainTargets();
                        for (int i = 0; i < arrows.size(); i++) {
                            int enemyIndex = battle.getEnemies().indexOf(arrows.get(i));
                            if (enemyIndex < 0) continue;
                            int slot = enemySlot(enemyIndex, battle.getEnemies());
                            int endX = enemyX(slot, slotWidth) + slotWidth / 2;
                            int endY = enemyFootY(h, slot) - spriteHeight / 2;
                            int spread = (i % 5 - 2) * 5;
                            int x, y, direction;
                            if (elapsed < 350) {
                                double progress = elapsed / 350.0;
                                x = startX + spread + (int)Math.round((i / 5 - 2) * 5 * progress);
                                y = (int)Math.round(startY + (-12 - startY) * progress);
                                direction = -1;
                            } else {
                                double progress = Math.min(1.0, (elapsed - 350) / 450.0);
                                x = endX + spread + (i / 5 - 2) * 3;
                                y = (int)Math.round(-12 + (endY + 12) * progress);
                                direction = 1;
                            }
                            g.setColor(new Color(47, 27, 36));
                            g.drawLine(x + 1, y - direction * 6, x + 1, y + direction * 2);
                            g.setColor(new Color(255, 225, 145));
                            g.drawLine(x, y - direction * 6, x, y + direction * 2);
                            g.drawLine(x - 1, y + direction, x + 1, y + direction);
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
                hudRenderer.draw(output, battle, virtualWidth, virtualHeight);
            } finally { output.dispose(); }
        }

    State battleState() { return state; }
    BufferedImage studentArt() { return sprites; }
    BufferedImage enemyArt(Enemy enemy) {
        return enemy.isMerchant() || enemy.isMirror() ? merchant : enemy.isLootGoblin() ? goblin
                : enemy.isBoss() ? boss : enemy.isSentinel() ? (enemy.isAttackSentinel() ? attackSentinel : sentinel) : wisp;
    }

        private double hudScale() { return getWidth() / 960.0; }
        int hudHeight() { return (int) Math.round(getHeight() / hudScale()); }
        private int rewardButtonY() { return overlays.rewardButtonY(); }

        int partyX(int index, int slotWidth) {
            return PARTY_CENTERS_X[index] - slotWidth / 2;
        }

        int partyFootY(int height, int index) {
            return PARTY_FEET_Y[index] * height / VIRTUAL_HEIGHT;
        }

        int enemySlot(int index, java.util.List<Enemy> enemies) {
            int bossIndex = -1;
            for (int i = 0; i < enemies.size(); i++) {
                if (enemies.get(i).isBoss() || enemies.get(i).isMerchant()) { bossIndex = i; break; }
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

        int enemyX(int index, int slotWidth) {
            if (state.battle() != null && state.battle().isMerchantEncounter()) {
                int[] centers = {190, 265, 225, 200, 210, 185};
                return centers[index] - slotWidth / 2;
            }
            int[] centers = state.encounterStage() == 4 ? new int[]{294, 250, 294, 172, 185, 210} : ENEMY_CENTERS_X;
            return centers[index] - slotWidth / 2;
        }

        int enemyFootY(int height, int index) {
            int[] feet = state.encounterStage() == 4 ? new int[]{110, 130, 145, 110, 145, 128} : ENEMY_FEET_Y;
            return feet[index] * height / VIRTUAL_HEIGHT;
        }

        int victoryLift(Student student, int index) {
            if (!student.isAlive()) { return 0; }
            double progress = state.victoryProgress();
            if (progress >= 1.0) { return 0; }
            double phase = Math.max(0, progress * 3.0 - index * 0.12);
            return -(int) Math.round(4 * Math.max(0, Math.sin(phase * 2 * Math.PI)));
        }

        double guardMovement() {
            double elapsed = state.effectProgress() * EFFECT_DURATION_MILLIS;
            if (elapsed < 140) return elapsed / 140.0;
            if (elapsed < 550) return 1.0;
            if (elapsed < 800) return (800.0 - elapsed) / 250.0;
            return 0.0;
        }
        int attackOffset(Object participant, int direction) {
            return direction * (int) Math.round(4 * attackHop(participant));
        }

        int attackLift(Object participant) {
            return -(int) Math.round(3 * attackHop(participant));
        }

        private double attackHop(Object participant) {
            if (state.attackingSprite() != participant) { return 0.0; }
            double phase = Math.min(1.0, state.effectProgress() * EFFECT_DURATION_MILLIS / HOP_DURATION_MILLIS);
            return Math.sin(Math.PI * phase);
        }

        void drawActiveMarker(Graphics2D g, int x, int footY) {
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

        void drawCursor(Graphics2D g, int x, int y) {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[] {x - 5, x + 5, x}, new int[] {y - 7, y - 7, y + 1}, 3);
            g.setColor(new Color(255, 232, 160));
            g.fillPolygon(new int[] {x - 4, x + 4, x}, new int[] {y - 6, y - 6, y}, 3);
        }
}
