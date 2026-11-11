import game.io.SaveStore;
import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Exercises stage three, its reward, replay, shop unlock and save/load. */
public final class StageThreeTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    private static void key(JComponent panel, String action) { panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action)); }
    public static void main(String[] args) throws Exception {
        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.CHARICE));
        progress.claimStageRewardItems(); progress.advance(); progress.claimStageRewardItems(); progress.advance();
        for (int offset = 0; offset < 3; offset++) {
            final int roll = offset;
            Random random = new Random() { @Override public int nextInt(int bound) { return Math.min(roll, bound - 1); } };
            Battle battle = new Battle(progress, random, false, 2);
            check(battle.getEnemies().size() == 3 + offset, "stage three spawn range");
            check(battle.getEnemies().stream().filter(Enemy::isSentinel).count() == 1, "stage three guardian composition");
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                BattlePanel panel = new BattlePanel(() -> {}); panel.start(progress, false, 2);
                Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true);
                Battle battle = (Battle) field.get(panel);
                Field sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                Object scene = sceneField.get(panel);
                Field imagesField = scene.getClass().getDeclaredField("stageBackgrounds"); imagesField.setAccessible(true);
                java.awt.image.BufferedImage[] backgrounds = (java.awt.image.BufferedImage[]) imagesField.get(scene);
                check(backgrounds.length == 5 && java.util.Arrays.stream(backgrounds).allMatch(java.util.Objects::nonNull),
                        "all five stage backgrounds are packaged");
                check(panel.getClass().getResource("/assets/backgrounds/stage-5-crystal-chamber-v3.png") != null,
                        "indoor Crystal Chamber background is packaged");
                for (Enemy enemy : battle.getEnemies()) { enemy.takeDamage(999); }
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                check(progress.getInventory().getEquipmentIds().contains("guardian_band"), "third stage reward granted");
                check(progress.advance() && progress.getEncounter() == 3, "third stage unlocks next marker");
                int size = progress.getInventory().getEquipmentIds().size();
                check(progress.claimStageRewardItems(2).isEmpty() && progress.getInventory().getEquipmentIds().size() == size,
                        "third stage reward is not duplicated on replay");
                int[] visits = {0};
                WorldMapPanel map = new WorldMapPanel((p, stage) -> {}, p -> {}, () -> {});
                map.setShopAction(p -> visits[0]++); map.open(progress); key(map, "map-left"); key(map, "map-up"); key(map, "map-enter");
                check(visits[0] == 1, "market accessible after stage three victory");
                Field timer = BattlePanel.class.getDeclaredField("victoryTimer"); timer.setAccessible(true); ((javax.swing.Timer) timer.get(panel)).stop();
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        Path directory = Files.createTempDirectory("stage-three-");
        try {
            SaveStore store = new SaveStore(directory);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, progress); GameProgress loaded = store.load(1, format);
                check(loaded.getEncounter() == 3 && loaded.getInventory().getEquipmentIds().contains("guardian_band"), "post-stage-three save " + format);
            }
        } finally {
            try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
        System.out.println("Stage three and market unlock checks passed");
    }
}
