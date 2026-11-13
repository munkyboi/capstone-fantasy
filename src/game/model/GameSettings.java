package game.model;

/** Persistent gameplay preferences shared by the settings screen and battles. */
public final class GameSettings {
    /** Action-point generation presets. */
    public enum BattleSpeed {
        /** Five action points per second. */ SLOW,
        /** Ten action points per second. */ NORMAL,
        /** Fifteen action points per second. */ FAST,
        /** Twenty action points per second. */ ULTRA
    }
    /** Multipliers applied to earned experience. */
    public enum XpBoost {
        /** Normal experience. */ NORMAL(1),
        /** Double experience. */ HYPER(2),
        /** Five times the normal experience. */ ULTRA(5);
        private final int multiplier;
        XpBoost(int multiplier) { this.multiplier = multiplier; }
        /** Returns the experience multiplier.
         * @return multiplier applied to XP
         */
        public int multiplier() { return multiplier; }
    }
    /** Difficulty presets and their combat and reward multipliers. */
    public enum Difficulty {
        /** Reduced difficulty. */ EASY(0.75),
        /** Default difficulty. */ NORMAL(1.0),
        /** Increased difficulty. */ HARD(1.5),
        /** Highest difficulty. */ INSANE(2.0);
        private final double multiplier;
        Difficulty(double multiplier) { this.multiplier = multiplier; }
        /** Returns the difficulty multiplier.
         * @return combat and reward multiplier
         */
        public double multiplier() { return multiplier; }
    }
    private static final double AMBUSH_CHANCE = 0.50;
    private static final double DEFAULT_AP_PER_SECOND = 10.0;
    private static final double MIN_AP_PER_SECOND = 0.5;
    private static final double MAX_AP_PER_SECOND = 30.0;
    private double apPerSecond = DEFAULT_AP_PER_SECOND;
    private BattleSpeed battleSpeed = BattleSpeed.NORMAL;
    private int levelCap = 30;
    private int partySize = 3;
    private XpBoost xpBoost = XpBoost.NORMAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean ambushEnabled = true;
    private boolean greedIsGood;

    /** Creates settings with the default action-point rate. */
    public GameSettings() { }
    /** Returns the selected action-bar speed.
     * @return selected speed
     */
    public BattleSpeed getBattleSpeed() { return battleSpeed; }
    /** Returns the XP growth percentage for the selected difficulty.
     * @return XP growth percentage
     */
    public int getGrowthFactor() { return switch (difficulty) { case EASY -> 1; case NORMAL -> 7; case HARD -> 15; case INSANE -> 30; }; }
    /** Returns the maximum level for new progression.
     * @return maximum level
     */
    public int getLevelCap() { return levelCap; }
    /** Returns the configured active party size.
     * @return active party size
     */
    public int getPartySize() { return partySize; }
    /** Returns the selected XP multiplier preset.
     * @return XP multiplier preset
     */
    public XpBoost getXpBoost() { return xpBoost; }
    /** Returns the selected combat difficulty.
     * @return combat difficulty
     */
    public Difficulty getDifficulty() { return difficulty; }
    /** Returns whether stage ambushes are enabled.
     * @return true if ambushes are enabled
     */
    public boolean isAmbushEnabled() { return ambushEnabled; }
    /** Returns whether the Petot multiplier is enabled.
     * @return true if the Petot multiplier is enabled
     */
    public boolean isGreedIsGood() { return greedIsGood; }
    /** Enables or disables the Petot multiplier.
     * @param enabled whether the Petot multiplier is enabled
     */
    public void setGreedIsGood(boolean enabled) { greedIsGood = enabled; }
    /** Enables or disables stage ambushes.
     * @param enabled whether ambushes are enabled
     */
    public void setAmbushEnabled(boolean enabled) { ambushEnabled = enabled; }
    /** Changes the XP multiplier preset.
     * @param value XP boost preset
     */
    public void setXpBoost(XpBoost value) { xpBoost = java.util.Objects.requireNonNull(value); }
    /** Changes the combat difficulty.
     * @param value combat difficulty
     */
    public void setDifficulty(Difficulty value) { difficulty = java.util.Objects.requireNonNull(value); }
    /** Restores all user-facing options to their default values. */
    public void resetDefaults() {
        setBattleSpeed(BattleSpeed.NORMAL); levelCap = 30; partySize = 3;
        xpBoost = XpBoost.NORMAL; difficulty = Difficulty.NORMAL; ambushEnabled = true; greedIsGood = false;
    }
    /** Adjusts the elapsed-time AP rate without changing the rendering frame rate.
     * @param speed action-bar speed preset
     */
    public void setBattleSpeed(BattleSpeed speed) {
        battleSpeed = java.util.Objects.requireNonNull(speed);
        apPerSecond = switch (speed) { case SLOW -> 5.0; case NORMAL -> 10.0; case FAST -> 15.0; case ULTRA -> 20.0; };
    }
    /** Changes the maximum level in increments of ten.
     * @param value maximum level, from 20 to 100 in steps of ten
     */
    public void setLevelCap(int value) { if (value < 20 || value > 100 || value % 10 != 0) throw new IllegalArgumentException("Invalid level cap"); levelCap = value; }
    /** Changes the number of active heroes.
     * @param value number of active heroes, from one to four
     */
    public void setPartySize(int value) { if (value < 1 || value > 4) throw new IllegalArgumentException("Invalid party size"); partySize = value; }
    /** Load saved preferences; invalid or missing settings fall back to defaults.
     * @param file file path
     * @return the result of load
     */
    public static GameSettings load(java.nio.file.Path file) {
        GameSettings settings = new GameSettings();
        if (!java.nio.file.Files.isRegularFile(file)) return settings;
        try (var reader = java.nio.file.Files.newBufferedReader(file)) {
            var props = new java.util.Properties(); props.load(reader);
            settings.setBattleSpeed(BattleSpeed.valueOf(props.getProperty("battleSpeed", "NORMAL")));
            int savedCap = Integer.parseInt(props.getProperty("levelCap", "30"));
            settings.setLevelCap(savedCap);
            settings.setPartySize(Integer.parseInt(props.getProperty("partySize", "3")));
            settings.setXpBoost(XpBoost.valueOf(props.getProperty("xpBoost", "NORMAL")));
            settings.setDifficulty(Difficulty.valueOf(props.getProperty("difficulty", "NORMAL")));
            String ambush = props.getProperty("ambushEnabled", "true");
            if (!ambush.equals("true") && !ambush.equals("false")) throw new IllegalArgumentException("Invalid ambush setting");
            settings.setAmbushEnabled(Boolean.parseBoolean(ambush));
            String greed = props.getProperty("greedIsGood", "false");
            if (!greed.equals("true") && !greed.equals("false")) throw new IllegalArgumentException("Invalid greed setting");
            settings.setGreedIsGood(Boolean.parseBoolean(greed));
            return settings;
        } catch (Exception invalid) { return new GameSettings(); }
    }
    /** Persists preferences separately from game saves.
     * @param file file path
     * @throws java.io.IOException if the settings cannot be written
     */
    public void save(java.nio.file.Path file) throws java.io.IOException {
        var parent = file.toAbsolutePath().getParent(); java.nio.file.Files.createDirectories(parent);
        var props = new java.util.Properties(); props.setProperty("battleSpeed", battleSpeed.name());
        props.setProperty("levelCap", Integer.toString(levelCap));
        props.setProperty("partySize", Integer.toString(partySize));
        props.setProperty("xpBoost", xpBoost.name()); props.setProperty("difficulty", difficulty.name());
        props.setProperty("ambushEnabled", Boolean.toString(ambushEnabled));
        props.setProperty("greedIsGood", Boolean.toString(greedIsGood));
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
