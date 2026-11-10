package game;

import game.view.MainMenuPanel;
import game.view.SelectionPanel;
import java.awt.CardLayout;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Starts Capstone Fantasy. */
public final class Main {
    private Main() { }

    /** Opens the game window on Swing's event dispatch thread.
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Capstone Fantasy");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            CardLayout layout = new CardLayout();
            JPanel screens = new JPanel(layout);
            SelectionPanel selection = new SelectionPanel(() -> layout.show(screens, "menu"));
            screens.add(new MainMenuPanel(window, () -> {
                selection.reset();
                layout.show(screens, "selection");
            }), "menu");
            screens.add(selection, "selection");
            window.setContentPane(screens);
            window.pack();
            window.setMinimumSize(window.getSize());
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
