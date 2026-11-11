import game.io.SaveStore;
import game.model.Equipment;
import game.model.GameProgress;
import game.model.Student;
import game.view.InventoryPanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;

/** Exercises equipment ownership, rewards, UI and save round trips. */
public final class EquipmentSystemTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(InventoryPanel panel, String name) {
        panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel, 0, name));
    }
    private static Student find(GameProgress progress, int id) {
        return progress.getParty().stream().filter(student -> student.getId() == id).findFirst().orElseThrow();
    }
    /** Runs equipment and reward checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        GameProgress unboosted = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        GameProgress boosted = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        GameProgress rewardItems = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        java.util.List<String> firstRewards = rewardItems.claimStageRewardItems();
        check(firstRewards.size() == 4 && firstRewards.contains("focus_charm")
                && firstRewards.contains("scholar_staff") && firstRewards.contains("cadet_blade")
                && firstRewards.contains("champion_wraps"), "reward IDs match selected party");
        check(rewardItems.claimStageRewardItems().isEmpty(), "reward IDs are not granted twice");
        rewardItems.advance();
        check(rewardItems.claimStageRewardItems().equals(java.util.List.of("training_uniform")),
                "second-stage popup receives the actual uniform reward");
        boosted.claimStageReward();
        check(boosted.equip(find(boosted, Student.ZACK), Equipment.Slot.WEAPON, "scholar_staff"), "equip upgraded staff");
        java.util.Random fixed = new java.util.Random() { @Override public int nextInt(int bound) { return 0; } };
        game.model.Battle baseBattle = new game.model.Battle(unboosted, fixed);
        game.model.Battle boostedBattle = new game.model.Battle(boosted, fixed);
        var baseTarget = baseBattle.getEnemies().get(0); var boostedTarget = boostedBattle.getEnemies().get(0);
        check(baseBattle.selectStudent(find(unboosted, Student.ZACK)) && boostedBattle.selectStudent(find(boosted, Student.ZACK)),
                "select attacker");
        int baseHp = baseTarget.getHp(), boostedHp = boostedTarget.getHp();
        check(baseBattle.attack(baseTarget) && boostedBattle.attack(boostedTarget), "perform comparable attacks");
        check((boostedHp - boostedTarget.getHp()) - (baseHp - baseTarget.getHp()) == 3,
                "weapon bonus affects battle damage");
        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.CHARICE, Student.ANGEL));
        Student zack = find(progress, Student.ZACK), charice = find(progress, Student.CHARICE);
        check("student_staff".equals(zack.getEquipmentId(Equipment.Slot.WEAPON))
                && "hand_crossbows".equals(charice.getEquipmentId(Equipment.Slot.WEAPON)), "matching starter weapons");
        check(progress.equip(zack, Equipment.Slot.WEAPON, null), "unequip starter weapon");
        check(!progress.equip(charice, Equipment.Slot.WEAPON, "student_staff")
                && progress.getInventory().getEquipmentIds().contains("student_staff"), "incompatible weapon stays in bag");
        check(progress.equip(zack, Equipment.Slot.WEAPON, "student_staff"), "reequip matching weapon");
        check(progress.equip(zack, Equipment.Slot.ARMOR, "campus_vest") && zack.getDefense() == zack.getBaseDefense() + 4,
                "armor changes defense");
        check(!progress.equip(charice, Equipment.Slot.ARMOR, "campus_vest"), "one item cannot be equipped twice");
        check("Focus Charm and party weapon upgrades".equals(progress.claimStageReward()) && progress.claimStageReward() == null,
                "stage reward is granted once");
        check(progress.getInventory().getEquipmentIds().contains(Equipment.upgradedWeapon(Student.CHARICE))
                && !progress.getInventory().getEquipmentIds().contains(Equipment.upgradedWeapon(Student.BOGART)),
                "first stage grants upgrades for the selected party");
        int baseAttack = charice.getAttack();
        int[] returned = {0};
        SwingUtilities.invokeAndWait(() -> {
            try {
                InventoryPanel panel = new InventoryPanel(() -> returned[0]++); panel.open(progress);
                key(panel, "inventory-right"); key(panel, "inventory-right");
                key(panel, "inventory-confirm"); key(panel, "inventory-down");
                Field details = InventoryPanel.class.getDeclaredField("details"); details.setAccessible(true);
                String preview = ((javax.swing.JTextArea) details.get(panel)).getText();
                check(preview.contains("Focus Charm") && preview.contains("ATK +2"), "item and stat preview");
                key(panel, "inventory-confirm");
                check(charice.getAttack() == baseAttack + 2 && "focus_charm".equals(charice.getEquipmentId(Equipment.Slot.ACCESSORY)),
                        "equip accessory with keyboard");
                key(panel, "inventory-back"); check(returned[0] == 1, "return to camp");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
        progress.advance();
        check("Training Uniform".equals(progress.claimStageReward()), "second stage reward");
        progress.advance();
        Path directory = Files.createTempDirectory("capstone-equipment-");
        try {
            SaveStore store = new SaveStore(directory);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, progress);
                GameProgress loaded = store.load(1, format);
                Student loadedCharice = find(loaded, Student.CHARICE), loadedZack = find(loaded, Student.ZACK);
                check("focus_charm".equals(loadedCharice.getEquipmentId(Equipment.Slot.ACCESSORY))
                        && loadedCharice.getAttack() == baseAttack + 2 && loadedZack.hasVest(), "equipped items survive " + format);
                check(loaded.getInventory().getEquipmentIds().contains("training_uniform")
                        && loaded.getInventory().isRewardClaimed(0) && loaded.getInventory().isRewardClaimed(1), "bag and reward flags survive " + format);
                check(loaded.equip(loadedZack, Equipment.Slot.WEAPON, "scholar_staff")
                        && loadedZack.getAttack() == loadedZack.getBaseAttack() + 3
                        && loaded.getInventory().getEquipmentIds().contains("student_staff"), "weapon upgrade changes attack");
                check(loaded.equip(loadedZack, Equipment.Slot.ARMOR, "training_uniform")
                        && loadedZack.getDefense() == loadedZack.getBaseDefense() + 6
                        && loaded.getInventory().getEquipmentIds().contains("campus_vest"), "swapping returns prior armor");
            }
            System.out.println("Equipment and stage reward checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                });
            }
        }
    }
}
