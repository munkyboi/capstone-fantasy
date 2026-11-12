import game.io.SaveStore;
import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Verifies boss cadence, Sentinel support, legendary equipment and persistence. */
public final class SpecialCombatEquipmentTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Random fixed() { return new Random() {
        @Override public int nextInt(int bound) { return 0; }
        @Override public double nextDouble() { return 0; }
    }; }
    private static Student student(GameProgress p, int id) { return p.getParty().stream().filter(s -> s.getId() == id).findFirst().orElseThrow(); }
    private static void equip(GameProgress p, int id, String item) {
        if (!p.getInventory().getEquipmentIds().contains(item)) p.getInventory().addEquipment(item);
        check(p.equip(student(p, id), Equipment.fromId(item).getSlot(), item), "equip " + item);
    }
    private static GameProgress finalParty() {
        GameProgress p = new GameProgress(List.of(0, 1, 2, 3)); for (int i = 0; i < 4; i++) p.advance(); return p;
    }
    public static void main(String[] args) throws Exception {
        GameProgress p = finalParty(); Battle b = new Battle(p, fixed(), false, 4, GameSettings.Difficulty.HARD);
        Enemy boss = b.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
        check(boss.getBossCountdown() == 3, "initial boss countdown");
        check(b.enemyAttack(boss) && boss.getBossCountdown() == 3, "boss first action");
        check(b.enemyAttack(boss) && boss.getBossCountdown() == 2, "boss second action");
        check(b.enemyAttack(boss) && boss.getBossCountdown() == 1, "boss third action");
        int living = (int) b.getParty().stream().filter(Student::isAlive).count();
        check(b.enemyAttack(boss) && b.getMessage().contains("Crystal Pulse") && b.getEffects().size() == living
                && boss.getBossCountdown() == 3, "boss fourth action damages all and resets countdown");
        for (BattleEffect effect : b.getEffects()) {
            Student target = effect.getStudent();
            int expected = (int) Math.round(Math.max(1, boss.getAttack() - target.getDefense()) * 1.5) * 2;
            check(-effect.getAmount() == Math.min(expected, target.getHp() - effect.getAmount()), "boss 2x difficulty damage");
        }
        GameProgress support = new GameProgress(List.of(0, 1, 2)); support.advance();
        Battle sb = new Battle(support, fixed(), false, 1); Enemy sentinel = sb.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Enemy ally = sb.getEnemies().stream().filter(e -> !e.isSentinel()).findFirst().orElseThrow();
        ally.takeDamage(40); int before = ally.getHp();
        check(sb.enemyAttack(sentinel) && sb.getMessage().contains("heals") && ally.getHp() == before + 4 && sentinel.getSp() == 20,
                "Sentinel heals injured ally for 20 percent attack and spends SP");
        check(sb.getEffects().get(0).getAmount() == 4 && sb.getEffects().get(0).getEnemy() == ally, "healing indicator targets ally");
        check(sb.enemyAttack(sentinel) && sb.enemyAttack(sentinel) && sentinel.getSp() == 0, "Sentinel spends available SP");
        check(sb.enemyAttack(sentinel) && !sb.getMessage().contains("heals"), "Sentinel cannot heal without SP");
        Enemy hardSentinel = new Enemy("Stone Sentinel", true, false, GameSettings.Difficulty.HARD);
        check(hardSentinel.getMaxSp() == 45 && hardSentinel.getSp() == 45, "difficulty scales Sentinel SP");
        GameProgress market = new GameProgress(List.of(0, 1, 2, 3)); market.addPetot(13000);
        for (int i = 2; i < 7; i++) check(CarbonMarket.buy(market, i).startsWith("Purchased"), "buy market item " + i);
        check(market.getPetot() == 500 && market.getInventory().getEquipmentIds().contains("light_saber"), "purchase price and items");
        equip(market, 0, "great_sage_staff"); equip(market, 1, "light_saber"); equip(market, 2, "demon_hunter"); equip(market, 3, "giant_slayer_gloves"); equip(market, 2, "ring_of_power");
        Student hunter = student(market, 2); check(hunter.getAttack() == 24 && hunter.getSpeed() == 41 && hunter.getDefense() == 12, "percentage stats stack additively");
        check(student(market, 1).getAttack() == 115 && student(market, 1).getSpeed() == 14, "Light Saber stats");
        check(student(market, 3).getAttack() == 38 && student(market, 3).getSpeed() == 20, "glove stats");
        market.advance(); Path dir = Files.createTempDirectory("special-equipment-");
        try {
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, market); GameProgress loaded = store.load(1, format);
                check(student(loaded, 2).getSpeed() == 41 && student(loaded, 1).getAttack() == 115, "legendary equipment save " + format);
            }
        } finally { try (var files = Files.walk(dir)) { for (Path f : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(f); } }
        GameProgress fire = finalParty(); equip(fire, 1, "light_saber"); Battle burn = new Battle(fire, fixed(), false, 4, GameSettings.Difficulty.INSANE);
        Enemy big = burn.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow(); Student warrior = student(fire, 1);
        check(burn.selectStudent(warrior), "select warrior"); int initial = big.getHp(); burn.attack(big); int hit = initial - big.getHp();
        List<BattleEffect> hitEffects = burn.getEffects();
        check(!burn.advanceDamageOverTime(0.99) && burn.getEffects().equals(hitEffects), "direct hit indicator remains visible before first tick");
        check(burn.advanceDamageOverTime(0.01), "first tick at one second");
        List<BattleEffect> tickEffects = burn.getEffects();
        check(!burn.advanceDamageOverTime(0.05) && burn.getEffects().equals(tickEffects), "tick indicator remains visible between updates");
        burn.advanceDamageOverTime(0.95);
        for (int i = 2; i < 5; i++) burn.advanceDamageOverTime(1.0);
        check(initial - big.getHp() == hit * 2, "five ticks deal 100 percent of hit over five seconds");
        check(!burn.advanceDamageOverTime(1), "damage stops after five ticks");
        GameProgress mageParty = new GameProgress(List.of(0, 1, 2)); equip(mageParty, 0, "great_sage_staff"); mageParty.advance();
        Battle casts = new Battle(mageParty, fixed(), false, 1); Student mage = student(mageParty, 0); int sp = mage.getSp();
        check(casts.selectStudent(mage) && casts.ability(null, null) && casts.hasPendingSecondCast()
                && casts.completeSecondCast() && casts.getMessage().contains("again")
                && mage.getSp() == sp - mage.getAbilitySpCost(), "double cast spends SP once");
        GameProgress monkParty = finalParty(); equip(monkParty, 3, "giant_slayer_gloves"); Battle punches = new Battle(monkParty, fixed(), false, 4);
        check(punches.selectStudent(student(monkParty, 3)) && punches.attack(punches.getEnemies().get(0))
                && punches.getEffects().size() == 4, "gloves add one basic hit");
        GameProgress ringParty = new GameProgress(List.of(0, 1, 2)); Student first = ringParty.getParty().get(0); equip(ringParty, first.getId(), "ring_of_power");
        Battle ring = new Battle(ringParty, fixed()); check(ring.enemyAttack(ring.getEnemies().get(0))
                && ring.consumeApRefills().contains(first), "ring can refill AP when damaged");
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress speedParty = new GameProgress(List.of(0, 1, 2)); equip(speedParty, 2, "demon_hunter");
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed()); panel.start(speedParty, true, 0);
                Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true); advance.invoke(panel, 1.0);
                Field points = BattlePanel.class.getDeclaredField("actionPoints"); points.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Student, Double> values = (Map<Student, Double>) points.get(panel);
                check(Math.abs(values.get(student(speedParty, 2)) - 17.7777777778) < 0.001, "speed equipment accelerates AP");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(panel)).stop(); }
                GameProgress ringProgress = new GameProgress(List.of(0, 1, 2));
                equip(ringProgress, 2, "ring_of_power");
                BattlePanel ringPanel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed());
                ringPanel.start(ringProgress, true, 0);
                advance.invoke(ringPanel, 1.0);
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                check(((Battle) battleField.get(ringPanel)).getCurrent() == student(ringProgress, 2), "ring refill selects character immediately");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(ringPanel)).stop(); }
                GameProgress timed = new GameProgress(List.of(1)); for (int i = 0; i < 4; i++) timed.advance();
                equip(timed, 1, "light_saber");
                BattlePanel dotPanel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed()); dotPanel.start(timed, false, 4);
                Battle dotBattle = (Battle) battleField.get(dotPanel);
                Enemy guardian = dotBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
                dotBattle.getEnemies().stream().filter(e -> !e.isBoss()).forEach(e -> e.takeDamage(9999));
                guardian.takeDamage(130);
                Method perform = BattlePanel.class.getDeclaredMethod("performAction", Runnable.class, boolean.class); perform.setAccessible(true);
                perform.invoke(dotPanel, (Runnable) () -> dotBattle.attack(guardian), true);
                check(dotBattle.selectStudent(student(timed, 1)), "select while damage over time is active");
                Field lastTick = BattlePanel.class.getDeclaredField("lastTickNanos"); lastTick.setAccessible(true);
                lastTick.setLong(dotPanel, System.nanoTime() - 5_100_000_000L);
                Method tick = BattlePanel.class.getDeclaredMethod("tickActionPoints"); tick.setAccessible(true); tick.invoke(dotPanel);
                check(dotBattle.isVictory() && timed.getPetot() > 0, "damage over time continues in selection and awards victory");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(dotPanel)).stop(); }
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        System.out.println("Special combat and equipment checks passed");
    }
}
