import game.io.SaveStore;
import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Checks Petot rewards, purchases, route access, and both save formats. */
public final class CarbonMarketTest {
    private static void check(boolean ok, String reason) { if (!ok) { throw new AssertionError(reason); } }
    private static void key(JComponent c, String name) { c.getActionMap().get(name).actionPerformed(new ActionEvent(c, 0, name)); }
    public static void main(String[] args) throws Exception {
        check(new Enemy("Wisp").getPetotDrop() == 12 && new Enemy("Sentinel", true).getPetotDrop() == 35, "monster currency fields");
        GameProgress p = new GameProgress(List.of(0, 1, 2)); p.advance(); p.advance();
        check(CarbonMarket.buy(p, 0).contains("Not enough") && p.getKits() == 3, "unaffordable purchase is rejected");
        p.addPetot(100);
        check(CarbonMarket.buy(p, 0).contains("Purchased") && p.getPetot() == 80 && p.getKits() == 4, "buy kit");
        check(CarbonMarket.buy(p, 1).contains("Purchased") && p.getPetot() == 20 && p.getInventory().getEquipmentIds().contains("market_charm"), "buy charm");
        p.addPetot(100);
        check(CarbonMarket.buy(p, 1).contains("already") && p.getPetot() == 120, "unique item purchase cannot duplicate or spend");
        while (p.getInventory().addKit()) { }
        check(CarbonMarket.buy(p, 0).contains("full") && p.getPetot() == 120, "full inventory does not charge");
        Path dir = Files.createTempDirectory("capstone-petot-");
        try {
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, p);
                GameProgress loaded = store.load(1, format);
                check(loaded.getPetot() == 120 && loaded.getInventory().getEquipmentIds().contains("market_charm"), "save " + format);
            }
            Path text = dir.resolve("slot-1.txt");
            Files.writeString(text, Files.readString(text).replace("version=5", "version=3").replaceAll("(?m)^inventory\\.petot=.*\\R", ""));
            check(store.load(1, SaveStore.Format.TEXT).getPetot() == 0, "old saves start with zero Petot");
        } finally { try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(path); } } }
        SwingUtilities.invokeAndWait(() -> {
            int[] shops = {0};
            WorldMapPanel map = new WorldMapPanel((progress, stage) -> {}, progress -> {}, () -> {});
            map.setShopAction(progress -> shops[0]++);
            GameProgress locked = new GameProgress(List.of(0, 1, 2)); map.open(locked);
            key(map, "map-up"); key(map, "map-enter"); check(shops[0] == 0, "shop locked before stage two");
            map.open(p); key(map, "map-up"); key(map, "map-enter"); check(shops[0] == 0, "shop remains locked after stage two");
            check(p.advance(), "stage three completion unlocks the shop");
            map.open(p); key(map, "map-left"); key(map, "map-up"); key(map, "map-enter");
            check(shops[0] == 1, "shop becomes accessible only after stage three");
            try {
                BattlePanel panel = new BattlePanel(() -> {}); GameProgress reward = new GameProgress(List.of(0, 1, 2));
                panel.start(reward, false);
                Field f = BattlePanel.class.getDeclaredField("battle"); f.setAccessible(true);
                Battle b = (Battle) f.get(panel); int expected = b.getEnemies().stream().mapToInt(Enemy::getPetotDrop).sum();
                for (Enemy enemy : b.getEnemies()) { enemy.takeDamage(999); }
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true);
                refresh.invoke(panel); refresh.invoke(panel);
                check(reward.getPetot() == expected, "victory pays defeated enemies exactly once");
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        System.out.println("Carbon Market and Petot checks passed");
        System.exit(0);
    }
}
