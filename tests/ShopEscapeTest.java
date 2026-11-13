import game.model.GameProgress;
import game.model.Student;
import game.view.ShopPanel;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Escape uses the shop's back action ahead of window-wide screen shortcuts. */
public final class ShopEscapeTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String origin : List.of("camp", "route")) {
                String[] destination = {"shop"};
                ShopPanel shop = new ShopPanel(() -> destination[0] = origin);
                shop.open(new GameProgress(List.of(Student.ZACK)));
                KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
                Object focused = shop.getInputMap(JComponent.WHEN_FOCUSED).get(escape);
                Object ancestor = shop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(escape);
                check("shop-back".equals(focused) && focused.equals(ancestor), "shop escape takes priority " + origin);
                shop.getActionMap().get(focused).actionPerformed(new ActionEvent(shop, 0, ""));
                check(destination[0].equals(origin), "returns to " + origin);
            }
        });
        System.out.println("Carbon Market Escape checks passed");
    }
}
