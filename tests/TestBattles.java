import game.model.GameProgress;
import game.model.GameSettings;
import game.view.BattlePanel;
import java.util.Random;
import java.util.function.Consumer;

/** Creates predictable two-monster battles for unrelated UI regression checks. */
final class TestBattles {
    private TestBattles() { }
    static BattlePanel twoEnemies(Runnable onDefeat) {
        return twoEnemies(onDefeat, progress -> onDefeat.run(), new GameSettings());
    }
    static BattlePanel twoEnemies(Runnable onDefeat, Consumer<GameProgress> onVictory, GameSettings settings) {
        Random random = new Random(12) {
            @Override public int nextInt(int bound) { return bound == 3 ? 1 : super.nextInt(bound); }
        };
        return new BattlePanel(onDefeat, onVictory, settings, random);
    }
}
