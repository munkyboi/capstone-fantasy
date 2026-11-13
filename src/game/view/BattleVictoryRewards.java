package game.view;

import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.GameSettings;
import game.model.Student;
import java.util.List;
import java.util.Random;

/** Awards a victorious encounter once and prepares its summary. */
final class BattleVictoryRewards {
    private BattleVictoryRewards() {}
    record Result(List<String> items, int petot, int experience,
                  List<String> leveledStudents, List<String> bonusXpStudents) {}
    static Result award(Battle battle, GameProgress progress, int encounterStage, Random battleRandom, GameSettings settings) {
        java.util.List<String> rewards = new java.util.ArrayList<>();
        if (battle.isMerchantEncounter()) progress.unlockCarbonMarket();
        else { rewards.addAll(progress.claimStageRewardItems(encounterStage)); rewards.addAll(game.model.MonsterLoot.award(battle.getRewardEnemies(), progress, battleRandom, settings.getDifficulty())); }
        List<String> rewardItems = List.copyOf(rewards);
        int victoryPetot = progress.addPetot(battle.victoryPetot(settings.isGreedIsGood()));
        int baseExperience = battle.getRewardEnemies().stream().filter(Enemy::isDefeated).mapToInt(Enemy::getExperienceDrop).sum();
        double multiplier = switch (progress.getParty().size()) {
            case 1 -> 3.0; case 2 -> 2.0; case 3 -> 1.5; case 4 -> 1.0;
            default -> throw new IllegalStateException("Invalid party size");
        };
        int victoryExperience = (int) Math.round(baseExperience * multiplier * settings.getXpBoost().multiplier());
        java.util.List<String> leveled = new java.util.ArrayList<>();
        java.util.List<String> bonus = new java.util.ArrayList<>();
        for (Student student : progress.getParty()) {
            boolean boosted = "designer_leather_vest".equals(student.getEquipmentId(game.model.Equipment.Slot.ARMOR));
            if (boosted) bonus.add(student.getName() + " +" + victoryExperience);
            if (student.gainExperience((long) victoryExperience * (boosted ? 2 : 1)) > 0) { leveled.add(student.getName()); }
        }
        List<String> bonusXpStudents = List.copyOf(bonus);
        List<String> leveledStudents = List.copyOf(leveled);
        return new Result(rewardItems, victoryPetot, victoryExperience, leveledStudents, bonusXpStudents);
    }
}
