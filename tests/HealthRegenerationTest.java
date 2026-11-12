import game.io.SaveStore;
import game.model.*;
import game.view.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Covers HP regeneration chances, amounts and battle/camp timing. */
public final class HealthRegenerationTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Random fixed(double roll) { return new Random() {
        @Override public int nextInt(int bound) { return 0; }
        @Override public double nextDouble() { return roll; }
    }; }
    private static void equip(GameProgress p, int id, String armor) {
        Student s = p.getParty().stream().filter(x -> x.getId() == id).findFirst().orElseThrow();
        p.getInventory().addEquipment(armor); check(p.equip(s, Equipment.Slot.ARMOR, armor), "equip armor");
    }
    public static void main(String[] args) throws Exception {
        int[] base = {3, 12, 5, 6};
        for (int id = 0; id < 4; id++) {
            Student s = new Student(id); check(HealthRegeneration.amount(s) == base[id], "base amount for " + s.getName());
            s.takeDamage(30); int before = s.getHp();
            check(HealthRegeneration.attempt(s, fixed(0.25)) == 0 && s.getHp() == before, "25 percent boundary fails");
            check(HealthRegeneration.attempt(s, fixed(0.249999)) == base[id], "roll below 25 percent succeeds");
            s.takeDamage(9999); check(HealthRegeneration.attempt(s, fixed(0)) == 0 && !s.isAlive(), "regen cannot revive");
        }
        GameProgress z = new GameProgress(List.of(0)); equip(z, 0, "ethereal_robe");
        check(HealthRegeneration.amount(z.getParty().get(0)) == 24, "Zack armor bonus uses max HP basis");
        GameProgress b = new GameProgress(List.of(1)); equip(b, 1, "blade_mail");
        check(HealthRegeneration.amount(b.getParty().get(0)) == 261, "blade bonus adds percentage points using max HP");
        GameProgress c = new GameProgress(List.of(2)); equip(c, 2, "ultra_instinct_shirt");
        check(HealthRegeneration.amount(c.getParty().get(0)) == 32, "shirt bonus");
        GameProgress a = new GameProgress(List.of(3)); equip(a, 3, "designer_leather_vest");
        check(HealthRegeneration.amount(a.getParty().get(0)) == 26, "vest bonus includes equipment HP");
        GameProgress campParty = new GameProgress(List.of(0)); Student camper = campParty.getParty().get(0); camper.takeDamage(50);
        SwingUtilities.invokeAndWait(() -> {
            CampPanel camp = new CampPanel(p -> {}, () -> {}, p -> {}, p -> {}, fixed(0));
            camp.open(campParty, false); check(camper.getHp() == 25, "five camp attempts");
            camp.resume(campParty, false); check(camper.getHp() == 25, "return from inventory does not reroll");
            camp.open(campParty, false); check(camper.getHp() == 40, "new visit can heal again");
            try {
                GameProgress p = new GameProgress(List.of(0)); Student s = p.getParty().get(0); s.takeDamage(40);
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed(0)); panel.start(p, false, 0);
                check(s.getHp() == 20, "initial full AP does not grant regeneration");
                for (String action : List.of("battle-confirm", "battle-confirm")) panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel, 0, action));
                Field enemies = BattlePanel.class.getDeclaredField("enemyPoints"); enemies.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Enemy, Double> enemyPoints = (Map<Enemy, Double>) enemies.get(panel);
                for (Enemy enemy : enemyPoints.keySet()) enemyPoints.put(enemy, -1000.0);
                Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                advance.invoke(panel, 3.4); check(s.getHp() == 20, "no regeneration before AP fills");
                advance.invoke(panel, 0.1); check(s.getHp() == 23, "one attempt when AP fills");
                advance.invoke(panel, 1.0); check(s.getHp() == 23, "no repeat while choosing action");
                Field bf = BattlePanel.class.getDeclaredField("battle"); bf.setAccessible(true);
                Battle fight = (Battle) bf.get(panel);
                check(fight.getEffects().stream().anyMatch(e -> e.getStudent() == s && e.getAmount() == 3), "battle healing indicator");
                GameProgress ringParty = new GameProgress(List.of(0)); Student ringMage = ringParty.getParty().get(0); ringMage.takeDamage(40);
                ringParty.getInventory().addEquipment("ring_of_power");
                check(ringParty.equip(ringMage, Equipment.Slot.ACCESSORY, "ring_of_power"), "equip ring");
                BattlePanel ringPanel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed(0)); ringPanel.start(ringParty, true, 0);
                advance.invoke(ringPanel, 1.0); Battle ringBattle = (Battle) bf.get(ringPanel);
                check(ringMage.getHp() == 13 && ringBattle.getCurrent() == ringMage, "ring refill causes one regeneration attempt");
                check(ringBattle.getEffects().stream().anyMatch(e -> e.getStudent() == ringMage && e.getAmount() == 3), "ring refill shows regeneration");
                for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer")) {
                    Field timer = BattlePanel.class.getDeclaredField(name); timer.setAccessible(true); ((javax.swing.Timer) timer.get(ringPanel)).stop();
                }
                for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer")) {
                    Field timer = BattlePanel.class.getDeclaredField(name); timer.setAccessible(true); ((javax.swing.Timer) timer.get(panel)).stop();
                }
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        Path dir = Files.createTempDirectory("regen-save-");
        try { SaveStore store = new SaveStore(dir); b.advance(); Student student = b.getParty().get(0); student.takeDamage(100);
            HealthRegeneration.attempt(student, fixed(0));
            for (SaveStore.Format format : SaveStore.Format.values()) { store.save(1, format, b); Student restored = store.load(1, format).getParty().get(0);
                check(restored.getHp() == student.getHp() && restored.getMaxHp() == student.getMaxHp(), "regenerated HP persists " + format); }
        } finally { try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
        System.out.println("Health regeneration checks passed");
    }
}
