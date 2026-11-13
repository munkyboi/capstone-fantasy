package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.Student;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Draws heroes, enemies and their targeting and status markers. */
final class BattleSpritesRenderer {
    private final BattleScene scene;
    private final BufferedImage sprites, stunIcon, dotIcon;

    BattleSpritesRenderer(BattleScene scene, BufferedImage sprites, BufferedImage stunIcon, BufferedImage dotIcon) {
        this.scene = scene;
        this.sprites = sprites;
        this.stunIcon = stunIcon;
        this.dotIcon = dotIcon;
    }

    void draw(Graphics2D g, Battle battle, int w, int h, int slotWidth, int spriteWidth, int spriteHeight) {
                java.util.List<Student> drawParty = new java.util.ArrayList<>(battle.getParty());
                if (scene.battleState().showEffects() && scene.battleState().blockingStudent() != null) {
                    drawParty.remove(scene.battleState().blockingStudent());
                    drawParty.add(scene.battleState().blockingStudent());
                }
                for (Student student : drawParty) {
                    int index = battle.getParty().indexOf(student);
                    int x = scene.partyX(index, slotWidth), y = scene.partyFootY(h, index) - spriteHeight;
                    int spriteCenter = x + slotWidth / 2;
                    int spriteTop = y;
                    if (!battle.isOver() && student.isAlive() && student == battle.getCurrent()) {
                        scene.drawActiveMarker(g, x + slotWidth / 2, scene.partyFootY(h, index));
                    }
                    BufferedImage fallen = student.isAlive() ? null : FallenSprites.forStudent(student.getId());
                    if (fallen != null) {
                        int fallenWidth = spriteWidth + 4;
                        int left = x + (slotWidth - fallenWidth) / 2;
                        BattleScene.drawFit(g, fallen, 0, fallen.getWidth(), left, y, fallenWidth, spriteHeight);
                    } else if (sprites != null) {
                        int slice = sprites.getWidth() / 4, id = student.getId();
                        int left = x + (slotWidth - spriteWidth) / 2 + scene.attackOffset(student, 1);
                        int lift = battle.isVictory() ? scene.victoryLift(student, index) : scene.attackLift(student);
                        if (student == scene.battleState().blockingStudent() && scene.battleState().protectedStudent() != null && scene.battleState().showEffects()) {
                            int targetIndex = battle.getParty().indexOf(scene.battleState().protectedStudent());
                            if (targetIndex >= 0) {
                                double movement = scene.guardMovement();
                                left += (int) Math.round((scene.partyX(targetIndex, slotWidth) - x + 7) * movement);
                                lift += (int) Math.round((scene.partyFootY(h, targetIndex) - scene.partyFootY(h, index)) * movement);
                            }
                        }
                        spriteCenter = left + spriteWidth / 2;
                        spriteTop = y + lift + spriteHeight - BattleScene.fittedHeight(sprites, slice, spriteWidth, spriteHeight);
                        java.awt.Composite previousComposite = g.getComposite();
                        if (student.hasEtherealCharge()) g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.5f));
                        if (scene.battleState().escaping() && student.isAlive()) {
                            int sway = (int) Math.round(2 * Math.sin(System.nanoTime() / 120_000_000.0));
                            Graphics2D flipped = (Graphics2D) g.create();
                            try { flipped.translate(2 * (left + sway) + spriteWidth, 0); flipped.scale(-1, 1);
                                BattleScene.drawFit(flipped, sprites, id * slice, slice, left + sway, y + lift, spriteWidth, spriteHeight);
                            } finally { flipped.dispose(); }
                        } else BattleScene.drawFit(g, sprites, id * slice, slice, left, y + lift, spriteWidth, spriteHeight);
                        g.setComposite(previousComposite);
                    }
                    if (student.isAlive() && !battle.isOver() && battle.getStunTurns(student) > 0 && stunIcon != null) {
                        int centerX = Math.max(9, Math.min(w - 9, spriteCenter - (battle.hasDamageOverTime(student) ? 8 : 0)));
                        int centerY = Math.max(9, spriteTop - 9);
                        int frame = scene.isShowing() ? (int) ((System.nanoTime() / 125_000_000L) % 16) : 0;
                        Graphics2D iconGraphics = (Graphics2D) g.create();
                        try {
                            iconGraphics.rotate(frame * Math.PI / 8, centerX, centerY);
                            BattleScene.drawFit(iconGraphics, stunIcon, 0, stunIcon.getWidth(), centerX - 8, centerY - 8, 16, 16);
                        } finally { iconGraphics.dispose(); }
                    }
                    if (student.isAlive() && battle.hasDamageOverTime(student) && dotIcon != null) {
                        int centerX = Math.max(8, Math.min(w - 8, spriteCenter + (battle.getStunTurns(student) > 0 ? 8 : 0)));
                        int centerY = Math.max(8, spriteTop - 9);
                        BattleScene.drawFit(g, dotIcon, 0, dotIcon.getWidth(), centerX - 7, centerY - 7, 14, 14);
                    }
                    if (scene.battleState().partyTarget(index)) {
                        int slice = sprites != null ? sprites.getWidth() / 4 : 0;
                        int drawnHeight = sprites != null ? BattleScene.fittedHeight(sprites, slice, spriteWidth, spriteHeight) : spriteHeight;
                        scene.drawCursor(g, x + slotWidth / 2, scene.partyFootY(h, index) - drawnHeight - 3);
                    }
                }
                java.util.List<Integer> drawOrder = new java.util.ArrayList<>();
                for (int index = 0; index < battle.getEnemies().size(); index++) { drawOrder.add(index); }
                drawOrder.sort(java.util.Comparator.comparingInt(index -> scene.enemyFootY(h, scene.enemySlot(index, battle.getEnemies()))));
                if (battle.isMerchantEncounter()) drawOrder.sort(java.util.Comparator.comparingInt(index -> battle.getEnemies().get(index).isMirror() ? 1 : 0));
                for (int enemyIndex : drawOrder) {
                    Enemy enemy = battle.getEnemies().get(enemyIndex);
                    int slot = scene.enemySlot(enemyIndex, battle.getEnemies());
                    int x = scene.enemyX(slot, slotWidth);
                    BufferedImage enemyImage = scene.enemyArt(enemy);
                    boolean escaping = enemy.hasEscaped() && scene.battleState().attackingSprite() == enemy
                            && scene.battleState().showEffects() && scene.battleState().effectProgress() < 1.0;
                    if (enemyImage != null && (enemy.isAlive() || escaping) && !(enemy.isMirror() && battle.isVictory())) {
                        int enemyWidth = enemy.isMerchant() || enemy.isMirror() ? slotWidth + 8 : enemy.isBoss() ? slotWidth + (scene.battleState().encounterStage() == 4 ? 40 : 25) : enemy.isSentinel() ? slotWidth + 6 : spriteWidth;
                        int enemyHeight = enemy.isMerchant() || enemy.isMirror() ? spriteHeight * 3 / 2 : enemy.isBoss() ? spriteHeight * 2 : enemy.isSentinel() ? spriteHeight * 3 / 2 : spriteHeight;
                        int enemyX = x + (slotWidth - enemyWidth) / 2 + (escaping
                                ? (int) Math.ceil((w + enemyWidth - x) * scene.battleState().effectProgress()) : scene.attackOffset(enemy, -1));
                        int enemyY = scene.enemyFootY(h, slot) - enemyHeight + scene.attackLift(enemy);
                        java.awt.Composite previous = g.getComposite();
                        if (enemy.isMirror()) g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.70f));
                        if (escaping) BattleScene.drawFlippedFit(g, enemyImage, enemyX, enemyY, enemyWidth, enemyHeight);
                        else BattleScene.drawFit(g, enemyImage, 0, enemyImage.getWidth(), enemyX, enemyY, enemyWidth, enemyHeight);
                        g.setComposite(previous);
                        if (enemy.isAlive() && battle.hasDamageOverTime(enemy) && dotIcon != null) {
                            int drawnHeight = BattleScene.fittedHeight(enemyImage, enemyImage.getWidth(), enemyWidth, enemyHeight);
                            int centerX = Math.max(7, Math.min(w - 7, enemyX + enemyWidth / 2));
                            int centerY = Math.max(7, enemyY + enemyHeight - drawnHeight - 8);
                            BattleScene.drawFit(g, dotIcon, 0, dotIcon.getWidth(), centerX - 7, centerY - 7, 14, 14);
                        }
                        if (enemy.isAlive() && ((enemy.isBoss() && enemy.getBossCountdown() > 0) || (enemy.isLootGoblin() && enemy.getEscapeCountdown() >= 0))) {
                            int drawnHeight = BattleScene.fittedHeight(enemyImage, enemyImage.getWidth(), enemyWidth, enemyHeight);
                            int textY = enemyY + enemyHeight - drawnHeight - 5;
                            g.setFont(ClassicUi.numbers(12));
                            String countdown = Integer.toString(enemy.isBoss() ? enemy.getBossCountdown() : enemy.getEscapeCountdown());
                            int textWidth = g.getFontMetrics().stringWidth(countdown);
                            int textX = Math.max(0, Math.min(w - textWidth - 2, enemyX + enemyWidth / 2 - textWidth / 2));
                            g.setColor(Color.BLACK); g.drawString(countdown, textX + 1, textY + 1);
                            g.setColor(new Color(250, 218, 143)); g.drawString(countdown, textX, textY);
                        }
                        if (enemy.isAlive() && scene.battleState().enemyTarget(enemyIndex)) {
                            int drawnHeight = BattleScene.fittedHeight(enemyImage, enemyImage.getWidth(), enemyWidth, enemyHeight);
                            scene.drawCursor(g, x + slotWidth / 2, scene.enemyFootY(h, slot) - drawnHeight - 3);
                        }
                    }
                }
    }
}
