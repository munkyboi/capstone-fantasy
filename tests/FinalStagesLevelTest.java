import game.io.SaveStore;
import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Verifies final stages, boss rules, experience, and late-game saves. */
public final class FinalStagesLevelTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    private static void key(JComponent panel, String name) { panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name)); }
    public static void main(String[] args) throws Exception {
        Student hero = new Student(Student.ZACK); hero.takeDamage(20);
        check(hero.gainExperience(99) == 0 && hero.getLevel() == 1, "no early level");
        hero.restoreSp(0);
        check(hero.gainExperience(201) == 3 && hero.getLevel() == 4 && hero.getMaxHp() == 75
                && hero.getBaseAttack() == 30 && hero.getBaseDefense() == 7 && hero.getHp() == hero.getMaxHp()
                && hero.getSp() == hero.getMaxSp(), "multiple levels grow stats and restore HP and SP");
        Student runner = new Student(Student.ZACK);
        int startingSpeed = runner.getSpeed();
        runner.gainExperience(400); check(runner.getSpeed() == startingSpeed, "speed stays stable through level five");
        runner.gainExperience(100); check(runner.getSpeed() == startingSpeed + 1, "speed grows at level six");
        runner.gainExperience(500); check(runner.getSpeed() == startingSpeed + 2, "speed grows again at level eleven");
        check(runner.allocateStatPoint(Student.Stat.SPD) && runner.getSpeed() == startingSpeed + 3, "allocated speed stacks with growth");
        hero.takeDamage(999); hero.gainExperience(100);
        check(hero.isAlive() && hero.getHp() == hero.getMaxHp() && hero.getSp() == hero.getMaxSp()
                && hero.getLevel() == 5, "level-up restores a KO student");
        hero.gainExperience(2499); check(hero.getLevel() == 29, "level before cap");
        hero.gainExperience(1); check(hero.getLevel() == 30, "level cap reached");
        hero.gainExperience(99999); check(hero.getLevel() == 30 && hero.getExperience() == 2900, "experience capped");
        GameProgress p = new GameProgress(List.of(0, 1, 2));
        p.claimStageRewardItems(); p.advance(); p.claimStageRewardItems(); p.advance(); p.claimStageRewardItems(); p.advance();
        for (int roll = 0; roll <= 2; roll++) {
            final int value = roll;
            Random random = new Random() { @Override public int nextInt(int bound) { return Math.min(value, bound - 1); } };
            Battle fourth = new Battle(p, random, false, 3);
            check(fourth.getEnemies().size() == 4 + roll && fourth.getEnemies().stream().filter(Enemy::isSentinel).count() == 1 + roll, "stage four count and sentinel range");
            check(fourth.getEnemies().stream().map(Enemy::getName).distinct().count() == fourth.getEnemies().size(), "stage four enemy names are unique");
        }
        p.claimStageRewardItems(3); p.advance();
        check(!new Battle(p, new Random(2), true, 4).getMessage().contains("ambushed"), "model enforces boss ambush exemption");
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> {}); panel.start(p, true, 4);
                Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true);
                Battle b = (Battle) field.get(panel);
                check(b.getEnemies().size() == 6 && b.getEnemies().stream().filter(Enemy::isBoss).count() == 1
                        && b.getEnemies().stream().filter(e -> e.isSentinel() && !e.isBoss()).count() == 5
                        && b.getEnemies().stream().map(Enemy::getName).distinct().count() == 6,
                        "final encounter has guardian and five distinct Sentinels");
                check(!b.getMessage().contains("ambushed"), "final boss cannot be ambushed");
                int xp = (int) Math.round(b.getEnemies().stream().mapToInt(Enemy::getExperienceDrop).sum() * 1.5);
                for (Enemy e : b.getEnemies()) { e.takeDamage(999); }
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                check(p.getParty().stream().allMatch(s -> s.getExperience() == xp), "each student earns boss experience");
                check(p.getInventory().getEquipmentIds().contains("campus_medal"), "boss stage reward");
                refresh.invoke(panel); check(p.getParty().stream().allMatch(s -> s.getExperience() == xp), "victory XP awarded once");
                check(p.advance() && p.getEncounter() == 5 && !p.advance(), "final victory records completion");
                int[] replay = {-1}; WorldMapPanel map = new WorldMapPanel((progress, stage) -> replay[0] = stage, q -> {}, () -> {});
                map.open(p); key(map,"map-enter"); key(map,"map-right"); key(map,"map-enter");
                check(replay[0] == 4 && p.getEncounter() == 5, "final stage remains replayable after completion");
                Field timer = BattlePanel.class.getDeclaredField("victoryTimer"); timer.setAccessible(true); ((javax.swing.Timer) timer.get(panel)).stop();
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        Path dir = Files.createTempDirectory("final-stage-");
        try {
            SaveStore store = new SaveStore(dir);
            GameProgress capped = new GameProgress(List.of(0, 1, 2));
            capped.claimStageRewardItems(); capped.advance();
            capped.getParty().get(0).gainExperience(2900);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, p); GameProgress saved = store.load(1, format);
                check(saved.getEncounter() == 5 && saved.getParty().get(0).getExperience() == p.getParty().get(0).getExperience()
                        && saved.getParty().get(0).getLevel() == p.getParty().get(0).getLevel(), "late-game save " + format);
                store.save(2, format, capped); Student restored = store.load(2, format).getParty().get(0);
                check(restored.getLevel() == 30 && restored.getExperience() == 2900
                        && restored.getMaxHp() == capped.getParty().get(0).getMaxHp()
                        && restored.getSp() == capped.getParty().get(0).getSp()
                        && restored.getSpeed() == capped.getParty().get(0).getSpeed(), "level 30 save " + format);
            }
        } finally { try (var paths = Files.walk(dir)) { for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); } }
        System.out.println("Final stages and level-up checks passed");
    }
}
