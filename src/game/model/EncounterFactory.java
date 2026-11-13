package game.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Creates the enemies and opening message for one campus-route encounter. */
final class EncounterFactory {
    private EncounterFactory() { }
    record Encounter(List<Enemy> enemies, String message) { }

    static Encounter create(int encounter, Random random, GameSettings.Difficulty difficulty,
            double averageLevel, int partySize, boolean ambushed) {
        List<Enemy> enemies = new ArrayList<>();
        int count = EncounterRules.rollMonsterCount(encounter, random, difficulty);
        boolean finalEncounter = encounter == GameProgress.FINAL_BOSS_ENCOUNTER;
        ambushed = ambushed && !finalEncounter;
        int sentinels = finalEncounter ? 5 : encounter == 3 ? 1 + random.nextInt(3) : encounter >= 1 ? 1 : 0;
        int wisps = count - sentinels - (finalEncounter ? 1 : 0);
        for (int index = 0; index < wisps; index++) {
            String name = "Library Wisp " + (char) ('A' + index);
            enemies.add(encounter == 0 && partySize == 1 ? Enemy.soloOpeningWisp(name, difficulty, averageLevel)
                    : new Enemy(name, false, false, difficulty, encounter, averageLevel));
        }
        for (int index = 0; index < sentinels; index++) {
            boolean attack = finalEncounter ? index >= 2 : index % 2 == 1;
            int variantIndex = finalEncounter ? (attack ? index - 2 : index) : index / 2;
            String name = (attack ? "Attack Sentinel " : "Support Sentinel ") + (char) ('A' + variantIndex);
            enemies.add(Enemy.sentinel(name, attack, difficulty, encounter, averageLevel, finalEncounter ? partySize : 0));
        }
        if (finalEncounter) { enemies.add(Enemy.finalOpponent("Crystal Guardian", true, difficulty, averageLevel, partySize)); }
        boolean goblinSpawned = !finalEncounter && random.nextInt(100) >= 95;
        if (goblinSpawned) {
            for (int index = enemies.size() - 1; index >= 0; index--) {
                if (!enemies.get(index).isSentinel()) { enemies.remove(index); wisps--; break; }
            }
            enemies.add(Enemy.itmigims(difficulty, encounter, averageLevel));
        }
        String message = finalEncounter ? "The Crystal Guardian and five Sentinels appear!"
                : sentinels > 0 ? sentinels + " Stone Sentinel(s)" + (wisps > 0 ? " and " + wisps + " Library Wisp(s) appear!" : " appear!")
                : wisps > 0 ? wisps + " Library Wisp(s) appear! Choose an action."
                : "Itmigims appears! Choose an action.";
        if (goblinSpawned && (wisps > 0 || sentinels > 0)) { message += "\nItmigims appears!"; }
        if (ambushed) { message = "You have been ambushed!\n" + message; }
        enemies.sort(Comparator.comparingInt(Enemy::getSpeed).reversed());
        return new Encounter(enemies, message);
    }
}
