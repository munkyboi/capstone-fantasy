package game.model;

import java.util.List;

/** Applies battle consumables to eligible party members. */
final class BattleItems {
    private final Battle battle;
    private final GameProgress progress;
    private final List<Student> party;
    private final List<BattleEffect> effects;
    BattleItems(Battle battle, GameProgress progress, List<Student> party, List<BattleEffect> effects) {
        this.battle = battle; this.progress = progress; this.party = party; this.effects = effects;
    }
    boolean useKit(Student target) {
        if (battle.isActionAnimating() || battle.isOver() || battle.getCurrent() == null || !battle.validAlly(target)) { return false; }
        int before = target.getHp();
        if (!progress.useKit(target)) { return false; }
        effects.clear();
        int healed = target.getHp() - before;
        effects.add(new BattleEffect(target, healed));
        battle.finish(battle.getCurrent().getName() + " uses a First-Aid Kit. " + target.getName()
                + " recovers " + healed + " HP.");
        return true;
    }
    /** Uses a White Flower on a fallen party member.
     * @param target target of the action
     * @return the result of use white flower
     */
    boolean useWhiteFlower(Student target) {
        if (battle.isActionAnimating() || battle.isOver() || battle.getCurrent() == null || target == null || !party.contains(target) || target.isAlive()) return false;
        int restored = progress.getInventory().useWhiteFlower(target);
        if (restored <= 0) return false;
        battle.clearKnockedOutHeroEffects(target);
        effects.clear(); effects.add(new BattleEffect(target, restored));
        battle.finish(battle.getCurrent().getName() + " uses White Flower. " + target.getName() + " returns with " + restored + " HP.");
        return true;
    }
}
