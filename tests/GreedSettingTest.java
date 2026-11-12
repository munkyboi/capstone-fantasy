import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Verifies the Petot multiplier, keyboard toggle, persistence and reset. */
public final class GreedSettingTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void key(JComponent panel, String name) { panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name)); }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("greed-setting-"); Path file = dir.resolve("settings.properties");
        try {
            GameSettings settings = new GameSettings(); check(!settings.isGreedIsGood(), "greed defaults off");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    SettingsPanel ui = new SettingsPanel(settings, file, () -> {});
                    for (int i = 0; i < 7; i++) key(ui, "settings-down");
                    key(ui, "settings-confirm"); check(settings.isGreedIsGood(), "enter toggles greed on");
                    key(ui, "settings-back"); check(GameSettings.load(file).isGreedIsGood(), "greed setting persists");
                    GameProgress progress = new GameProgress(List.of(0, 1, 2));
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, settings, new Random(3)); panel.start(progress, false, 0);
                    Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true); Battle battle = (Battle) field.get(panel);
                    int base = battle.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum();
                    battle.getEnemies().forEach(enemy -> enemy.takeDamage(9999));
                    Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                    check(progress.getPetot() == base * 10, "victory grants ten times Petot");
                    refresh.invoke(panel); check(progress.getPetot() == base * 10, "reward only granted once");
                    for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(panel)).stop(); }
                    settings.resetDefaults(); check(!settings.isGreedIsGood(), "reset turns greed off");
                } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
            });
        } finally { try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
        System.out.println("Greed setting checks passed");
    }
}
