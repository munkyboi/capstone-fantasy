package game;

import game.view.MainMenuPanel;
import game.view.SelectionPanel;
import game.view.BattlePanel;
import game.view.CampPanel;
import game.view.SaveSlotPanel;
import game.io.SaveStore;
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
            Runnable showMenu = () -> {
                layout.show(screens, "menu");
                screens.getComponent(0).requestFocusInWindow();
            };
            BattlePanel[] battleHolder = new BattlePanel[1];
            CampPanel[] campHolder = new CampPanel[1];
            boolean[] saving = {false};
            SaveSlotPanel slots = new SaveSlotPanel(new SaveStore(SaveStore.defaultDirectory()), () -> {
                if (saving[0]) { layout.show(screens, "camp"); campHolder[0].requestFocusInWindow(); }
                else { showMenu.run(); }
            }, progress -> {
                campHolder[0].open(progress, false);
                layout.show(screens, "camp");
                campHolder[0].requestFocusInWindow();
            });
            CampPanel camp = new CampPanel(progress -> {
                battleHolder[0].start(progress);
                layout.show(screens, "battle");
                battleHolder[0].requestFocusInWindow();
            }, showMenu, progress -> {
                saving[0] = true;
                slots.openSave(progress);
                layout.show(screens, "slots");
                slots.requestFocusInWindow();
            });
            campHolder[0] = camp;
            BattlePanel battle = new BattlePanel(showMenu, progress -> {
                boolean hasNext = progress.advance();
                camp.open(progress, !hasNext);
                layout.show(screens, "camp");
                camp.requestFocusInWindow();
            });
            battleHolder[0] = battle;
            SelectionPanel selection = new SelectionPanel(showMenu, party -> {
                battle.start(party);
                layout.show(screens, "battle");
                battle.requestFocusInWindow();
            });
            MainMenuPanel menu = new MainMenuPanel(window, () -> {
                selection.reset();
                layout.show(screens, "selection");
                selection.requestFocusInWindow();
            }, () -> {
                saving[0] = false;
                slots.openLoad();
                layout.show(screens, "slots");
                slots.requestFocusInWindow();
            });
            screens.add(menu, "menu");
            screens.add(selection, "selection");
            screens.add(battle, "battle");
            screens.add(camp, "camp");
            screens.add(slots, "slots");
            window.setContentPane(screens);
            window.pack();
            window.setResizable(false);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            menu.requestFocusInWindow();
        });
    }
}
