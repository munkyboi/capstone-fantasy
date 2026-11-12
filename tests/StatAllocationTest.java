import game.io.SaveStore;
import game.model.*;
import game.view.CampPanel;
import java.awt.event.ActionEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Exercises point spending, party selection, and persistence in both save formats. */
public final class StatAllocationTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void key(CampPanel camp, String name) { camp.getActionMap().get(name).actionPerformed(new ActionEvent(camp, 0, "")); }
    public static void main(String[] args) throws Exception {
        GameProgress progress = new GameProgress(List.of(Student.ZACK, Student.BOGART, Student.ANGEL));
        progress.advance();
        Student hero = progress.getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
        check(!hero.allocateStatPoint(Student.Stat.ATK), "no points at level one");
        hero.gainExperience(700);
        check(hero.getUnspentStatPoints() == 7, "one point per level");
        int hp = hero.getMaxHp(), sp = hero.getMaxSp(), attack = hero.getAttack(), defense = hero.getDefense(), speed = hero.getSpeed();
        int agility = hero.getAgility(), accuracy = hero.getAccuracy();
        SwingUtilities.invokeAndWait(() -> {
            CampPanel camp = new CampPanel(p -> {}, () -> {}); camp.open(progress, false);
            key(camp, "camp-allocate");
            for (int i = 0; i < 7; i++) { key(camp, "camp-use"); if (i < 6) key(camp, "camp-right"); }
            key(camp, "camp-use");
            key(camp, "camp-menu");
        });
        check(hero.getUnspentStatPoints() == 0 && hero.getMaxHp() == hp + 5 && hero.getMaxSp() == sp + 3
                && hero.getAttack() == attack + 1 && hero.getDefense() == defense + 1 && hero.getSpeed() == speed + 1
                && hero.getAgility() == agility + 1 && hero.getAccuracy() == accuracy + 1, "each stat spent once");
        try { hero.restoreStatAllocations(20, 0, 0, 0, 0); throw new AssertionError("invalid points accepted"); }
        catch (IllegalArgumentException expected) { }
        Path directory = Files.createTempDirectory("allocation-save-");
        try {
            SaveStore store = new SaveStore(directory);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, progress);
                Student loaded = store.load(1, format).getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
                check(loaded.getUnspentStatPoints() == 0 && loaded.getHp() == hero.getHp() && loaded.getSp() == hero.getSp()
                        && loaded.getMaxHp() == hero.getMaxHp() && loaded.getMaxSp() == hero.getMaxSp()
                        && loaded.getAttack() == hero.getAttack() && loaded.getDefense() == hero.getDefense()
                        && loaded.getSpeed() == hero.getSpeed() && loaded.getAgility() == hero.getAgility() && loaded.getAccuracy() == hero.getAccuracy(), "allocation roundtrip " + format);
                for (Student.Stat stat : Student.Stat.values()) check(loaded.getAllocatedStatPoints(stat) == 1, "saved " + stat);
            }
            // Legacy v5 saves had no allocation fields: their earned points remain unspent.
            GameProgress legacy = new GameProgress(List.of(0, 1, 2)); legacy.advance();
            legacy.getParty().get(0).gainExperience(300); store.save(2, SaveStore.Format.TEXT, legacy);
            Path path = directory.resolve("slot-2.txt");
            String text = Files.readString(path).replace("version=9", "version=5").replaceAll("(?m)^(student\\.[123]\\.sp)=.*$", "$1=0"); Files.writeString(path, text);
            check(store.load(2, SaveStore.Format.TEXT).getParty().get(0).getUnspentStatPoints() == 3, "old saves receive earned points");
            GameProgress old = new GameProgress(List.of(0)); old.advance(); old.getParty().get(0).gainExperience(200);
            old.getParty().get(0).allocateStatPoint(Student.Stat.ATK); store.save(3, SaveStore.Format.TEXT, old);
            Path oldPath = directory.resolve("slot-3.txt");
            Files.writeString(oldPath, Files.readString(oldPath).replaceAll("(?m)^student\\.1\\.allocated\\.(AGI|ACC)=.*\\R", ""));
            Student restored = store.load(3, SaveStore.Format.TEXT).getParty().get(0);
            check(restored.getAllocatedStatPoints(Student.Stat.ATK)==1 && restored.getAllocatedStatPoints(Student.Stat.AGI)==0
                    && restored.getAllocatedStatPoints(Student.Stat.ACC)==0 && restored.getUnspentStatPoints()==1, "older version-nine saves retain points");
        } finally { try (var paths = Files.walk(directory)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
        System.out.println("Stat allocation and save checks passed");
    }
}
