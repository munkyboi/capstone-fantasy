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
        int[] growthRates = {1, 7, 15, 30};
        for (GameSettings.Difficulty difficulty : GameSettings.Difficulty.values()) {
            GameSettings choice = new GameSettings(); choice.setDifficulty(difficulty);
            check(choice.getGrowthFactor() == growthRates[difficulty.ordinal()], "difficulty sets growth " + difficulty);
            Student hero = Student.create(0); hero.configureProgression(choice.getGrowthFactor(), 100);
            check(hero.gainExperience(100) == 1 && hero.getXpRequired() == 100 + growthRates[difficulty.ordinal()], "XP curve " + difficulty);
        }
        settings.setBattleSpeed(GameSettings.BattleSpeed.SLOW); check(settings.getApPerSecond() == 5, "slow AP");
        settings.setBattleSpeed(GameSettings.BattleSpeed.FAST); check(settings.getApPerSecond() == 15, "fast AP");
        settings.setBattleSpeed(GameSettings.BattleSpeed.ULTRA); check(settings.getApPerSecond() == 20, "ultra AP");
        check(settings.isAmbushEnabled(), "ambush defaults on");
        settings.setAmbushEnabled(false);
        check(!settings.rollAmbush(0, new Random() { @Override public double nextDouble() { return 0; } }), "disabled ambush never occurs");
        for (int cap = 20; cap <= 100; cap += 10) {
            GameSettings choices = new GameSettings(); choices.setLevelCap(cap);
            Student eligible = Student.create(0); eligible.configureProgression(7, cap);
            check(choices.getLevelCap() == cap && eligible.getLevelCap() == cap, "level cap choice " + cap);
        }
        Student student = Student.create(0); student.configureProgression(7, 30);
        check(student.getXpRequired() == 100 && student.gainExperience(100) == 1 && student.getXpRequired() == 107, "growing XP curve");
        student.gainExperience(300); int level = student.getLevel(); int hp = student.getHp();
        student.configureProgression(20, 20); check(student.getLevel() == level && student.getHp() == hp, "change curve preserves progress");
        Student overCap = Student.create(1); overCap.restoreExperience(2900); overCap.configureProgression(7, 20);
        long xp = overCap.getExperience(); check(overCap.getLevel() == 30 && overCap.gainExperience(500) == 0 && overCap.getExperience() == xp, "lower cap preserves level");
        overCap.configureProgression(7, 40); check(overCap.gainExperience(500000) > 0 && overCap.getLevel() == 40, "raised cap permits levels");
        Student maxGrowth = Student.create(0); maxGrowth.configureProgression(20, 100);
        check(maxGrowth.gainExperience(Long.MAX_VALUE) == 99 && maxGrowth.getLevel() == 100
                && maxGrowth.getExperience() > Integer.MAX_VALUE && maxGrowth.getXpRequired() == 0, "level 100 supports full growth curve");
        maxGrowth.configureProgression(7, 20);
        check(maxGrowth.getLevel() == 100 && maxGrowth.gainExperience(1) == 0, "lower cap preserves level 100");
        Path dir = Files.createTempDirectory("game-settings-");
        try {
            settings.setDifficulty(GameSettings.Difficulty.HARD); settings.setPartySize(4); settings.setLevelCap(50); Path file = dir.resolve("settings.properties"); settings.save(file);
            GameSettings loaded = GameSettings.load(file); check(loaded.getGrowthFactor() == 15 && loaded.getPartySize() == 4 && loaded.getLevelCap() == 50 && loaded.getApPerSecond() == 20 && !loaded.isAmbushEnabled(), "settings persist");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, loaded, new Random(1));
                    GameProgress progress = new GameProgress(List.of(0, 1, 2)); panel.start(progress, true, 0);
                    var ambushField = BattlePanel.class.getDeclaredField("showAmbush"); ambushField.setAccessible(true);
                    check(!ambushField.getBoolean(panel), "disabled setting prevents forced ambush");
                    loaded.setAmbushEnabled(true); panel.start(progress, true, 0);
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
                key(ui, "settings-down"); key(ui, "settings-right");
                key(ui, "settings-confirm");
            });
            GameSettings uiSaved = GameSettings.load(uiFile);
            check(uiSaved.getApPerSecond() == 20 && uiSaved.getGrowthFactor() == 7 && uiSaved.getLevelCap() == 40
                    && uiSaved.getPartySize() == 4, "keyboard settings persist");
            SwingUtilities.invokeAndWait(() -> {
                SettingsPanel ui = new SettingsPanel(edited, uiFile, () -> {});
                key(ui, "settings-down");
                for (int i = 0; i < 6; i++) key(ui, "settings-right");
                check(edited.getLevelCap() == 100, "settings allow level 100");
            });
            SwingUtilities.invokeAndWait(() -> {
                GameSettings toggle = new GameSettings(); SettingsPanel ui = new SettingsPanel(toggle, uiFile, () -> {});
                for (int i = 0; i < 5; i++) key(ui, "settings-down");
                key(ui, "settings-confirm"); check(!toggle.isAmbushEnabled(), "enter toggles ambush off");
                key(ui, "settings-back"); check(!GameSettings.load(uiFile).isAmbushEnabled(), "ambush toggle saves");
                toggle.resetDefaults(); check(toggle.isAmbushEnabled(), "reset enables ambush");
            });
            Path oldSettings = dir.resolve("old-settings.properties");
            Files.writeString(oldSettings, "levelCap=40\npartySize=4\n");
            GameSettings migrated = GameSettings.load(oldSettings);
            check(migrated.getLevelCap() == 40 && migrated.getPartySize() == 4 && migrated.isAmbushEnabled(), "legacy cap 40 and preferences remain unchanged");
            Files.writeString(oldSettings, "growthFactor=20\ndifficulty=INSANE\nlevelCap=40\npartySize=4\n");
            migrated = GameSettings.load(oldSettings);
            check(migrated.getDifficulty() == GameSettings.Difficulty.INSANE && migrated.getGrowthFactor() == 30 && migrated.getLevelCap() == 40, "old growth preference yields to saved difficulty");
            GameProgress migratedParty = new GameProgress(List.of(0, 1));
            Student existing = migratedParty.getParty().get(0); existing.configureProgression(7, 100); existing.gainExperience(1700);
            int existingLevel = existing.getLevel(); double existingFraction = (double) existing.getXpProgress() / existing.getXpRequired();
            migratedParty.applySettings(migrated);
            check(existing.getLevel() == existingLevel && existing.getExperienceGrowth() == 30 && Math.abs((double) existing.getXpProgress() / existing.getXpRequired() - existingFraction) < 0.01, "difficulty change preserves level and XP progress");
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
            resting.gainExperience(250); resting.allocateStatPoint(Student.Stat.ATK); long reserveXp = resting.getExperience();
            party.setActiveParty(List.of(0));
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(2, format, party); GameProgress saved = store.load(2, format);
                check(saved.getParty().size() == 1 && saved.getRoster().size() == 3, "reserve retained " + format);
                saved.setActiveParty(List.of(0, 2)); Student recovered = saved.getParty().stream().filter(s -> s.getId() == 2).findFirst().orElseThrow();
                check(recovered.getExperience() == reserveXp && recovered.getAllocatedStatPoints(Student.Stat.ATK) == 1, "reserve stats retained " + format);
            }
            GameProgress level100 = new GameProgress(List.of(0)); level100.advance();
            GameSettings maxSettings = new GameSettings(); maxSettings.setLevelCap(100); maxSettings.setDifficulty(GameSettings.Difficulty.INSANE);
            level100.applySettings(maxSettings); Student maxStudent = level100.getParty().get(0);
            maxStudent.gainExperience(Long.MAX_VALUE); maxStudent.allocateStatPoint(Student.Stat.SPD);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(3, format, level100); Student restored = store.load(3, format).getParty().get(0);
                check(restored.getLevel() == 100 && restored.getExperience() == maxStudent.getExperience()
                        && restored.getAllocatedStatPoints(Student.Stat.SPD) == 1, "level 100 save " + format);
            }
            GameProgress high = new GameProgress(List.of(0)); high.advance(); high.applySettings(settings);
            Student veteran = high.getParty().get(0); veteran.gainExperience(Integer.MAX_VALUE);
            veteran.configureProgression(7, 20); long veteranXp = veteran.getExperience();
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(3, format, high); Student restored = store.load(3, format).getParty().get(0);
                check(restored.getLevel() == 50 && restored.getLevelCap() == 20 && restored.getExperience() == veteranXp,
                        "above-cap veteran survives save " + format);
            }
        } finally { try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
        System.out.println("Game settings and variable party checks passed");
    }
}
