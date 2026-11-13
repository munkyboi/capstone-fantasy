package game.view;

import game.model.Battle;
import game.model.Student;
import static game.view.BattlePanel.TargetAction;

/** Determines eligible targets for the selected battle command. */
final class BattleTargetRules {
    private BattleTargetRules() {}
    static boolean targetsParty(Battle battle, TargetAction action) {
        return action == TargetAction.ITEM || action == TargetAction.WHITE_FLOWER || action == TargetAction.DEFEND
                || (action == TargetAction.ABILITY && battle.getCurrent().getAbilityType() == Student.AbilityType.HEALING_KISS);
    }
    static boolean targetsAllEnemies(Battle battle, TargetAction action) {
        return action == TargetAction.ABILITY && (battle.getCurrent().getAbilityType() == Student.AbilityType.ARCANE_BURST
                || battle.getCurrent().getAbilityType() == Student.AbilityType.RAIN_OF_ARROWS);
    }
    static boolean validPartyTarget(Battle battle, TargetAction action, Student target) {
        return action == TargetAction.DEFEND ? battle.canDefend(target)
                : action == TargetAction.WHITE_FLOWER ? !target.isAlive()
                : action == TargetAction.ABILITY && battle.getCurrent().getAbilityType() == Student.AbilityType.HEALING_KISS || target.isAlive();
    }
}
