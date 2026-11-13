package game.view;

import game.model.Student;
import static game.view.BattlePanel.TargetAction;

/** Updates the battle command and status panels from current state. */
final class BattleHudController {
    private final BattlePanel panel;
    private BattleHudController(BattlePanel panel) { this.panel = panel; }
    static void update(BattlePanel panel) { new BattleHudController(panel).refresh(); }
    private void refresh() {
        boolean over = panel.actionBattle().isOver();
        refreshStatus();
        String targetMessage = panel.inputTargeting() == TargetAction.NONE ? null : panel.inputTargetsParty()
                ? "Choose " + panel.actionBattle().getParty().get(panel.inputTargetIndex()).getName() + " as the target."
                : panel.inputTargeting() == TargetAction.ABILITY && panel.actionBattle().getCurrent().getAbilityType() == Student.AbilityType.ARCANE_BURST
                ? "Arcane Burst targets all enemies." : "Choose " + panel.actionBattle().getEnemies().get(panel.inputTargetIndex()).getName() + " as the target.";
        String message = panel.inputChoosingItem() ? "Choose a consumable." : targetMessage != null ? targetMessage
                : panel.inputNoticeValue() != null ? panel.inputNoticeValue() : panel.actionBattle().getMessage();
        panel.hudLog().setText(message);
        panel.hudLog().setCaretPosition(0);
        BattleControls.updateParty(panel.actionBattle(), panel.hudPartyStatus());
        BattleControls.updateBars(panel.actionBattle(), panel.hudBars(), panel.actionPoints());
        boolean canChoose = !over && !panel.actionBattle().isActionAnimating() && panel.inputTargeting() == TargetAction.NONE && !panel.inputChoosingItem() && panel.actionBattle().getCurrent() != null;
        panel.inputAttack().setEnabled(canChoose); panel.inputAbility().setEnabled(canChoose); panel.inputItem().setEnabled(canChoose);
        panel.inputDefend().setEnabled(canChoose && panel.actionBattle().canUseDefendOrEscape());
        panel.inputEscape().setEnabled(canChoose && !panel.actionBattle().isEscaping() && panel.actionBattle().canUseDefendOrEscape());
        if (canChoose && !panel.inputCommands()[panel.inputCommand()].isEnabled()) panel.inputCommand(0);
        panel.inputFinish().setVisible(over && !panel.actionBattle().isVictory());
        panel.inputFinish().setText(panel.actionBattle().isVictory() ? "Continue" : "Return to Menu");
        panel.inputUpdateCommands();
        panel.inputScene().repaint();
    }

    private void refreshStatus() {
        boolean over = panel.actionBattle().isOver();
        String hint = panel.actionBattle().hasPendingLooperHits() ? "Looper Ring strikes..." : panel.actionBattle().hasPendingSecondCast() ? "Second cast incoming..." : panel.inputChoosingItem() ? "Up/Down Choose panel.inputItem()  •  Enter Confirm  •  Esc Cancel" : panel.inputTargeting() == TargetAction.NONE ? "Up/Down Move  •  Enter Select"
                : panel.inputTargetsParty() ? "Left/Right Choose ally  •  Enter Confirm  •  Esc Cancel"
                : panel.inputTargetsAllEnemies() ? "Enter Confirm  •  Esc Cancel" : "Left/Right Choose enemy  •  Enter Confirm  •  Esc Cancel";
        panel.hudStatus().setText(over ? (panel.actionBattle().isVictory() ? (panel.actionBattle().isMerchantEncounter() || panel.viewExplore() == null
                ? "VICTORY  •  Enter: Camp" : "VICTORY  •  Left/Right: Choose  •  Enter: Confirm") : "GAME OVER  •  Enter: Return to Menu")
                : (panel.actionBattle().getCurrent() == null ? "Waiting for AP" : panel.actionBattle().getCurrent().getName() + " is ready")
                + (panel.actionBattle().isEscaping() ? "  |  Escaping: " + panel.actionBattle().getEscapeInitiator().getName() + " (" + panel.actionBattle().getEscapeTurnsRemaining() + ")"
                    : "  |  Kits: " + panel.actionBattle().getKits() + "  Flowers: " + panel.inputProgress().getInventory().getWhiteFlowers()) + "  |  " + hint);
    }

}
