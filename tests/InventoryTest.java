import game.io.SaveStore;
import game.model.Equipment;
import game.model.GameCharacter;
import game.model.GameProgress;
import game.model.Hero;
import game.model.Student;
import game.view.CampPanel;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.Action;
import javax.swing.SwingUtilities;

/** Checks equipment, inventory and both save formats. */
public final class InventoryTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(CampPanel panel, String name) {
        Action action = panel.getActionMap().get(name);
        if (action == null) { throw new AssertionError("Missing action " + name); }
        action.actionPerformed(new ActionEvent(panel, 0, name));
    }
    /** Runs shared inventory checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        check(progress.getParty().stream().allMatch(member -> member instanceof Hero && member instanceof GameCharacter),
                "party uses hero hierarchy");
        check(progress.getInventory().getItems().stream().map(item -> item.getCategory()).distinct().count() == 3,
                "consumable, equipment and key item are modeled");
        progress.advance();
        Student selected = progress.getParty().get(0);
        int baseDefense = selected.getDefense();
        SwingUtilities.invokeAndWait(() -> {
            CampPanel camp = new CampPanel(p -> { }, () -> { });
            camp.open(progress, false);
            key(camp, "camp-equip");
            check(selected.hasVest() && selected.getDefense() == baseDefense + Equipment.DEFENSE_BONUS,
                    "equip vest with keyboard");
        });
        selected.takeDamage(15);
        check(progress.useKit(selected) && progress.getKits() == 2, "kit uses shared inventory");
        Path directory = Files.createTempDirectory("capstone-inventory-");
        try {
            SaveStore store = new SaveStore(directory);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, progress);
                GameProgress loaded = store.load(1, format);
                Student wearer = loaded.getParty().stream().filter(s -> s.getId() == selected.getId()).findFirst().orElseThrow();
                check(loaded.getKits() == 2 && loaded.getInventory().getVestOwner() == selected.getId()
                        && wearer.hasVest() && wearer.getDefense() == baseDefense + Equipment.DEFENSE_BONUS,
                        "inventory and equipment survive " + format + " save");
                check(loaded.getInventory().getItems().stream().anyMatch(item -> item.getName().equals("Campus Pass")),
                        "key item persists");
                check(loaded.toggleVest(wearer) && !wearer.hasVest() && wearer.getDefense() == baseDefense,
                        "unequipping removes bonus");
            }
            System.out.println("Inventory and equipment checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                });
            }
        }
    }
}
