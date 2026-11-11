package game.model;

/** Game-wide settings; a settings screen can edit these in a future update. */
public final class GameSettings {
    private static final double DEFAULT_AP_PER_SECOND = 3.0;
    private static final double MIN_AP_PER_SECOND = 0.5;
    private static final double MAX_AP_PER_SECOND = 30.0;
    private double apPerSecond = DEFAULT_AP_PER_SECOND;

    /** Creates settings with the default action-point rate. */
    public GameSettings() { }

    /** Returns action-point charge rate per second.
     * @return action-point charge rate per second
     */
    public double getApPerSecond() { return apPerSecond; }

    /** Changes the charge rate for subsequent elapsed time.
     * @param rate AP per second, between 0.5 and 30
     */
    public void setApPerSecond(double rate) {
        if (!Double.isFinite(rate) || rate < MIN_AP_PER_SECOND || rate > MAX_AP_PER_SECOND) {
            throw new IllegalArgumentException("AP rate must be between 0.5 and 30 per second");
        }
        apPerSecond = rate;
    }
}
