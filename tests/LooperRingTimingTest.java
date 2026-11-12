import game.model.*;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class LooperRingTimingTest {
    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                GameProgress progress = new GameProgress(List.of(Student.BOGART));
                Student hero = progress.getParty().get(0);
                progress.getInventory().addEquipment("looper_ring");
                check(progress.equip(hero, Equipment.Slot.ACCESSORY, "looper_ring"), "equip ring");
                Random random = new Random() {
                    @Override public double nextDouble() { return 0; }
                    @Override public int nextInt(int bound) { return 0; }
                };
                BattlePanel panel = new BattlePanel(() -> {}, p -> {}, new GameSettings(), random);
                panel.start(progress, false, 0);
                Battle battle = (Battle) field(panel, "battle");
                Enemy enemy = battle.getEnemies().get(0);
                panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel, 0, ""));
                panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel, 0, ""));
                Timer timer = (Timer) field(panel, "looperHitTimer");
                check(timer.isRunning() && timer.getInitialDelay() == 300 && timer.getDelay() == 300, "300 ms between hits");
                int hp = enemy.getHp();
                check(!battle.attack(enemy), "actions are locked during extra hits");
                timer.stop();
                for (int i = 0; i < 5; i++) {
                    for (var listener : timer.getActionListeners()) listener.actionPerformed(new ActionEvent(timer, 0, ""));
                    check(enemy.getHp() < hp, "hit " + (i + 1) + " deals damage");
                    hp = enemy.getHp();
                }
                check(!battle.hasPendingLooperHits(), "sequence ends after five hits");
                for (String name : List.of("effectTimer", "ambushAlertTimer", "victoryTimer", "apTimer", "secondCastTimer", "looperHitTimer", "abilityAlertTimer")) ((Timer) field(panel, name)).stop();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Looper Ring timing checks passed");
    }
}
