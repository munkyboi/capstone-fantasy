import game.io.SaveStore;
import game.model.*;
import game.view.BattlePanel;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Checks armor combat, health changes, equipment and saved data. */
public final class ArmorEquipmentTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Random fixed(double value) { return new Random() {
        @Override public int nextInt(int bound) { return 0; }
        @Override public double nextDouble() { return value; }
    }; }
    private static Student student(GameProgress p, int id) { return p.getParty().stream().filter(s -> s.getId() == id).findFirst().orElseThrow(); }
    private static void equip(GameProgress p, int id, String armor) {
        p.getInventory().addEquipment(armor); check(p.equip(student(p, id), Equipment.Slot.ARMOR, armor), "equip " + armor);
    }
    private static boolean status(Battle b, Student target, String text) {
        return b.getEffects().stream().anyMatch(e -> e.getStudent() == target && text.equals(e.getLabel()));
    }
    public static void main(String[] args) throws Exception {
        GameProgress market = new GameProgress(List.of(0, 1, 2)); market.addPetot(10000);
        for (int i = 7; i < 11; i++) check(CarbonMarket.buy(market, i).startsWith("Purchased"), "buy armor " + i);
        check(market.getPetot() == 0, "armor costs 2500 each");
        GameProgress blade = new GameProgress(List.of(0)); Student zack = student(blade, 0); zack.takeDamage(20);
        equip(blade, 0, "blade_mail");
        check(zack.getMaxHp() == 180 && zack.getHp() == 120 && zack.getDefense() == 2 && zack.getSpeed() == 14,
                "blade stats and preserved health fraction");
        Battle reflect = new Battle(blade, fixed(0)); Enemy wisp = reflect.getEnemies().get(0);
        int heroBefore = zack.getHp(), enemyBefore = wisp.getHp(); reflect.enemyAttack(wisp);
        int damage = heroBefore - zack.getHp();
        check(damage > 0 && enemyBefore - wisp.getHp() == Math.round(damage * 0.5) && status(reflect, zack, "Reflect"), "blade reflects actual damage and shows label");
        wisp.takeDamage(wisp.getHp() - 1); reflect.enemyAttack(wisp);
        check(reflect.isVictory() && status(reflect, zack, "Reflect"), "reflection can finish the last enemy");
        check(blade.equip(zack, Equipment.Slot.ARMOR, null) && zack.getMaxHp() == 60 && zack.getHp() <= 60, "unequipping safely scales HP");
        GameProgress dodgeParty = new GameProgress(List.of(0)); Student dodgeHero = student(dodgeParty, 0); equip(dodgeParty, 0, "ultra_instinct_shirt");
        check(dodgeHero.getDefense() == 3 && dodgeHero.getSpeed() == 20, "shirt stats");
        Battle evade = new Battle(dodgeParty, fixed(0.499999)); int dodgeBefore = dodgeHero.getHp(); evade.enemyAttack(evade.getEnemies().get(0));
        check(dodgeHero.getHp() == dodgeBefore && status(evade, dodgeHero, "Dodge"), "roll below 50 percent dodges");
        Battle hit = new Battle(dodgeParty, fixed(0.5)); hit.enemyAttack(hit.getEnemies().get(0));
        check(dodgeHero.getHp() < dodgeBefore && !status(hit, dodgeHero, "Dodge"), "roll at 50 percent is a hit");
        GameProgress robeParty = new GameProgress(List.of(0)); Student mage = student(robeParty, 0); equip(robeParty, 0, "ethereal_robe");
        check(mage.getMaxHp() == 160 && mage.getDefense() == 5, "robe HP and defense");
        Battle robe = new Battle(robeParty, fixed(0)); Enemy foe = robe.getEnemies().get(0); robe.enemyAttack(foe);
        check(mage.hasEtherealCharge(), "damage charges robe"); int beforeHp = foe.getHp();
        check(robe.selectStudent(mage) && robe.attack(foe), "empowered basic attack");
        check(beforeHp - foe.getHp() == Math.min(beforeHp, mage.getAttack() * 4 - foe.getDefense()) && !mage.hasEtherealCharge(), "next basic attack uses 4x ATK once");
        GameProgress restore = new GameProgress(List.of(0, 1, 2)); restore.advance();
        equip(restore, 0, "blade_mail"); equip(restore, 1, "designer_leather_vest"); equip(restore, 2, "ethereal_robe");
        student(restore, 0).takeDamage(40); student(restore, 1).takeDamage(20); student(restore, 2).takeDamage(10);
        Path dir = Files.createTempDirectory("armor-save-");
        try {
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, restore); GameProgress saved = store.load(1, format);
                for (int id = 0; id < 3; id++) check(student(saved, id).getHp() == student(restore, id).getHp()
                        && student(saved, id).getMaxHp() == student(restore, id).getMaxHp(), "HP armor save " + format + " " + id);
            }
        } finally { try (var files = Files.walk(dir)) { for (Path f : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(f); } }
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress xpParty = new GameProgress(List.of(0, 1)); equip(xpParty, 0, "designer_leather_vest");
                check(student(xpParty, 0).getMaxHp() == 110 && student(xpParty, 0).getSpeed() == 20 && student(xpParty, 0).getDefense() == 4, "designer vest stats");
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed(0.9)); panel.start(xpParty, false, 0);
                Field bf = BattlePanel.class.getDeclaredField("battle"); bf.setAccessible(true); Battle battle = (Battle) bf.get(panel);
                int base = battle.getEnemies().stream().mapToInt(Enemy::getExperienceDrop).sum() * 2;
                battle.getEnemies().forEach(enemy -> enemy.takeDamage(9999));
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(panel);
                check(student(xpParty, 0).getExperience() == base * 2 && student(xpParty, 1).getExperience() == base, "only wearer receives double XP");
                Field bonus = BattlePanel.class.getDeclaredField("bonusXpStudents"); bonus.setAccessible(true);
                check(((List<?>) bonus.get(panel)).size() == 1, "victory popup reports bonus XP");
                for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer")) { Field f = BattlePanel.class.getDeclaredField(name); f.setAccessible(true); ((javax.swing.Timer) f.get(panel)).stop(); }
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        System.out.println("Armor equipment checks passed");
    }
}
