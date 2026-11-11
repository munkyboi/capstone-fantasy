package game.view;

import javax.swing.JPanel;

/** Layers the battle controls over the bottom of the battlefield. */
final class BattleOverlay extends JPanel {
    private static final double CONTROLS_HEIGHT = 0.23;
    private final JPanel scene;
    private final JPanel controls;

    BattleOverlay(JPanel scene, JPanel controls) {
        this.scene = scene;
        this.controls = controls;
        setLayout(null);
        setOpaque(false);
        add(controls);
        add(scene);
    }

    @Override public void doLayout() {
        int height = getHeight();
        int controlsHeight = (int) Math.round(height * CONTROLS_HEIGHT);
        scene.setBounds(0, 0, getWidth(), height);
        controls.setBounds(0, height - controlsHeight, getWidth(), controlsHeight);
    }

    @Override public boolean isOptimizedDrawingEnabled() { return false; }
}
