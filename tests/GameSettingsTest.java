import game.io.SaveStore;
import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Checks settings, progression migration, party sizes, and saved reserves. */
public final class GameSettingsTest {
    private static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    private static void key(javax.swing.JComponent c, String name) { c.getActionMap().get(name).actionPerformed(new ActionEvent(c, 0, name)); }
    public static void main(String[] args) throws Exception {
        GameSettings settings = new GameSettings();
        check(settings.getApPerSecond() == 10 && settings.getGrowthFactor() == 7 && settings.getLevelCap() == 30 && settings.getPartySize() == 3, "defaults");
        settings.setBattleSpeed(GameSettings.BattleSpeed.SLOW); check(settings.getApPerSecond() == 5, "slow AP");
        settings.setBattleSpeed(GameSettings.BattleSpeed.FAST); check(settings.getApPerSecond() == 15, "fast AP");
        settings.setBattleSpeed(GameSettings.BattleSpeed.ULTRA); check(settings.getApPerSecond() == 20, "ultra AP");
        Student student = Student.create(0); student.configureProgression(7, 30);
        check(student.getXpRequired() == 100 && student.gainExperience(100) == 1 && student.getXpRequired() == 107, "growing XP curve");
        student.gainExperience(300); int level = student.getLevel(); int hp = student.getHp();
        student.configureProgression(20, 20); check(student.getLevel() == level && student.getHp() == hp, "change curve preserves progress");
        Student overCap = Student.create(1); overCap.restoreExperience(2900); overCap.configureProgression(7, 20);
        int xp = overCap.getExperience(); check(overCap.getLevel() == 30 && overCap.gainExperience(500) == 0 && overCap.getExperience() == xp, "lower cap preserves level");
        overCap.configureProgression(7, 40); check(overCap.gainExperience(500000) > 0 && overCap.getLevel() == 40, "raised cap permits levels");
        Path dir = Files.createTempDirectory("game-settings-");
        try {
            settings.setGrowthFactor(15); settings.setPartySize(4); settings.setLevelCap(50); Path file = dir.resolve("settings.properties"); settings.save(file);
            GameSettings loaded = GameSettings.load(file); check(loaded.getGrowthFactor() == 15 && loaded.getPartySize() == 4 && loaded.getLevelCap() == 50 && loaded.getApPerSecond() == 20, "settings persist");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, loaded, new Random(1));
                    GameProgress progress = new GameProgress(List.of(0, 1, 2)); panel.start(progress, true, 0);
                    var advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                    advance.invoke(panel, 0.1);
                    var points = BattlePanel.class.getDeclaredField("actionPoints"); points.setAccessible(true);
                    var values = (Map<?, ?>) points.get(panel);
                    check(progress.getParty().stream().allMatch(s -> Math.abs((double) values.get(s) - 2.0) < 0.0001), "ultra fills 2 AP in 0.1 seconds");
                    var timer = BattlePanel.class.getDeclaredField("apTimer"); timer.setAccessible(true);
                    check(((javax.swing.Timer) timer.get(panel)).getDelay() == 50, "ultra preserves animation frame interval");
                    ((javax.swing.Timer) timer.get(panel)).stop();
                } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
            });
            GameSettings edited = new GameSettings(); Path uiFile = dir.resolve("ui-settings.properties");
            SwingUtilities.invokeAndWait(() -> {
                SettingsPanel ui = new SettingsPanel(edited, uiFile, () -> {});
                key(ui, "settings-right"); key(ui, "settings-right"); key(ui, "settings-down"); key(ui, "settings-right");
                key(ui, "settings-down"); key(ui, "settings-right"); key(ui, "settings-down"); key(ui, "settings-right");
                key(ui, "settings-confirm");
            });
            GameSettings uiSaved = GameSettings.load(uiFile);
            check(uiSaved.getApPerSecond() == 20 && uiSaved.getGrowthFactor() == 8 && uiSaved.getLevelCap() == 40
                    && uiSaved.getPartySize() == 4, "keyboard settings persist");
            SwingUtilities.invokeAndWait(() -> {
                List<List<Integer>> chosen = new ArrayList<>();
                SelectionPanel selection = new SelectionPanel(() -> {}, chosen::add);
                selection.configure(1, List.of(0, 1, 2));
                key(selection, "party-confirm"); key(selection, "party-right"); key(selection, "party-confirm");
                key(selection, "party-continue"); key(selection, "party-confirm");
                check(chosen.size() == 1 && chosen.get(0).equals(List.of(2)), "choose active party when shrinking");
            });
            for (int size = 1; size <= 4; size++) {
                List<Integer> ids = java.util.stream.IntStream.range(0, size).boxed().toList();
                GameProgress p = new GameProgress(ids); p.applySettings(settings); p.advance();
                Battle battle = new Battle(p, new Random(2)); check(battle.getParty().size() == size, "battle size " + size);
                SwingUtilities.invokeAndWait(() -> {
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, settings); panel.setSize(960, 540); panel.start(p, false, 0);
                    var image = new java.awt.image.BufferedImage(960,540,2); var g = image.createGraphics(); try { panel.doLayout(); panel.paint(g); } finally { g.dispose(); }
                    InventoryPanel inventory = new InventoryPanel(() -> {}); inventory.open(p);
                });
                SaveStore store = new SaveStore(dir);
                for (SaveStore.Format format : SaveStore.Format.values()) {
                    store.save(1, format, p); GameProgress result = store.load(1, format);
                    check(result.getParty().size() == size && result.getParty().get(0).getExperienceGrowth() == 15, "saved party " + size + " " + format);
                }
            }
            GameProgress party = new GameProgress(List.of(0, 1, 2)); party.advance(); party.applySettings(settings);
            Student resting = party.getParty().stream().filter(s -> s.getId() == 2).findFirst().orElseThrow();
            resting.gainExperience(250); resting.allocateStatPoint(Student.Stat.ATK); int reserveXp = resting.getExperience();
            party.setActiveParty(List.of(0));
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(2, format, party); GameProgress saved = store.load(2, format);
                check(saved.getParty().size() == 1 && saved.getRoster().size() == 3, "reserve retained " + format);
                saved.setActiveParty(List.of(0, 2)); Student recovered = saved.getParty().stream().filter(s -> s.getId() == 2).findFirst().orElseThrow();
                check(recovered.getExperience() == reserveXp && recovered.getAllocatedStatPoints(Student.Stat.ATK) == 1, "reserve stats retained " + format);
            }
            GameProgress high = new GameProgress(List.of(0)); high.advance(); high.applySettings(settings);
            Student veteran = high.getParty().get(0); veteran.gainExperience(Integer.MAX_VALUE);
            veteran.configureProgression(7, 20); int veteranXp = veteran.getExperience();
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(3, format, high); Student restored = store.load(3, format).getParty().get(0);
                check(restored.getLevel() == 50 && restored.getLevelCap() == 20 && restored.getExperience() == veteranXp,
                        "above-cap veteran survives save " + format);
            }
        } finally { try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
        System.out.println("Game settings and variable party checks passed");
    }
}
