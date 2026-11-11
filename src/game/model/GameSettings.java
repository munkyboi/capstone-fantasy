package game.model;

/** Persistent gameplay preferences shared by the settings screen and battles. */
public final class GameSettings {
    /** AP generation presets. */
    public enum BattleSpeed { SLOW, NORMAL, FAST, ULTRA }
    public enum XpBoost { NORMAL(1), HYPER(2), ULTRA(5); private final int multiplier; XpBoost(int multiplier) { this.multiplier = multiplier; } public int multiplier() { return multiplier; } }
    public enum Difficulty { EASY(0.75), NORMAL(1.0), HARD(1.5), INSANE(2.0); private final double multiplier; Difficulty(double multiplier) { this.multiplier = multiplier; } public double multiplier() { return multiplier; } }
    private static final double AMBUSH_CHANCE = 0.50;
    private static final double DEFAULT_AP_PER_SECOND = 10.0;
    private static final double MIN_AP_PER_SECOND = 0.5;
    private static final double MAX_AP_PER_SECOND = 30.0;
    private double apPerSecond = DEFAULT_AP_PER_SECOND;
    private BattleSpeed battleSpeed = BattleSpeed.NORMAL;
    private int growthFactor = 7;
    private int levelCap = 30;
    private int partySize = 3;
    private XpBoost xpBoost = XpBoost.NORMAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean ambushEnabled = true;

    /** Creates settings with the default action-point rate. */
    public GameSettings() { }
    public BattleSpeed getBattleSpeed() { return battleSpeed; }
    public int getGrowthFactor() { return growthFactor; }
    public int getLevelCap() { return levelCap; }
    public int getPartySize() { return partySize; }
    public XpBoost getXpBoost() { return xpBoost; }
    public Difficulty getDifficulty() { return difficulty; }
    public boolean isAmbushEnabled() { return ambushEnabled; }
    public void setAmbushEnabled(boolean enabled) { ambushEnabled = enabled; }
    public void setXpBoost(XpBoost value) { xpBoost = java.util.Objects.requireNonNull(value); }
    public void setDifficulty(Difficulty value) { difficulty = java.util.Objects.requireNonNull(value); }
    /** Restores all user-facing options to their default values. */
    public void resetDefaults() {
        setBattleSpeed(BattleSpeed.NORMAL); growthFactor = 7; levelCap = 30; partySize = 3;
        xpBoost = XpBoost.NORMAL; difficulty = Difficulty.NORMAL; ambushEnabled = true;
    }
    /** Adjusts the elapsed-time AP rate without changing the rendering frame rate. */
    public void setBattleSpeed(BattleSpeed speed) {
        battleSpeed = java.util.Objects.requireNonNull(speed);
        apPerSecond = switch (speed) { case SLOW -> 5.0; case NORMAL -> 10.0; case FAST -> 15.0; case ULTRA -> 20.0; };
    }
    public void setGrowthFactor(int value) { if (value < 1 || value > 20) throw new IllegalArgumentException("Invalid growth factor"); growthFactor = value; }
    public void setLevelCap(int value) { if (value < 20 || value > 100 || value % 10 != 0) throw new IllegalArgumentException("Invalid level cap"); levelCap = value; }
    public void setPartySize(int value) { if (value < 1 || value > 4) throw new IllegalArgumentException("Invalid party size"); partySize = value; }
    /** Load saved preferences; invalid or missing settings fall back to defaults. */
    public static GameSettings load(java.nio.file.Path file) {
        GameSettings settings = new GameSettings();
        if (!java.nio.file.Files.isRegularFile(file)) return settings;
        try (var reader = java.nio.file.Files.newBufferedReader(file)) {
            var props = new java.util.Properties(); props.load(reader);
            settings.setBattleSpeed(BattleSpeed.valueOf(props.getProperty("battleSpeed", "NORMAL")));
            settings.setGrowthFactor(Integer.parseInt(props.getProperty("growthFactor", "7")));
            int savedCap = Integer.parseInt(props.getProperty("levelCap", "30"));
            settings.setLevelCap(savedCap);
            settings.setPartySize(Integer.parseInt(props.getProperty("partySize", "3")));
            settings.setXpBoost(XpBoost.valueOf(props.getProperty("xpBoost", "NORMAL")));
            settings.setDifficulty(Difficulty.valueOf(props.getProperty("difficulty", "NORMAL")));
            String ambush = props.getProperty("ambushEnabled", "true");
            if (!ambush.equals("true") && !ambush.equals("false")) throw new IllegalArgumentException("Invalid ambush setting");
            settings.setAmbushEnabled(Boolean.parseBoolean(ambush));
            return settings;
        } catch (Exception invalid) { return new GameSettings(); }
    }
    /** Persists preferences separately from game saves. */
    public void save(java.nio.file.Path file) throws java.io.IOException {
        var parent = file.toAbsolutePath().getParent(); java.nio.file.Files.createDirectories(parent);
        var props = new java.util.Properties(); props.setProperty("battleSpeed", battleSpeed.name());
        props.setProperty("growthFactor", Integer.toString(growthFactor)); props.setProperty("levelCap", Integer.toString(levelCap));
        props.setProperty("partySize", Integer.toString(partySize));
        props.setProperty("xpBoost", xpBoost.name()); props.setProperty("difficulty", difficulty.name());
        props.setProperty("ambushEnabled", Boolean.toString(ambushEnabled));
        var temp = java.nio.file.Files.createTempFile(parent, "settings-", ".tmp");
        try {
            try (var writer = java.nio.file.Files.newBufferedWriter(temp)) { props.store(writer, "Capstone Fantasy settings"); }
            try { java.nio.file.Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ex) { java.nio.file.Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } finally { java.nio.file.Files.deleteIfExists(temp); }
    }

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
    public boolean allowsAmbush(int encounter) { return ambushEnabled && encounter != GameProgress.FINAL_BOSS_ENCOUNTER; }

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
