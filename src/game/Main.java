package game;

import game.view.MainMenuPanel;
import game.view.AspectRatioPanel;
import game.view.PixelShell;
import game.view.SelectionPanel;
import game.view.BattlePanel;
import game.view.CampPanel;
import game.view.InventoryPanel;
import game.view.SaveSlotPanel;
import game.view.WorldMapPanel;
import game.view.ShopPanel;
import game.view.SettingsPanel;
import game.io.SaveStore;
import game.model.GameProgress;
import game.model.GameSettings;
import java.awt.CardLayout;
import java.util.Random;
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
            screens.setPreferredSize(new java.awt.Dimension(960, 720));
            Runnable showMenu = () -> {
                layout.show(screens, "menu");
                ((AspectRatioPanel) screens.getComponent(0)).getComponent(0).requestFocusInWindow();
            };
            BattlePanel[] battleHolder = new BattlePanel[1];
            java.nio.file.Path settingsPath = SaveStore.defaultDirectory().getParent().resolve("settings.properties");
            GameSettings settings = GameSettings.load(settingsPath);
            GameProgress[] pendingParty = {null};
            SelectionPanel[] selectionHolder = new SelectionPanel[1];
            CampPanel[] campHolder = new CampPanel[1];
            InventoryPanel[] inventoryHolder = new InventoryPanel[1];
            WorldMapPanel[] mapHolder = new WorldMapPanel[1];
            boolean[] saving = {false};
            int[] activeStage = {0};
            Random random = new Random();
            SaveSlotPanel slots = new SaveSlotPanel(new SaveStore(SaveStore.defaultDirectory()), () -> {
                if (saving[0]) { layout.show(screens, "camp"); campHolder[0].requestFocusInWindow(); }
                else { showMenu.run(); }
            }, progress -> {
                if (progress.getParty().size() != settings.getPartySize()) {
                    pendingParty[0] = progress;
                    selectionHolder[0].configure(settings.getPartySize(), progress.getParty().stream().map(game.model.Student::getId).toList());
                    layout.show(screens, "selection"); selectionHolder[0].requestFocusInWindow();
                } else {
                    progress.applySettings(settings);
                    campHolder[0].open(progress, false);
                    layout.show(screens, "camp"); campHolder[0].requestFocusInWindow();
                }
            });
            CampPanel camp = new CampPanel(progress -> {
                mapHolder[0].open(progress);
                layout.show(screens, "map");
                mapHolder[0].requestFocusInWindow();
            }, showMenu, progress -> {
                saving[0] = true;
                slots.openSave(progress);
                layout.show(screens, "slots");
                slots.requestFocusInWindow();
            }, progress -> {
                inventoryHolder[0].open(progress);
                layout.show(screens, "inventory");
                inventoryHolder[0].requestFocusInWindow();
            });
            campHolder[0] = camp;
            InventoryPanel inventory = new InventoryPanel(() -> {
                camp.open(campHolder[0].getProgress(), false);
                layout.show(screens, "camp");
                camp.requestFocusInWindow();
            });
            inventoryHolder[0] = inventory;
            BattlePanel battle = new BattlePanel(showMenu, progress -> {
                boolean replay = activeStage[0] < progress.getEncounter();
                if (!replay) { progress.advance(); }
                boolean hasNext = progress.getEncounter() < 5;
                for (game.model.Student student : progress.getParty()) { student.restoreAllSp(); }
                camp.open(progress, !hasNext);
                layout.show(screens, "camp");
                camp.requestFocusInWindow();
            }, settings);
            battleHolder[0] = battle;
            WorldMapPanel map = new WorldMapPanel((progress, stage) -> {
                activeStage[0] = stage;
                boolean ambushed = settings.rollAmbush(stage, random);
                battle.start(progress, ambushed, stage);
                layout.show(screens, "battle");
                battle.requestFocusInWindow();
            }, progress -> {
                camp.open(progress, false);
                layout.show(screens, "camp");
                camp.requestFocusInWindow();
            }, showMenu);
            mapHolder[0] = map;
            ShopPanel shop = new ShopPanel(() -> {
                map.open(campHolder[0].getProgress());
                layout.show(screens, "map"); map.requestFocusInWindow();
            });
            map.setShopAction(progress -> {
                shop.open(progress); layout.show(screens, "shop"); shop.requestFocusInWindow();
            });
            SelectionPanel selection = new SelectionPanel(showMenu, party -> {
                if (pendingParty[0] != null) {
                    GameProgress loaded = pendingParty[0];
                    try { loaded.setActiveParty(party); }
                    catch (IllegalArgumentException ex) {
                        javax.swing.JOptionPane.showMessageDialog(window, "Choose at least one student who is not knocked out."); return;
                    }
                    pendingParty[0] = null; loaded.applySettings(settings); camp.open(loaded, false);
                    layout.show(screens, "camp"); camp.requestFocusInWindow();
                } else {
                    GameProgress progress = new GameProgress(party); progress.applySettings(settings);
                    map.open(progress); layout.show(screens, "map"); map.requestFocusInWindow();
                }
            });
            selectionHolder[0] = selection;
            SettingsPanel settingsPanel = new SettingsPanel(settings, settingsPath, showMenu);
            MainMenuPanel menu = new MainMenuPanel(window, () -> {
                pendingParty[0] = null;
                selection.configure(settings.getPartySize(), java.util.List.of());
                layout.show(screens, "selection");
                selection.requestFocusInWindow();
            }, () -> {
                saving[0] = false;
                slots.openLoad();
                layout.show(screens, "slots");
                slots.requestFocusInWindow();
            }, () -> { layout.show(screens, "settings"); settingsPanel.requestFocusInWindow(); });
            screens.add(new AspectRatioPanel(menu), "menu");
            screens.add(new AspectRatioPanel(selection), "selection");
            screens.add(battle, "battle");
            screens.add(new AspectRatioPanel(camp), "camp");
            screens.add(new AspectRatioPanel(inventory), "inventory");
            screens.add(new AspectRatioPanel(map), "map");
            screens.add(new AspectRatioPanel(shop), "shop");
            screens.add(new AspectRatioPanel(slots), "slots");
            screens.add(new AspectRatioPanel(settingsPanel), "settings");
            window.setContentPane(PixelShell.wrap(screens));
            window.pack();
            window.setMinimumSize(window.getSize());
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            menu.requestFocusInWindow();
        });
    }
}
