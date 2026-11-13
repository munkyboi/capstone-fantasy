package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.Student;
import java.util.List;
import static game.view.BattlePanel.TargetAction;

/** Supplies live battle state to the battlefield renderer. */
final class BattleViewState implements BattleScene.State {
    private final BattlePanel panel;
    BattleViewState(BattlePanel panel) { this.panel = panel; }

            @Override public Battle battle() { return panel.actionBattle(); }
            @Override public int encounterStage() { return panel.viewStage(); }
            @Override public boolean partyTarget(int index) {
                return panel.inputTargeting() != TargetAction.NONE && panel.inputTargetsParty() && index == panel.inputTargetIndex();
            }
            @Override public boolean enemyTarget(int index) {
                return panel.inputTargeting() != TargetAction.NONE && !panel.inputTargetsParty()
                        && (panel.inputTargetsAllEnemies() || index == panel.inputTargetIndex());
            }
            @Override public List<String> rewardItems() { return panel.viewRewards(); }
            @Override public int rewardPetot() { return panel.viewPetot(); }
            @Override public int rewardExperience() { return panel.viewExperience(); }
            @Override public int victoryChoice() { return panel.inputVictoryChoice(); }
            @Override public boolean canExplore() { return !panel.actionBattle().isMerchantEncounter() && panel.viewExplore() != null; }
            @Override public void chooseVictory(int choice) {
                panel.inputVictoryChoice(choice);
                panel.inputConfirm();
            }
            @Override public List<String> leveledStudents() { return panel.viewLeveled(); }
            @Override public List<String> bonusXpStudents() { return panel.viewBonus(); }
            @Override public boolean showEffects() { return panel.viewShowEffects(); }
            @Override public List<BattleScene.EffectBatch> effectBatches() { return List.copyOf(panel.viewBatches()); }
            @Override public boolean escaping() { return panel.actionBattle() != null && panel.actionBattle().isEscaping(); }
            @Override public boolean showAmbush() { return panel.viewAmbush(); }
            @Override public Battle.AbilityAnnouncement abilityAnnouncement() { return panel.viewAnnouncement(); }
            @Override public boolean choosingItem() { return panel.inputChoosingItem(); }
            @Override public int itemChoice() { return panel.inputItemChoice(); }
            @Override public int kits() { return panel.actionBattle() == null ? 0 : panel.actionBattle().getKits(); }
            @Override public int flowers() { return panel.inputProgress() == null ? 0 : panel.inputProgress().getInventory().getWhiteFlowers(); }
            @Override public double victoryProgress() {
                return panel.viewVictoryStarted() == 0 ? 1.0 : Math.min(1.0,
                        (System.nanoTime() - panel.viewVictoryStarted()) / (1800 * 1_000_000.0));
            }
            @Override public double enemyPoints(Enemy enemy) { return panel.viewEnemyPoints().getOrDefault(enemy, 0.0); }
            @Override public Object attackingSprite() { return panel.viewAttacker(); }
            @Override public Student rainShooter() { return panel.viewRainShooter(); }
            @Override public List<Enemy> rainTargets() { return panel.viewRainTargets(); }
            @Override public double rainElapsedMillis() { return (System.nanoTime() - panel.viewRainStarted()) / 1_000_000.0; }
            @Override public Student blockingStudent() { return panel.actionBattle() == null || !(panel.viewAttacker() instanceof Enemy) ? null : panel.actionBattle().getBlockingStudent(); }
            @Override public Student protectedStudent() { return panel.actionBattle() == null || !(panel.viewAttacker() instanceof Enemy) ? null : panel.actionBattle().getProtectedStudent(); }
            @Override public double effectProgress() {
                return panel.viewAttacker() != null ? Math.min(1.0, (System.nanoTime() - panel.viewEffectStarted()) / (1600 * 1_000_000.0)) : 1.0;
            }
}
