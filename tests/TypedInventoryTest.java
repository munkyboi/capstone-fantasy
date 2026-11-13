import game.io.SaveStore;
import game.model.*;
import java.nio.file.*;
import java.util.*;

/** Typed inventory behavior, error handling, and save round trips. */
public final class TypedInventoryTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        ItemBag<Equipment> bag = new ItemBag<>();
        Equipment charm = Equipment.fromId("focus_charm"); bag.add(charm); bag.add(Equipment.fromId("campus_vest"));
        check(bag.items().size() == 2 && bag.removeFirst(item -> item.getId().equals("focus_charm")), "generic bag stores and removes typed equipment");
        try { bag.items().clear(); throw new AssertionError("snapshot must be immutable"); } catch (UnsupportedOperationException expected) { }
        try { Equipment.fromId("missing_item"); throw new AssertionError("unknown item must fail"); }
        catch (ItemNotFoundException expected) { check(expected.getMessage().contains("missing_item"), "error identifies item"); }
        GameProgress progress = new GameProgress(List.of(Student.CHARICE)); Inventory inventory = progress.getInventory();
        check(inventory.getItems().stream().anyMatch(KeyItem.class::isInstance), "key item lives in bag");
        check(inventory.getItems().stream().filter(FirstAidKit.class::isInstance).count() == progress.getKits(), "kits are carried objects");
        check(inventory.getItems().stream().filter(WhiteFlower.class::isInstance).count() == 1, "white flower is a carried object");
        inventory.addEquipment("hunter_crossbows"); Student hero = progress.getParty().get(0);
        check(progress.equip(hero, Equipment.Slot.WEAPON, "hunter_crossbows"), "typed equipment can be equipped");
        check(inventory.getItems().stream().filter(Equipment.class::isInstance).map(Equipment.class::cast).anyMatch(e -> e.getId().equals("hand_crossbows")), "old weapon returns as an object");
        progress.advance();
        Path dir = Files.createTempDirectory("typed-inventory-");
        try {
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, progress); Inventory loaded = store.load(1, format).getInventory();
                check(loaded.getEquipmentIds().equals(inventory.getEquipmentIds()) && loaded.getKits() == inventory.getKits(), "equipment and kits survive " + format);
                check(loaded.getItems().stream().anyMatch(KeyItem.class::isInstance) && loaded.getItems().stream().anyMatch(WhiteFlower.class::isInstance), "typed items restored from " + format);
            }
        } finally { try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
        System.out.println("Typed inventory checks passed");
    }
}
