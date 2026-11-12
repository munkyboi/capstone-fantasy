import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Verifies difficulty scaling, stacked XP bonuses, and reset behavior. */
public final class DifficultySettingsTest {
    private static void check(boolean ok, String text) { if (!ok) throw new AssertionError(text); }
    private static void key(SettingsPanel panel, String action) { panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, "")); }
    public static void main(String[] args) throws Exception {
        int[] hp = {49, 65, 98, 130}, speed = {2, 3, 5, 6}, threshold = {48, 36, 24, 18};
        int[] xp = {15, 20, 30, 40}, damage = {9, 12, 18, 24};
        for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) {
            int i = difficulty.ordinal(); Enemy wisp = new Enemy("Library Wisp A", false, false, difficulty);
            check(wisp.getMaxHp() == hp[i] && wisp.getHp() == hp[i] && wisp.getSpeed() == speed[i]
                    && wisp.getActionThreshold() == threshold[i] && wisp.getExperienceDrop() == xp[i]
                    && wisp.scaleDamage(12) == damage[i], "monster scaling " + difficulty);
            Enemy boss = new Enemy("Crystal Guardian", true, true, difficulty);
            check(boss.getMaxHp() == Math.round(260 * difficulty.multiplier())
                    && boss.getExperienceDrop() == Math.round(120 * difficulty.multiplier()), "boss scaling " + difficulty);
        }
        GameProgress normalProgress = new GameProgress(List.of(0));
        GameProgress hardProgress = new GameProgress(List.of(0));
        Battle normal = new Battle(normalProgress, new Random(8), false, 0, GameSettings.Difficulty.NORMAL);
        Battle hard = new Battle(hardProgress, new Random(8), false, 0, GameSettings.Difficulty.HARD);
        int before = normalProgress.getParty().get(0).getHp(); normal.enemyAttack(normal.getEnemies().get(0));
        int normalDamage = before - normalProgress.getParty().get(0).getHp();
        before = hardProgress.getParty().get(0).getHp(); hard.enemyAttack(hard.getEnemies().get(0));
        check(before - hardProgress.getParty().get(0).getHp() == Math.round(normalDamage * 1.5), "actual attack scales after defense");
        Path dir = Files.createTempDirectory("difficulty-settings-");
        try {
            GameSettings settings = new GameSettings();
            check(settings.getDifficulty() == GameSettings.Difficulty.NORMAL && settings.getXpBoost() == GameSettings.XpBoost.NORMAL, "new settings default normal");
            settings.setDifficulty(GameSettings.Difficulty.HARD); settings.setXpBoost(GameSettings.XpBoost.ULTRA);
            Path file = dir.resolve("settings.properties"); settings.save(file);
            GameSettings loaded = GameSettings.load(file);
            check(loaded.getDifficulty() == GameSettings.Difficulty.HARD && loaded.getXpBoost() == GameSettings.XpBoost.ULTRA, "new settings persist");
            Files.writeString(file, "battleSpeed=NORMAL\n");
            loaded = GameSettings.load(file); check(loaded.getDifficulty() == GameSettings.Difficulty.NORMAL && loaded.getXpBoost() == GameSettings.XpBoost.NORMAL, "older settings retain normal defaults");
            GameSettings stack = new GameSettings(); stack.setDifficulty(GameSettings.Difficulty.HARD); stack.setXpBoost(GameSettings.XpBoost.ULTRA);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    GameProgress progress = new GameProgress(List.of(0, 1, 2));
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, stack, new Random(2)); panel.start(progress, false, 0);
                    var field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true); Battle battle = (Battle) field.get(panel);
                    int base = battle.getEnemies().stream().mapToInt(Enemy::getExperienceDrop).sum();
                    check(battle.getEnemies().stream().allMatch(e -> e.getMaxHp() == 98
                            && e.getActionThreshold() == Math.round(new Enemy(e.getName()).getActionThreshold() / 1.5)), "battle uses difficulty");
                    battle.getEnemies().forEach(e -> e.takeDamage(9999));
                    var refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                    int expected = (int) Math.round(base * 1.5 * 5);
                    var shown = BattlePanel.class.getDeclaredField("victoryExperience"); shown.setAccessible(true);
                    check(shown.getInt(panel) == expected && progress.getParty().stream().allMatch(s -> s.getExperience() == expected), "difficulty, party bonus and boost stack");
                    var timer = BattlePanel.class.getDeclaredField("victoryTimer"); timer.setAccessible(true); ((javax.swing.Timer) timer.get(panel)).stop();
                    SettingsPanel ui = new SettingsPanel(stack, file, () -> {});
                    key(ui, "settings-up"); key(ui, "settings-confirm");
                    check(stack.getBattleSpeed() == GameSettings.BattleSpeed.NORMAL && stack.getApPerSecond() == 10
                            && stack.getGrowthFactor() == 7 && stack.getLevelCap() == 30 && stack.getPartySize() == 3
                            && stack.getDifficulty() == GameSettings.Difficulty.NORMAL && stack.getXpBoost() == GameSettings.XpBoost.NORMAL, "reset button restores every option");
                    key(ui, "settings-back"); GameSettings reset = GameSettings.load(file);
                    check(reset.getDifficulty() == GameSettings.Difficulty.NORMAL && reset.getXpBoost() == GameSettings.XpBoost.NORMAL, "reset is saved on exit");
                    SettingsPanel sliders = new SettingsPanel(reset, file, () -> {});
                    for (int i = 0; i < 4; i++) key(sliders, "settings-down");
                    key(sliders, "settings-right"); key(sliders, "settings-right");
                    key(sliders, "settings-down"); key(sliders, "settings-right"); key(sliders, "settings-right");
                    key(sliders, "settings-back"); GameSettings selected = GameSettings.load(file);
                    check(selected.getXpBoost() == GameSettings.XpBoost.ULTRA && selected.getDifficulty() == GameSettings.Difficulty.INSANE,
                            "XP boost and difficulty sliders save keyboard selections");
                } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
            });
        } finally { try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
        System.out.println("Difficulty, XP boost and reset checks passed");
    }
}
