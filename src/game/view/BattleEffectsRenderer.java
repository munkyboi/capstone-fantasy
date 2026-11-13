package game.view;

import game.model.Battle;
import game.model.BattleEffect;
import game.model.Enemy;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

/** Draws floating damage, healing and status indicators. */
final class BattleEffectsRenderer {
    private final BattleScene scene;
    BattleEffectsRenderer(BattleScene scene) { this.scene = scene; }
    void draw(Graphics2D g, int w, int h, java.util.List<BattleEffect> effects, double progress, int lane) {
            Battle battle = scene.battleState().battle();
            Map<Object, int[]> changes = new java.util.LinkedHashMap<>();
            Map<Object, java.util.List<String>> labels = new java.util.LinkedHashMap<>();
            Map<Enemy, Integer> manaLeaks = new java.util.LinkedHashMap<>();
            for (BattleEffect effect : effects) {
                if (effect.isManaLeak()) { manaLeaks.merge(effect.getEnemy(), effect.getAmount(), Integer::sum); continue; }
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
                int height = scene.studentArt() != null ? BattleScene.fittedHeight(scene.studentArt(), scene.studentArt().getWidth() / 4, spriteWidth, spriteHeight) : spriteHeight;
                int ground = 0;
                if (index >= 0) {
                    x = scene.partyX(index, slotWidth) + slotWidth / 2;
                    ground = scene.partyFootY(h, index);
                }
                else {
                    index = battle.getEnemies().indexOf(entry.getKey());
                    if (index < 0) { continue; }
                    int slot = scene.enemySlot(index, battle.getEnemies());
                    x = scene.enemyX(slot, slotWidth) + slotWidth / 2;
                    ground = scene.enemyFootY(h, slot);
                    Enemy enemy = (Enemy) entry.getKey();
                    BufferedImage enemyImage = scene.enemyArt(enemy);
                    int boxWidth = enemy.isMerchant() || enemy.isMirror() ? slotWidth + 8 : enemy.isBoss() ? slotWidth + (scene.battleState().encounterStage() == 4 ? 40 : 25) : enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                    int boxHeight = enemy.isMerchant() || enemy.isMirror() ? spriteHeight * 3 / 2 : enemy.isBoss() ? spriteHeight * 2 : enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                    height = enemyImage != null ? BattleScene.fittedHeight(enemyImage, enemyImage.getWidth(), boxWidth, boxHeight) : boxHeight;
                }
                int alpha = (int) Math.round(255 * Math.min(1.0, Math.max(0.0, (1.0 - progress) * 2.0)));
                int py = Math.max(22, Math.round((float) (ground - 4 - height) * scene.hudHeight() / h) - (int) (progress * 28));
                for (int amount : entry.getValue()) {
                    if (amount == 0) { continue; }
                    String text = (amount > 0 ? "+" : "") + amount + " HP";
                    int px = Math.round((float) x * 960 / w) - g.getFontMetrics().stringWidth(text) / 2 + lane;
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
                    int px = Math.round((float) x * 960 / w) - g.getFontMetrics().stringWidth(label) / 2 + lane;
                    g.setColor(new Color(0, 0, 0, alpha)); g.drawString(label, px + 2, py + 2);
                    g.setColor(new Color(255, 255, 255, alpha));
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0) g.drawString(label, px + dx, py + dy);
                    g.setColor(new Color(250, 218, 143, alpha)); g.drawString(label, px, py);
                    py = Math.max(20, py - 22);
                }
            }
            for (Map.Entry<Enemy, Integer> entry : manaLeaks.entrySet()) {
                Enemy enemy = entry.getKey();
                int index = battle.getEnemies().indexOf(enemy);
                if (index < 0) continue;
                int slot = scene.enemySlot(index, battle.getEnemies());
                int spriteWidth = Math.max(20, slotWidth * 3 / 4);
                BufferedImage art = scene.enemyArt(enemy);
                int width = enemy.isMerchant() || enemy.isMirror() ? slotWidth + 8 : enemy.isBoss() ? slotWidth + (scene.battleState().encounterStage() == 4 ? 40 : 25) : enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                int boxHeight = enemy.isMerchant() || enemy.isMirror() || enemy.isSentinel() && !enemy.isBoss() ? spriteHeight * 3 / 2 : enemy.isBoss() ? spriteHeight * 2 : spriteHeight;
                int height = art != null ? BattleScene.fittedHeight(art, art.getWidth(), width, boxHeight) : boxHeight;
                int centerX = scene.enemyX(slot, slotWidth) + slotWidth / 2;
                int centerY = scene.enemyFootY(h, slot) - height / 2;
                int alpha = (int) Math.round(255 * Math.max(0, Math.min(1, Math.min(progress * 4, (1 - progress) * 4))));
                String text = entry.getValue() + " SP";
                int px = Math.round((float) centerX * 960 / w) - g.getFontMetrics().stringWidth(text) / 2 + lane;
                int py = Math.round((float) centerY * scene.hudHeight() / h) + (int) (progress * 20);
                g.setColor(new Color(0, 0, 0, alpha)); g.drawString(text, px + 2, py + 2);
                g.setColor(new Color(255, 255, 255, alpha));
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                    if (dx != 0 || dy != 0) g.drawString(text, px + dx, py + dy);
                g.setColor(new Color(255, 157, 45, alpha)); g.drawString(text, px, py);
            }
        }

}
