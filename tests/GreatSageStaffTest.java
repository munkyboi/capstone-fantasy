import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Checks both sides of the double-cast probability and battle controls. */
public final class GreatSageStaffTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static GameProgress party(boolean staff) {
        GameProgress p = new GameProgress(List.of(Student.ZACK));
        if (staff) {
            p.getInventory().addEquipment("great_sage_staff");
            check(p.equip(p.getParty().get(0), Equipment.Slot.WEAPON, "great_sage_staff"), "equip staff");
        }
        return p;
    }
    private static class Roll extends Random {
        final double value; int rolls;
        Roll(double value) { this.value = value; }
        @Override public double nextDouble() { rolls++; return rolls == 1 ? value : 0; }
        @Override public int nextInt(int bound) { return bound == 2 ? 1 : 0; }
    }
    private static void outcome(double value, boolean twice) {
        GameProgress p = party(true); Student mage = p.getParty().get(0); Roll random = new Roll(value);
        Battle b = new Battle(p, random, false, 0, GameSettings.Difficulty.HARD); List<Enemy> enemies = b.getEnemies(); int sp = mage.getSp();
        check(b.selectStudent(mage) && b.ability(null, null), "cast succeeds");
        check(b.hasPendingSecondCast() == twice && random.rolls == 1 + enemies.size(), "probability boundary " + value);
        for (Enemy e : enemies) {
            int hit = Battle.damageAfterDefense(mage.getAttack() + (int) Math.ceil(mage.getBaseAttack() * 0.40), e.getDefense());
            check(e.getMaxHp() - e.getHp() == hit, "only first cast happens immediately");
        }
        if (twice) check(b.completeSecondCast() && b.getMessage().contains("casts Arcane Burst again"), "second cast resolves");
        check(mage.getSp() == sp - mage.getAbilitySpCost(), "SP charged once");
        for (Enemy e : enemies) {
            int hit = Battle.damageAfterDefense(mage.getAttack() + (int) Math.ceil(mage.getBaseAttack() * 0.40), e.getDefense());
            check(e.getMaxHp() - e.getHp() == Math.min(e.getMaxHp(), hit * (twice ? 2 : 1)), "damage applied for each cast");
        }
        check(b.getEffects().size() == enemies.size(), "latest cast produces one effect per target");
    }
    private static Object getField(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    public static void main(String[] args) throws Exception {
        outcome(0.0, true); outcome(0.499999, true); outcome(0.5, false); outcome(0.999999, false);
        GameProgress finishingParty = party(true); Battle finishing = new Battle(finishingParty, new Roll(0), false, 0);
        for (Enemy enemy : finishing.getEnemies()) enemy.takeDamage(enemy.getMaxHp() - 1);
        check(finishing.selectStudent(finishingParty.getParty().get(0)) && finishing.ability(null, null)
                && finishing.isVictory() && !finishing.hasPendingSecondCast(), "no follow-up blocks a first-cast victory");
        Roll noStaff = new Roll(0); GameProgress normal = party(false); Battle noBonus = new Battle(normal, noStaff, false, 0);
        check(noBonus.selectStudent(normal.getParty().get(0)) && noBonus.ability(null, null)
                && noStaff.rolls == noBonus.getEnemies().size() && !noBonus.getMessage().contains("casts Arcane Burst again"), "staff required");
        Random rolls = new Random(51773); int doubled = 0, attempts = 5000;
        for (int i = 0; i < attempts; i++) {
            double roll = rolls.nextDouble(); GameProgress p = party(true); Battle b = new Battle(p, new Roll(roll), false, 0, GameSettings.Difficulty.HARD);
            b.selectStudent(p.getParty().get(0)); b.ability(null, null);
            if (b.hasPendingSecondCast()) { doubled++; b.completeSecondCast(); }
        }
        check(doubled > 2350 && doubled < 2650, "frequency is consistent with 50 percent: " + doubled);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress progress = party(true); Student mage = progress.getParty().get(0);
                progress.advance();
                BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), new Roll(0));
                panel.start(progress, false, 1);
                int sp = mage.getSp();
                for (String key : List.of("battle-down", "battle-confirm", "battle-confirm")) {
                    panel.getActionMap().get(key).actionPerformed(new ActionEvent(panel, 0, key));
                }
                Field field = BattlePanel.class.getDeclaredField("battle"); field.setAccessible(true); Battle b = (Battle) field.get(panel);
                check(b.hasPendingSecondCast() && mage.getSp() == sp - mage.getAbilitySpCost(), "keyboard starts first cast");
                Field points = BattlePanel.class.getDeclaredField("actionPoints"); points.setAccessible(true);
                @SuppressWarnings("unchecked") java.util.Map<Student, Double> ap = (java.util.Map<Student, Double>) points.get(panel);
                check(ap.get(mage) == 0.0, "double cast consumes one action bar");
                int firstHp = b.getEnemies().get(0).getHp();
                check(firstHp > 0 && ((javax.swing.JButton) getField(panel, "ability")).isEnabled() == false, "first cast happens and controls lock");
                Field attacker = BattlePanel.class.getDeclaredField("attackingSprite"); attacker.setAccessible(true);
                Field started = BattlePanel.class.getDeclaredField("effectStartedNanos"); started.setAccessible(true);
                long firstStart = started.getLong(panel);
                check(attacker.get(panel) == mage, "first cast animates Zack");
                javax.swing.Timer timer = (javax.swing.Timer) getField(panel, "secondCastTimer");
                check(timer.isRunning() && timer.getInitialDelay() == 700, "second cast is scheduled with a delay");
                timer.stop();
                for (java.awt.event.ActionListener listener : timer.getActionListeners()) listener.actionPerformed(new ActionEvent(timer, 0, "test"));
                check(!b.hasPendingSecondCast() && b.getEnemies().get(0).getHp() < firstHp && b.getMessage().contains("casts Arcane Burst again"), "second cast deals damage after delay");
                check(attacker.get(panel) == mage && started.getLong(panel) > firstStart, "second cast restarts Zack's animation");
                Object scene = getField(panel, "scene");
                java.lang.reflect.Method offset = scene.getClass().getDeclaredMethod("attackOffset", Object.class, int.class); offset.setAccessible(true);
                started.setLong(panel, System.nanoTime() - 90_000_000L);
                check((int) offset.invoke(scene, mage, 1) > 0, "Zack hops forward again during second cast");
                check(mage.getSp() == sp - mage.getAbilitySpCost() && ap.get(mage) == 0.0, "second cast costs no additional SP or AP");
                for (String timerName : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer")) {
                    Field f = BattlePanel.class.getDeclaredField(timerName); f.setAccessible(true); ((javax.swing.Timer) f.get(panel)).stop();
                }
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        java.util.concurrent.CountDownLatch completed = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress progress = party(true); BattlePanel panel = new BattlePanel(() -> {}, ignored -> {}, new GameSettings(), new Roll(0));
                progress.advance();
                panel.start(progress, false, 1);
                for (String key : List.of("battle-down", "battle-confirm", "battle-confirm"))
                    panel.getActionMap().get(key).actionPerformed(new ActionEvent(panel, 0, key));
                Battle battle = (Battle) getField(panel, "battle");
                check(battle.hasPendingSecondCast(), "follow-up waits after first cast");
                javax.swing.Timer verify = new javax.swing.Timer(1000, event -> {
                    try { check(!battle.hasPendingSecondCast() && battle.getMessage().contains("again"), "real timer resolves the follow-up"); }
                    catch (Throwable error) { failure.set(error); }
                    finally {
                        try { for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer")) ((javax.swing.Timer) getField(panel, name)).stop(); }
                        catch (ReflectiveOperationException error) { failure.set(error); }
                        completed.countDown();
                    }
                });
                verify.setRepeats(false); verify.start();
            } catch (Throwable error) { failure.set(error); completed.countDown(); }
        });
        check(completed.await(3, java.util.concurrent.TimeUnit.SECONDS), "real follow-up timer completed");
        if (failure.get() != null) throw new AssertionError("real timer test", failure.get());
        System.out.println("Great Sage Staff checks passed; " + doubled + "/" + attempts + " doubled");
    }
}
