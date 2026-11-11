import game.model.Battle;
import game.model.Enemy;
import game.model.GameProgress;
import game.model.Student;
import game.view.BattlePanel;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Checks which combat events animate, and the direction and duration of movement. */
public final class AnimationTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void key(JPanel panel, String name) {
        Action action = panel.getActionMap().get(name);
        if (action == null) { throw new AssertionError("Missing action " + name); }
        action.actionPerformed(new ActionEvent(panel, 0, name));
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) { layout((Container) child); }
        }
    }
    private static boolean hasWhiteOutline(JPanel panel, int color) {
        panel.setSize(960, 540); layout(panel);
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics graphics = image.getGraphics();
        try { panel.paint(graphics); } finally { graphics.dispose(); }
        for (int y = 1; y < 350; y++) {
            for (int x = 1; x < 959; x++) {
                if ((image.getRGB(x, y) & 0xffffff) != color) { continue; }
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((image.getRGB(x + dx, y + dy) & 0xffffff) == 0xffffff) { return true; }
                    }
                }
            }
        }
        return false;
    }
    /** Runs the animation checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Method advance = BattlePanel.class.getDeclaredMethod("advanceActionPoints", double.class); advance.setAccessible(true);
                Field battleField = BattlePanel.class.getDeclaredField("battle"); battleField.setAccessible(true);
                Field sceneField = BattlePanel.class.getDeclaredField("scene"); sceneField.setAccessible(true);
                Field attackerField = BattlePanel.class.getDeclaredField("attackingSprite"); attackerField.setAccessible(true);
                Field startedField = BattlePanel.class.getDeclaredField("effectStartedNanos"); startedField.setAccessible(true);
                Field showingField = BattlePanel.class.getDeclaredField("showEffects"); showingField.setAccessible(true);
                Field timerField = BattlePanel.class.getDeclaredField("effectTimer"); timerField.setAccessible(true);
                Field pointsField = BattlePanel.class.getDeclaredField("actionPoints"); pointsField.setAccessible(true);

                BattlePanel player = new BattlePanel(() -> { }); player.start(List.of(0, 1, 2));
                advance.invoke(player, 10.0);
                Battle fight = (Battle) battleField.get(player);
                Student actor = fight.getCurrent();
                key(player, "battle-confirm"); key(player, "battle-confirm");
                check(attackerField.get(player) == actor && showingField.getBoolean(player), "student attack animates");
                check(((Timer) timerField.get(player)).isRunning(), "animation timer runs");
                Object scene = sceneField.get(player);
                Method offset = scene.getClass().getDeclaredMethod("attackOffset", Object.class, int.class); offset.setAccessible(true);
                startedField.setLong(player, System.nanoTime() - 90_000_000L);
                check((int) offset.invoke(scene, actor, 1) > 0, "student moves toward enemies");
                Method lift = scene.getClass().getDeclaredMethod("attackLift", Object.class); lift.setAccessible(true);
                check((int) lift.invoke(scene, actor) < 0, "student hops upward");
                startedField.setLong(player, System.nanoTime() - 250_000_000L);
                check((int) offset.invoke(scene, actor, 1) == 0, "student returns after attack");
                startedField.setLong(player, System.nanoTime());
                check(hasWhiteOutline(player, 0xff6464), "damage text has a white outline");

                BattlePanel monster = new BattlePanel(() -> { }); monster.start(new GameProgress(List.of(0, 1, 3)), true);
                advance.invoke(monster, 8.0);
                Object attackingEnemy = attackerField.get(monster);
                check(attackingEnemy instanceof Enemy && showingField.getBoolean(monster), "monster attack animates");
                Object monsterScene = sceneField.get(monster);
                startedField.setLong(monster, System.nanoTime() - 90_000_000L);
                check((int) offset.invoke(monsterScene, attackingEnemy, -1) < 0, "monster moves toward party");

                GameProgress progress = new GameProgress(List.of(0, 1, 3));
                Student zack = progress.getParty().stream().filter(s -> s.getId() == 0).findFirst().orElseThrow();
                Student angel = progress.getParty().stream().filter(s -> s.getId() == 3).findFirst().orElseThrow();
                zack.takeDamage(20);
                BattlePanel healer = new BattlePanel(() -> { }); healer.start(progress, true);
                Battle healingBattle = (Battle) battleField.get(healer);
                check(healingBattle.selectStudent(angel), "select healer");
                @SuppressWarnings("unchecked") Map<Student, Double> points = (Map<Student, Double>) pointsField.get(healer);
                points.put(angel, (double) angel.getActionThreshold());
                Method refresh = BattlePanel.class.getDeclaredMethod("refresh"); refresh.setAccessible(true); refresh.invoke(healer);
                key(healer, "battle-down"); key(healer, "battle-confirm"); key(healer, "battle-confirm");
                check(showingField.getBoolean(healer) && attackerField.get(healer) == null, "heal animates without attack lunge");
                check(healingBattle.getEffects().stream().anyMatch(effect -> effect.getStudent() == zack && effect.getAmount() > 0), "healing indicator targets ally");
                startedField.setLong(healer, System.nanoTime());
                check(hasWhiteOutline(healer, 0x5cff85), "healing text has a white outline");
                ((Timer) timerField.get(player)).stop(); ((Timer) timerField.get(monster)).stop(); ((Timer) timerField.get(healer)).stop();
                System.out.println("Battle animation checks passed");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        });
    }
}
