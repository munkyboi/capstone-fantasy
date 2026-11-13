package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.Student;
import javax.swing.JButton;
import static game.view.BattlePanel.TargetAction;

/** Handles keyboard commands, target selection, and victory choices. */
final class BattleInput {
    private static final double AP_EPSILON = 0.0000001;
    private final BattlePanel panel;
    BattleInput(BattlePanel panel) { this.panel = panel; }
    void bind() {
        panel.inputAttack().addActionListener(e -> {
            Student actor = battle().getCurrent();
            if (actor != null && actor.getId() == Student.CHARICE && actor.getSp() < 2) {
                panel.inputNotice("Not enough SP. Twin Shot needs 2 SP."); panel.refresh(); return;
            }
            beginTargeting(TargetAction.ATTACK);
        });
        panel.inputEscape().addActionListener(e -> panel.performAction(() -> battle().beginEscape(), false));
        panel.inputDefend().addActionListener(e -> beginTargeting(TargetAction.DEFEND));
        panel.inputAbility().addActionListener(e -> {
            Student actor = battle().getCurrent();
            if (actor != null && actor.getSp() < actor.getAbilitySpCost()) {
                panel.inputNotice("Not enough SP. " + actor.getName() + " needs " + actor.getAbilitySpCost() + " SP."); panel.refresh(); return;
            }
            beginTargeting(TargetAction.ABILITY);
        });
        panel.inputItem().addActionListener(e -> {
            int kits = battle().getKits(), flowers = panel.inputProgress().getInventory().getWhiteFlowers();
            if (kits == 0 && flowers == 0) { panel.inputNotice("No usable items remain."); panel.refresh(); return; }
            boolean fallen = battle().getParty().stream().anyMatch(student -> !student.isAlive());
            if (flowers == 0 || (kits > 0 && !fallen)) { beginTargeting(TargetAction.ITEM); return; }
            if (kits == 0) { panel.beginFlowerTargeting(); return; }
            panel.inputChoosingItem(true); panel.inputItemChoice(0); panel.inputNotice(null); panel.refresh();
        });
        panel.inputFinish().addActionListener(e -> panel.inputFinishAction());
        Keys.bind(panel, "UP", "battle-up", () -> moveCursor(-1));
        Keys.bind(panel, "DOWN", "battle-down", () -> moveCursor(1));
        Keys.bind(panel, "LEFT", "battle-left", () -> moveTarget(-1));
        Keys.bind(panel, "RIGHT", "battle-right", () -> moveTarget(1));
        Keys.bind(panel, "ENTER", "battle-confirm", this::confirmCommand);
        Keys.bind(panel, "SPACE", "battle-confirm-space", this::confirmCommand);
        Keys.bind(panel, "ESCAPE", "battle-cancel", this::cancelTargeting);
        Keys.bind(panel, "PAGE_DOWN", "log-down", () -> panel.inputScroll(1));
        Keys.bind(panel, "PAGE_UP", "log-up", () -> panel.inputScroll(-1));
    }
    private Battle battle() { return panel.actionBattle(); }
    private boolean valid(Student student) { return BattleTargetRules.validPartyTarget(battle(), panel.inputTargeting(), student); }
    boolean targetsParty() { return BattleTargetRules.targetsParty(battle(), panel.inputTargeting()); }
    boolean targetsAllEnemies() { return BattleTargetRules.targetsAllEnemies(battle(), panel.inputTargeting()); }
    void moveCursor(int direction) {
        if (battle() == null || battle().isOver() || battle().isActionAnimating()) return;
        if (panel.inputChoosingItem()) { panel.inputItemChoice(Math.floorMod(panel.inputItemChoice() + direction, 2)); panel.refresh(); return; }
        if (panel.inputTargeting() != TargetAction.NONE) { moveTarget(direction); return; }
        JButton[] commands = panel.inputCommands();
        if (java.util.Arrays.stream(commands).noneMatch(JButton::isEnabled)) return;
        do { panel.inputCommand(Math.floorMod(panel.inputCommand() + direction, commands.length)); }
        while (!commands[panel.inputCommand()].isEnabled());
        updateCommandDisplay();
    }
    void updateCommandDisplay() {
        BattleControls.updateCommands(panel.inputCommands(), panel.inputCommand());
        if (battle() != null && battle().getCurrent() != null && !battle().isOver()) {
            if (battle().getCurrent().getId() == Student.CHARICE) panel.inputAttack().setText("Twin Shot 2 SP");
            panel.inputAbility().setText("Ability " + battle().getCurrent().getAbilitySpCost() + " SP");
        }
    }
    void beginTargeting(TargetAction action) {
        if (battle() == null || battle().isOver() || battle().isActionAnimating() || battle().getCurrent() == null || panel.inputTargeting() != TargetAction.NONE
                || panel.actionPoints().get(battle().getCurrent()) + AP_EPSILON < battle().getCurrent().getActionThreshold()) return;
        panel.inputNotice(null); panel.inputTargeting(action); panel.inputTargetIndex(0);
        if (targetsParty()) {
            while (!valid(battle().getParty().get(panel.inputTargetIndex()))) panel.inputTargetIndex(panel.inputTargetIndex() + 1);
        } else {
            while (!battle().getEnemies().get(panel.inputTargetIndex()).isAlive()) panel.inputTargetIndex(panel.inputTargetIndex() + 1);
        }
        panel.refresh();
    }
    void moveTarget(int direction) {
        if (battle() != null && battle().isVictory() && !battle().isMerchantEncounter() && panel.inputCanExplore()) {
            panel.inputVictoryChoice(Math.floorMod(panel.inputVictoryChoice() + direction, 2)); panel.inputScene().repaint(); return;
        }
        if (battle() == null || battle().isOver() || panel.inputTargeting() == TargetAction.NONE || targetsAllEnemies()) return;
        int size = targetsParty() ? battle().getParty().size() : battle().getEnemies().size();
        do { panel.inputTargetIndex(Math.floorMod(panel.inputTargetIndex() + direction, size)); }
        while (targetsParty() ? !valid(battle().getParty().get(panel.inputTargetIndex())) : !battle().getEnemies().get(panel.inputTargetIndex()).isAlive());
        panel.refresh();
    }
    void cancelTargeting() {
        if (panel.inputChoosingItem()) { panel.inputChoosingItem(false); panel.refresh(); }
        else if (panel.inputTargeting() != TargetAction.NONE) { panel.inputTargeting(TargetAction.NONE); panel.inputNotice(null); panel.refresh(); }
    }
    void confirmCommand() {
        if (battle() == null || battle().isActionAnimating()) return;
        if (battle().isOver()) {
            if (battle().isVictory() && panel.inputVictoryChoice() == 0 && !battle().isMerchantEncounter() && panel.inputCanExplore()) panel.inputExplore();
            else panel.inputFinish().doClick();
        } else if (panel.inputChoosingItem()) {
            panel.inputChoosingItem(false);
            if (panel.inputItemChoice() == 0) beginTargeting(TargetAction.ITEM); else panel.beginFlowerTargeting();
        } else if (panel.inputTargeting() != TargetAction.NONE) {
            Student target = targetsParty() ? battle().getParty().get(panel.inputTargetIndex()) : null;
            Enemy enemy = !targetsParty() ? battle().getEnemies().get(panel.inputTargetIndex()) : null;
            TargetAction action = panel.inputTargeting(); panel.inputTargeting(TargetAction.NONE); panel.inputNotice(null);
            boolean attacking = action == TargetAction.ATTACK || (action == TargetAction.ABILITY && battle().getCurrent().getAbilityType() != Student.AbilityType.HEALING_KISS);
            panel.performAction(() -> {
                if (action == TargetAction.ATTACK) battle().attack(enemy);
                else if (action == TargetAction.ABILITY) {
                    boolean fallen = target != null && !target.isAlive();
                    if (battle().ability(target, enemy)) { if (fallen) panel.resetStudentAp(target); panel.announceAbility(); }
                } else if (action == TargetAction.DEFEND) battle().defend(target);
                else if (action == TargetAction.WHITE_FLOWER) { if (battle().useWhiteFlower(target)) panel.resetStudentAp(target); }
                else if (!battle().useKit(target)) panel.inputNotice(target.getName() + " already has full HP.");
            }, attacking);
        } else if (battle().getCurrent() != null) panel.inputCommands()[panel.inputCommand()].doClick();
    }
}
