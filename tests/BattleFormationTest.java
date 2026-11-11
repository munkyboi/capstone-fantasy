import game.view.BattlePanel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Checks the party's formation and position on the courtyard. */
public final class BattleFormationTest {
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    /** Runs placement checks.
     * @param args unused
     * @throws Exception if a check fails
     */
    public static void main(String[] args) throws Exception {
        BattlePanel panel = new BattlePanel(() -> { });
        Field field = BattlePanel.class.getDeclaredField("scene"); field.setAccessible(true);
        Object scene = field.get(panel);
        Method x = scene.getClass().getDeclaredMethod("partyX", int.class, int.class); x.setAccessible(true);
        Method y = scene.getClass().getDeclaredMethod("partyFootY", int.class, int.class); y.setAccessible(true);
        Method enemyX = scene.getClass().getDeclaredMethod("enemyX", int.class, int.class); enemyX.setAccessible(true);
        Method enemyY = scene.getClass().getDeclaredMethod("enemyFootY", int.class, int.class); enemyY.setAccessible(true);
        int topX = (int) x.invoke(scene, 0, 45), frontX = (int) x.invoke(scene, 1, 45), bottomX = (int) x.invoke(scene, 2, 45);
        int topY = (int) y.invoke(scene, 180, 0), middleY = (int) y.invoke(scene, 180, 1), bottomY = (int) y.invoke(scene, 180, 2);
        check(frontX > topX && topX > bottomX, "one student stands in front of two");
        check(topY < middleY && middleY < bottomY, "party rows form a triangle");
        check(bottomY < 160 && topY >= 100, "formation stays above the ledge");
        int[] expectedX = {280, 260, 295, 226, 234, 198};
        int[] expectedY = {101, 120, 139, 101, 139, 120};
        for (int i = 0; i < 6; i++) {
            int center = (int) enemyX.invoke(scene, i, 45) + 22;
            int feet = (int) enemyY.invoke(scene, 180, i);
            check(center == expectedX[i] && feet == expectedY[i], "monster slot follows the marked order");
        }
        Method slot = scene.getClass().getDeclaredMethod("enemySlot", int.class, java.util.List.class); slot.setAccessible(true);
        var enemies = java.util.List.of(new game.model.Enemy("Wisp A"), new game.model.Enemy("Wisp B"),
                new game.model.Enemy("Final Boss", true, true), new game.model.Enemy("Wisp C"));
        check((int) slot.invoke(scene, 2, enemies) == 1, "boss uses marked slot 2 regardless of list order");
        check((int) slot.invoke(scene, 0, enemies) == 0 && (int) slot.invoke(scene, 1, enemies) == 2
                && (int) slot.invoke(scene, 3, enemies) == 3, "other monsters do not overlap the boss slot");
        System.out.println("Battle formation checks passed");
    }
}
