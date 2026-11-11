package game.model;

/** Game-wide settings; a settings screen can edit these in a future update. */
public final class GameSettings {
    private static final double AMBUSH_CHANCE = 0.50;
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

    /** Returns the chance of an ambush when starting a stage battle.
     * @return probability from zero to one
     */
    public double getAmbushChance() { return AMBUSH_CHANCE; }

    /** Checks whether a route encounter may start as an ambush.
     * @param encounter zero-based route position
     * @return false for the final boss encounter
     */
    public boolean allowsAmbush(int encounter) { return encounter != GameProgress.FINAL_BOSS_ENCOUNTER; }

    /** Rolls whether an eligible stage begins with an ambush.
     * @param encounter zero-based route position
     * @param random random number source
     * @return true when the stage is an ambush
     */
    public boolean rollAmbush(int encounter, java.util.Random random) {
        return allowsAmbush(encounter) && random.nextDouble() < AMBUSH_CHANCE;
    }

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
