package game.view;

import game.io.SaveException;
import game.io.SaveStore;
import game.model.GameProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Keyboard-operated save and load slot screen. */
public final class SaveSlotPanel extends JPanel {
    private final SaveStore store;
    private final Runnable back;
    private final Consumer<GameProgress> loaded;
    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final JLabel[] slots = {new JLabel(), new JLabel(), new JLabel()};
    private final JLabel info = new JLabel("", SwingConstants.CENTER);
    private final JLabel help = new JLabel("", SwingConstants.CENTER);
    private int slot;
    private SaveStore.Format format = SaveStore.Format.TEXT;
    private GameProgress progress;
    private boolean saving;
    private boolean overwritePending;

    /** Creates the slot screen.
     * @param store save file storage
     * @param back returns to the calling screen
     * @param loaded receives loaded progress
     */
    public SaveSlotPanel(SaveStore store, Runnable back, Consumer<GameProgress> loaded) {
        this.store = store; this.back = back; this.loaded = loaded;
        setPreferredSize(new Dimension(960, 540)); setFocusable(true);
        setLayout(new BorderLayout(20, 20)); setBackground(new Color(18, 31, 62));
        setBorder(BorderFactory.createEmptyBorder(35, 125, 35, 125));
        heading.setFont(ClassicUi.title(30)); heading.setForeground(new Color(250, 218, 143));
        add(heading, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(4, 1, 0, 8)); center.setOpaque(false);
        for (JLabel label : slots) {
            label.setForeground(Color.WHITE); label.setFont(ClassicUi.numbers(19));
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(162, 184, 215), 2),
                    BorderFactory.createEmptyBorder(8, 18, 8, 18)));
            center.add(label);
        }
        info.setForeground(new Color(250, 218, 143)); info.setFont(ClassicUi.numbers(15));
        center.add(info); add(center, BorderLayout.CENTER);
        help.setForeground(Color.WHITE); help.setFont(ClassicUi.body(14)); add(help, BorderLayout.SOUTH);
        Keys.bind(this, "UP", "slot-up", () -> move(-1));
        Keys.bind(this, "DOWN", "slot-down", () -> move(1));
        Keys.bind(this, "LEFT", "slot-left", this::switchFormat);
        Keys.bind(this, "RIGHT", "slot-right", this::switchFormat);
        Keys.bind(this, "ENTER", "slot-confirm", this::confirm);
        Keys.bind(this, "SPACE", "slot-confirm-space", this::confirm);
        Keys.bind(this, "ESCAPE", "slot-back", () -> { if (overwritePending) { overwritePending = false; refresh("Overwrite cancelled."); } else { back.run(); } });
    }
    /** Opens the screen for saving.
     * @param progress current camp progress
     */
    public void openSave(GameProgress progress) { this.progress = progress; saving = true; open(); }
    /** Opens the screen for loading. */
    public void openLoad() { progress = null; saving = false; open(); }
    private void open() { slot = 0; format = SaveStore.Format.TEXT; overwritePending = false; refresh("Choose a slot and format."); }
    private void move(int direction) { slot = Math.floorMod(slot + direction, 3); overwritePending = false; refresh("Choose a slot and format."); }
    private void switchFormat() { format = format == SaveStore.Format.TEXT ? SaveStore.Format.BINARY : SaveStore.Format.TEXT; overwritePending = false; refresh("Choose a slot and format."); }
    private void confirm() {
        try {
            if (saving) {
                if (store.exists(slot + 1, format) && !overwritePending) {
                    overwritePending = true; refresh("Slot already has a " + formatName() + " save. Enter again to overwrite; Esc cancels."); return;
                }
                store.save(slot + 1, format, progress); overwritePending = false;
                refresh("Saved successfully. Press Esc to return to camp.");
            } else {
                GameProgress result = store.load(slot + 1, format);
                loaded.accept(result);
            }
        } catch (SaveException ex) { overwritePending = false; refresh(ex.getMessage()); }
    }
    private String formatName() { return format == SaveStore.Format.TEXT ? "text" : "binary"; }
    private void refresh(String message) {
        heading.setText((saving ? "SAVE GAME" : "LOAD GAME") + "  •  " + formatName().toUpperCase());
        for (int i = 0; i < slots.length; i++) {
            ViewportFonts.setScaledIcon(slots[i], i == slot ? PixelPointer.HAND : PixelPointer.EMPTY);
            slots[i].setText("SLOT " + (i + 1) + "     "
                    + (store.exists(i + 1, format) ? "Saved game" : "Empty"));
        }
        info.setText(message);
        help.setText("↑ ↓ Slot   •   ← → Text / Binary   •   Enter Confirm   •   Esc Back");
    }
}
