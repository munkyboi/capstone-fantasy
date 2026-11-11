import game.model.*;
import game.view.WorldMapPanel;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Covers replay confirmation, unique rewards, loot limits and stage selection. */
public final class ReplayLootTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    private static void key(JComponent panel, String name) { panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name)); }
    private static final class Always extends Random {
        @Override public double nextDouble() { return 0.0; }
    }
    public static void main(String[] args) throws Exception {
        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        check(progress.claimStageRewardItems().size() == 4, "first clear awards equipment");
        progress.advance(); progress.claimStageRewardItems(); progress.advance();
        check(progress.claimStageRewardItems(0).isEmpty() && progress.claimStageRewardItems(1).isEmpty(), "replays do not repeat stage rewards");
        Battle first = new Battle(progress, new Random(1), false, 0);
        Battle second = new Battle(progress, new Random(1), false, 1);
        check(first.getEnemies().stream().noneMatch(Enemy::isSentinel), "stage one replay uses stage one enemies");
        check(second.getEnemies().stream().anyMatch(Enemy::isSentinel), "stage two replay includes sentinel");
        check(progress.getEncounter() == 2, "replaying does not rewind progress");
        Enemy wisp = new Enemy("Library Wisp A"), sentinel = new Enemy("Stone Sentinel", true);
        wisp.takeDamage(999); sentinel.takeDamage(999);
        List<String> drops = MonsterLoot.award(List.of(wisp, sentinel), progress, new Always());
        check(drops.equals(List.of("first_aid_kit", "sentinel_band")) && progress.getKits() == 4, "loot is credited to the bag");
        check(MonsterLoot.award(List.of(sentinel), progress, new Always()).isEmpty(), "unique band is not duplicated");
        for (int i = 0; i < 10; i++) { progress.getInventory().addKit(); }
        check(progress.getKits() == Inventory.MAX_KITS && MonsterLoot.award(List.of(wisp), progress, new Always()).isEmpty(), "full kit bag reports no drop");
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("capstone-replay-save-");
        try {
            game.io.SaveStore store = new game.io.SaveStore(dir);
            for (game.io.SaveStore.Format format : game.io.SaveStore.Format.values()) {
                store.save(1, format, progress);
                GameProgress loaded = store.load(1, format);
                check(loaded.getEncounter() == 2 && loaded.getKits() == Inventory.MAX_KITS
                        && loaded.getInventory().getEquipmentIds().contains("sentinel_band"), "loot and progress survive " + format);
            }
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(dir)) {
                for (java.nio.file.Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    java.nio.file.Files.deleteIfExists(file);
                }
            }
        }
        GameProgress reserves = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        reserves.getInventory().addEquipment("sentinel_band");
        Student reserve = reserves.getParty().get(0);
        check(reserves.equip(reserve, Equipment.Slot.ACCESSORY, "sentinel_band"), "equip unique band");
        reserves.setActiveParty(reserves.getParty().stream().filter(s -> s != reserve).map(Student::getId).toList());
        check(MonsterLoot.award(List.of(sentinel), reserves, new Always()).isEmpty(), "unique band equipped by reserve is not duplicated");
        SwingUtilities.invokeAndWait(() -> {
            int[] started = {-1};
            WorldMapPanel map = new WorldMapPanel((p, stage) -> started[0] = stage, p -> {}, () -> {});
            map.open(progress); key(map, "map-left"); key(map, "map-enter");
            check(started[0] == -1, "replay asks before starting");
            key(map, "map-enter"); check(started[0] == -1, "default No cancels replay");
            key(map, "map-enter"); key(map, "map-right"); key(map, "map-enter");
            check(started[0] == 1 && progress.getEncounter() == 2, "confirm starts selected cleared stage");
        });
        System.out.println("Replay and monster loot checks passed");
    }
}
