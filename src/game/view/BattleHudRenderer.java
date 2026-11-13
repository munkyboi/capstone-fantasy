package game.view;

import game.model.Battle;
import game.model.Enemy;
import java.awt.Color;
import java.awt.Graphics2D;

/** Draws enemy status, floating indicators and battle overlays. */
final class BattleHudRenderer {
    private final BattleScene.State state;
    private final BattleEffectsRenderer effectRenderer;
    private final BattleHudOverlays overlays;

    BattleHudRenderer(BattleScene.State state, BattleEffectsRenderer effectRenderer, BattleHudOverlays overlays) {
        this.state = state;
        this.effectRenderer = effectRenderer;
        this.overlays = overlays;
    }

    void draw(Graphics2D output, Battle battle, int virtualWidth, int virtualHeight) {
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
                    String label = (enemy.isMerchant() ? "Merchant" : enemy.isMirror() ? enemy.getName().replace("Mirror Image", "Mirror") : enemy.isBoss() ? "Guardian" : enemy.getName().replace("Support Sentinel ", "Support ").replace("Attack Sentinel ", "Attack ").replace("Stone ", "").replace("Library ", "")) + "  " + enemy.getHp() + " HP"
                            + (enemy.hasEscaped() ? " ESC" : enemy.isAlive() ? "" : " KO");
                    java.awt.Font normalFont = output.getFont();
                    float labelSize = normalFont.getSize2D();
                    while (output.getFontMetrics().stringWidth(label) > columnWidth - 68 && labelSize > 7) {
                        labelSize -= 0.5f; output.setFont(normalFont.deriveFont(labelSize));
                    }
                    output.drawString(label, left + 7, y);
                    output.setFont(normalFont);
                    output.setColor(new Color(52, 68, 101));
                    output.fillRect(left + columnWidth - 54, y - 8, 46, 4);
                    output.setColor(new Color(239, 157, 102));
                    output.fillRect(left + columnWidth - 54, y - 8,
                            enemy.isAlive() ? (int) (46 * state.enemyPoints(enemy) / enemy.getActionThreshold()) : 0, 4);
                }
                if (state.showEffects()) {
                    long now = System.nanoTime();
                    for (BattleScene.EffectBatch batch : state.effectBatches()) {
                        double progress = Math.min(1.0, Math.max(0.0, (now - batch.startedNanos()) / (BattleScene.EFFECT_DURATION_MILLIS * 1_000_000.0)));
                        if (progress < 1.0) effectRenderer.draw(output, virtualWidth, virtualHeight, batch.effects(), progress, batch.lane());
                    }
                }
                if (state.abilityAnnouncement() != null) overlays.drawAbilityAlert(output, state.abilityAnnouncement());
                if (state.choosingItem()) overlays.drawItemPicker(output);
                if (battle.isOver()) {
                    overlays.drawBattleAlert(output, battle.isVictory() ? "Victory!" : "You lost!");
                    if (battle.isVictory()) { overlays.drawRewards(output); }
                }
                else if (state.showAmbush()) { overlays.drawBattleAlert(output, "You have been ambushed!"); }
    }
}
