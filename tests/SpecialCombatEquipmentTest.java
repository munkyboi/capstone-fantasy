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
        check(boss.getBossCountdown() == 0 && b.enemyAttack(boss) && boss.getBossCountdown() == 5, "boss starts charge");
        for (int i=4;i>=1;i--) { p.getParty().forEach(h -> h.heal(9999)); check(b.enemyAttack(boss) && boss.getBossCountdown() == i, "boss countdown " + i); }
        p.getParty().forEach(h -> h.heal(9999));
        check(b.enemyAttack(boss) && b.getMessage().contains("Crystal Pulse") && boss.getBossCountdown() == 0, "pulse resolves after countdown");
        while (b.hasPendingGuardianLoop()) b.resolveGuardianLoop();
        GameProgress support = new GameProgress(List.of(0, 1, 2)); support.advance();
        Battle sb = new Battle(support, fixed(), false, 1); Enemy sentinel = sb.getEnemies().stream().filter(Enemy::isSentinel).findFirst().orElseThrow();
        Enemy ally = sb.getEnemies().stream().filter(e -> !e.isSentinel()).findFirst().orElseThrow();
        ally.takeDamage(40); int before = ally.getHp();
        check(sb.enemyAttack(sentinel) && sb.getMessage().contains("heals") && ally.getHp() == before + 13 && sentinel.getSp() == 885,
                "Sentinel heals 20 percent of the ally's maximum HP and spends SP");
        check(sb.getEffects().get(0).getAmount() == 13 && sb.getEffects().get(0).getEnemy() == ally, "healing indicator targets ally");
        Field sentinelSp = Enemy.class.getDeclaredField("skillPoints"); sentinelSp.setAccessible(true); sentinelSp.setInt(sentinel, 15);
        for (int i = 0; i < 4; i++) { support.getParty().forEach(s -> s.heal(999)); check(sb.enemyAttack(sentinel), "Sentinel waits during cooldown"); }
        ally.takeDamage(Math.max(0, ally.getHp() - 25));
        check(sb.enemyAttack(sentinel) && sb.getMessage().contains("heals"), "Sentinel uses remaining SP after cooldown");
        check(sentinel.getSp() == 0, "Sentinel spends available SP");
        check(sb.enemyAttack(sentinel) && !sb.getMessage().contains("heals"), "Sentinel cannot heal without SP");
        Enemy hardSentinel = new Enemy("Stone Sentinel", true, false, GameSettings.Difficulty.HARD);
        check(hardSentinel.getMaxSp() == 900 && hardSentinel.getSp() == 900, "Sentinel SP is fixed across difficulties");
        GameProgress market = new GameProgress(List.of(0, 1, 2, 3)); market.addPetot(13000);
        for (int i = 2; i < 7; i++) check(CarbonMarket.buy(market, i).startsWith("Purchased"), "buy market item " + i);
        check(market.getPetot() == 500 && market.getInventory().getEquipmentIds().contains("light_saber"), "purchase price and items");
        equip(market, 0, "great_sage_staff"); equip(market, 1, "light_saber"); equip(market, 2, "demon_hunter"); equip(market, 3, "giant_slayer_gloves"); equip(market, 2, "ring_of_power");
        Student hunter = student(market, 2); check(hunter.getAttack() == 19 && hunter.getSpeed() == 43 && hunter.getDefense() == 8, "percentage stats stack additively");
        check(student(market, 1).getAttack() == 116 && student(market, 1).getSpeed() == 18, "Light Saber stats");
        check(student(market, 3).getAttack() == 38 && student(market, 3).getSpeed() == 20, "glove stats");
        market.advance(); Path dir = Files.createTempDirectory("special-equipment-");
        try {
            SaveStore store = new SaveStore(dir);
            for (SaveStore.Format format : SaveStore.Format.values()) {
                store.save(1, format, market); GameProgress loaded = store.load(1, format);
                check(student(loaded, 2).getSpeed() == 43 && student(loaded, 1).getAttack() == 116, "legendary equipment save " + format);
            }
        } finally { try (var files = Files.walk(dir)) { for (Path f : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(f); } }
        GameProgress fire = finalParty(); equip(fire, 1, "light_saber"); Battle burn = new Battle(fire, fixed(), false, 4, GameSettings.Difficulty.INSANE);
        Enemy big = burn.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow(); Student warrior = student(fire, 1);
        check(burn.selectStudent(warrior), "select warrior"); int initial = big.getHp(); burn.attack(big); int hit = initial - big.getHp();
        check(burn.hasDamageOverTime(big), "Light Saber applies the shared DOT status");
        for (int i = 0; i < 5; i++) check(burn.tickDamageOverTime(big), "one tick per target turn");
        check(initial - big.getHp() == hit * 2, "five turns deal 100 percent of hit");
        check(!burn.hasDamageOverTime(big) && !burn.tickDamageOverTime(big), "DOT status and damage stop after five ticks");
        GameProgress mageParty = new GameProgress(List.of(0, 1, 2)); equip(mageParty, 0, "great_sage_staff"); mageParty.advance();
        Battle casts = new Battle(mageParty, fixed(), false, 1); Student mage = student(mageParty, 0); int sp = mage.getSp();
        check(casts.selectStudent(mage) && casts.ability(null, null) && casts.hasPendingSecondCast()
                && casts.completeSecondCast() && casts.getMessage().contains("again")
                && mage.getSp() == sp - mage.getAbilitySpCost(), "double cast spends SP once");
        GameProgress monkParty = finalParty(); equip(monkParty, 3, "giant_slayer_gloves"); Battle punches = new Battle(monkParty, fixed(), false, 4);
        check(punches.selectStudent(student(monkParty, 3)) && punches.attack(punches.getEnemies().get(0))
                && punches.hasPendingAngelAttack(), "gloves start timed combo");
        int punchCount = 1; while (!punches.isAngelExplosionPending() && punches.hasPendingAngelAttack()) { punches.advanceAngelAttack(); punchCount++; }
        check(punchCount == 5, "gloves add two basic hits"); punches.advanceAngelAttack();
        GameProgress ringParty = new GameProgress(List.of(0, 1, 2)); Student first = ringParty.getParty().get(0); equip(ringParty, first.getId(), "ring_of_power");
        Battle ring = new Battle(ringParty, fixed()); check(ring.enemyAttack(ring.getEnemies().get(0))
                && ring.consumeApRefills().contains(first), "ring can refill AP when damaged");
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress speedParty = new GameProgress(List.of(0, 1, 2)); equip(speedParty, 2, "demon_hunter");
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed()); panel.start(speedParty, true, 0);
                Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                Field speedEnemies = BattlePanel.class.getDeclaredField("enemyPoints"); speedEnemies.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Enemy, Double> speedEnemyBars = (Map<Enemy, Double>) speedEnemies.get(panel); speedEnemyBars.replaceAll((e,v)->-1000.0);
                advance.invoke(panel, 1.0);
                Field points = BattlePanel.class.getDeclaredField("actionPoints"); points.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Student, Double> values = (Map<Student, Double>) points.get(panel);
                check(Math.abs(values.get(student(speedParty, 2)) - 17.0) < 0.001, "speed equipment accelerates AP");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(panel)).stop(); }
                GameProgress ringProgress = new GameProgress(List.of(0, 1, 2));
                equip(ringProgress, 2, "ring_of_power");
                BattlePanel ringPanel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed());
                ringPanel.start(ringProgress, true, 0);
                @SuppressWarnings("unchecked") Map<Enemy, Double> ringEnemies = (Map<Enemy, Double>) speedEnemies.get(ringPanel);
                ringEnemies.replaceAll((e,v)->(double)e.getActionThreshold());
                advance.invoke(ringPanel, 0.0); advance.invoke(ringPanel, 0.0);
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                check(((Battle) battleField.get(ringPanel)).getCurrent() == student(ringProgress, 2), "ring refill selects character immediately");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(ringPanel)).stop(); }
                GameProgress timed = new GameProgress(List.of(1)); for (int i = 0; i < 4; i++) timed.advance();
                equip(timed, 1, "light_saber");
                BattlePanel dotPanel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), fixed()); dotPanel.start(timed, false, 4);
                Battle dotBattle = (Battle) battleField.get(dotPanel);
                Enemy guardian = dotBattle.getEnemies().stream().filter(Enemy::isBoss).findFirst().orElseThrow();
                dotBattle.getEnemies().stream().filter(e -> !e.isBoss()).forEach(e -> e.takeDamage(9999));
                Method perform = BattlePanel.class.getDeclaredMethod("performAction", Runnable.class, boolean.class); perform.setAccessible(true);
                perform.invoke(dotPanel, (Runnable) () -> dotBattle.attack(guardian), true);
                check(dotBattle.selectStudent(student(timed, 1)), "select while damage over time is active");
                guardian.takeDamage(guardian.getHp() - 1);
                Field lastDot = BattlePanel.class.getDeclaredField("lastDotNanos"); lastDot.setAccessible(true);
                lastDot.setLong(dotPanel, System.nanoTime() - 1_010_000_000L);
                Field lastTick = BattlePanel.class.getDeclaredField("lastTickNanos"); lastTick.setAccessible(true);
                lastTick.setLong(dotPanel, System.nanoTime());
                Method tick = BattlePanel.class.getDeclaredMethod("tickActionPoints"); tick.setAccessible(true); tick.invoke(dotPanel);
                check(dotBattle.isVictory() && timed.getPetot() > 0, "DOT ticks during selection and awards victory");
                for (String timer : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer")) { Field f = BattlePanel.class.getDeclaredField(timer); f.setAccessible(true); ((javax.swing.Timer) f.get(dotPanel)).stop(); }
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        });
        System.out.println("Special combat and equipment checks passed");
    }
}
