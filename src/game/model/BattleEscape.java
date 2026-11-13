package game.model;

import java.util.List;

/** Tracks the party escape attempt and applies its penalties. */
final class BattleEscape {
    private final Battle battle;
    private final List<Student> party;
    private final GameProgress progress;
    private Student initiator;
    private int turnsRemaining;
    BattleEscape(Battle battle, List<Student> party, GameProgress progress) { this.battle = battle; this.party = party; this.progress = progress; }
    boolean isEscaping() { return initiator != null; }
    Student initiator() { return initiator; }
    int turnsRemaining() { return turnsRemaining; }
    boolean begin() {
        if (battle.isOver() || battle.isActionAnimating() || isEscaping() || !battle.canUseDefendOrEscape()) return false;
        initiator = battle.getCurrent(); turnsRemaining = 2;
        battle.finish(initiator.getName() + " leads the party's escape! Turns remaining: 2.");
        return true;
    }
    boolean cancelIfKnockedOut() {
        if (initiator == null || initiator.isAlive()) return false;
        battle.heroMessage(initiator.getName() + " was knocked out. Escape failed.");
        initiator = null; turnsRemaining = 0; return true;
    }
    boolean advance(Student student) {
        if (student == null || student != initiator || !student.isAlive() || turnsRemaining <= 0) return false;
        turnsRemaining--;
        if (turnsRemaining > 0) { battle.heroMessage("Escaping... Turns remaining: " + turnsRemaining + "."); return false; }
        initiator = null;
        for (Student hero : party) hero.applyEscapeXpPenalty();
        progress.spendPetot((progress.getPetot() + 9) / 10);
        battle.heroMessage("The party escaped!");
        return true;
    }
}
