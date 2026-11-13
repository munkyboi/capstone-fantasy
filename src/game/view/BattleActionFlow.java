package game.view;

import game.model.Student;

/** Coordinates the delayed visual steps of a battle action. */
final class BattleActionFlow {
    private static final double AP_EPSILON = 0.0000001;
    private final BattlePanel panel;
    BattleActionFlow(BattlePanel panel) { this.panel = panel; }
    void performAction(Runnable action, boolean attacking) {
        if (panel.actionBattle() == null || panel.actionBattle().isOver() || panel.actionBattle().isActionAnimating() || panel.actionBattle().getCurrent() == null
                || panel.actionPoints().get(panel.actionBattle().getCurrent()) + AP_EPSILON < panel.actionBattle().getCurrent().getActionThreshold()) { return; }
        Student actor = panel.actionBattle().getCurrent();
        action.run();
        if (attacking && actor.getId() == Student.CHARICE && panel.actionBattle().getLastAbility() != null
                && "Rain of Arrows".equals(panel.actionBattle().getLastAbility().skill())) {
            panel.actionRain(actor);
        }
        if (panel.actionBattle().hasPendingTwinShot()) {
            panel.resetStudentAp(actor);
            panel.showActionEffects(attacking ? actor : null);
            scheduleTwinShot();
        } else if (panel.actionBattle().hasPendingAngelAttack()) {
            panel.resetStudentAp(actor);
            panel.showActionEffects(attacking ? actor : null);
            scheduleAngelAttack();
        } else if (panel.actionBattle().hasPendingLooperHits()) {
            panel.resetStudentAp(actor);
            panel.showActionEffects(attacking ? actor : null);
            panel.actionLooperTimer().restart();
        } else if (panel.actionBattle().hasPendingSecondCast()) {
            panel.resetStudentAp(actor);
            panel.showActionEffects(attacking ? actor : null);
            panel.actionSecondCastTimer().restart();
        } else if (panel.actionBattle().getCurrent() != actor || panel.actionBattle().isOver()) {
            panel.resetStudentAp(actor);
            panel.showActionEffects(attacking ? actor : null);
            if (!panel.actionBattle().isOver()) { panel.selectReadyStudent(); }
        }
        if (panel.actionBattle().isOver()) { panel.actionApTimer().stop(); }
        panel.actionTickNow();
        panel.refresh();
    }

    void completeSecondCast() {
        if (panel.actionBattle() == null || !panel.actionBattle().hasPendingSecondCast()) return;
        Student actor = panel.actionBattle().getCurrent();
        if (panel.actionBattle().completeSecondCast()) {
            panel.announceAbility();
            panel.showActionEffects(actor);
            if (panel.actionBattle().hasPendingLooperHits()) panel.actionLooperTimer().restart();
            else if (!panel.actionBattle().isOver()) panel.selectReadyStudent();
        }
        if (panel.actionBattle().isOver()) panel.actionApTimer().stop();
        panel.actionTickNow();
        panel.refresh();
    }

    void advanceLooperHit() {
        if (panel.actionBattle() == null || !panel.actionBattle().hasPendingLooperHits()) { panel.actionLooperTimer().stop(); return; }
        panel.actionBattle().advanceLooperHit();
        panel.showActionEffects(null);
        if (!panel.actionBattle().hasPendingLooperHits()) {
            panel.actionLooperTimer().stop();
            if (panel.actionBattle().hasPendingAngelAttack() && !panel.actionBattle().isOver()) scheduleAngelAttack();
            else if (panel.actionBattle().hasPendingSecondCast() && !panel.actionBattle().isOver()) panel.actionSecondCastTimer().restart();
            else if (!panel.actionBattle().isOver()) panel.selectReadyStudent();
        }
        if (panel.actionBattle().isOver()) panel.actionApTimer().stop();
        panel.actionTickNow();
        panel.refresh();
    }

    void scheduleTwinShot() {
        panel.actionTwinTimer().setInitialDelay(panel.actionBattle().twinShotDelayMillis());
        panel.actionTwinTimer().restart();
    }
    void advanceTwinShot() {
        if (panel.actionBattle() == null || !panel.actionBattle().hasPendingTwinShot()) return;
        Student actor = panel.actionBattle().getCurrent();
        panel.actionBattle().advanceTwinShot();
        panel.showActionEffects(actor);
        if (panel.actionBattle().hasPendingTwinShot()) scheduleTwinShot();
        else if (panel.actionBattle().hasPendingLooperHits()) panel.actionLooperTimer().restart();
        else if (!panel.actionBattle().isOver()) panel.selectReadyStudent();
        if (panel.actionBattle().isOver()) panel.actionApTimer().stop();
        panel.actionTickNow();
        panel.refresh();
    }
    void scheduleAngelAttack() {
        panel.actionAngelTimer().setInitialDelay(300);
        panel.actionAngelTimer().restart();
    }
    void resolveSparkExplosion() {
        if (panel.actionBattle() == null) return;
        boolean resolved = panel.actionBattle().hasPendingGuardianLoop() ? panel.actionBattle().resolveGuardianLoop() : panel.actionBattle().resolveSparkExplosion();
        if (!resolved) return;
        panel.actionBattle().cancelEscapeIfKnockedOut();
        panel.applyApRefills();
        panel.showActionEffects(null);
        if (panel.actionBattle().hasPendingGuardianLoop() && !panel.actionBattle().isOver()) panel.actionSparkTimer().restart();
        if (panel.actionBattle().isOver()) panel.actionApTimer().stop();
        panel.actionTickNow();
        panel.refresh();
    }
    void advanceAngelAttack() {
        if (panel.actionBattle() == null || !panel.actionBattle().hasPendingAngelAttack()) return;
        Student actor = panel.actionBattle().getCurrent();
        panel.actionBattle().advanceAngelAttack();
        panel.showActionEffects(actor);
        if (panel.actionBattle().hasPendingLooperHits()) panel.actionLooperTimer().restart();
        else if (panel.actionBattle().hasPendingAngelAttack()) scheduleAngelAttack();
        else if (!panel.actionBattle().isOver()) panel.selectReadyStudent();
        if (panel.actionBattle().isOver()) panel.actionApTimer().stop();
        panel.actionTickNow();
        panel.refresh();
    }

}
