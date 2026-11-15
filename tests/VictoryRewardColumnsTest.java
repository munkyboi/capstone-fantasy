import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.IntSupplier;

/** Verifies that large victory rewards fit in two columns above the controls. */
public final class VictoryRewardColumnsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Class<?> stateClass = Class.forName("game.view.BattleScene$State");
        Object state = Proxy.newProxyInstance(stateClass.getClassLoader(), new Class<?>[]{stateClass}, (proxy, method, arguments) -> switch (method.getName()) {
            case "rewardItems" -> List.of("focus_charm", "hunter_crossbows", "cadet_blade", "champion_wraps",
                    "scholar_staff", "guardian_band", "campus_medal", "white_flower", "focus_charm");
            case "rewardExperience" -> 600;
            case "rewardPetot" -> 480;
            case "leveledStudents" -> List.of("Charice", "Bogart", "Angel");
            case "bonusXpStudents" -> List.of("Charice +600");
            case "canExplore" -> true;
            case "victoryChoice" -> 1;
            default -> throw new AssertionError("Unexpected state method: " + method.getName());
        });
        Class<?> overlays = Class.forName("game.view.BattleHudOverlays");
        Constructor<?> constructor = overlays.getDeclaredConstructor(stateClass, IntSupplier.class, BufferedImage.class, BufferedImage[].class);
        constructor.setAccessible(true);
        Object renderer = constructor.newInstance(state, (IntSupplier) () -> 540, null, new BufferedImage[5]);
        Method rows = overlays.getDeclaredMethod("rewardRowCount"); rows.setAccessible(true);
        check((int) rows.invoke(renderer) == 7, "ten distinct entries occupy five rows, plus two summaries");
        Method buttons = overlays.getDeclaredMethod("rewardButtonY"); buttons.setAccessible(true);
        check((int) buttons.invoke(renderer) + 22 < 540 * 0.77, "choices remain above battle controls");
        BufferedImage canvas = new BufferedImage(960, 540, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        Method draw = overlays.getDeclaredMethod("drawRewards", Graphics2D.class); draw.setAccessible(true);
        draw.invoke(renderer, g); g.dispose();
        int petotIcon = canvas.getRGB(487, 202);
        check(new Color(petotIcon, true).getAlpha() > 0 && (petotIcon & 0x00ffffff) != 0x000c1930,
                "second column renders the Petot icon");
        System.out.println("Victory reward column checks passed");
    }
}
