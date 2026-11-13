package game.model;

import java.util.Random;

/** Defines encounter sizes for the five planned route stages. */
public final class EncounterRules {
    private static final int[] MINIMUM_MONSTERS = {1, 2, 3, 4, 6};
    private static final int[] MAXIMUM_MONSTERS = {2, 3, 4, 6, 6};
    private EncounterRules() { }

    /** Rolls the number of monsters for a route stage.
     * @param stage zero-based route position
     * @param random random source
     * @return encounter size, including any boss
     */
    public static int rollMonsterCount(int stage, Random random) {
        return rollMonsterCount(stage, random, GameSettings.Difficulty.NORMAL);
    }
    /** Biases encounter sizes within their existing stage limits. */
    public static int rollMonsterCount(int stage, Random random, GameSettings.Difficulty difficulty) {
        if (stage < 0 || stage >= MINIMUM_MONSTERS.length) {
            throw new IllegalArgumentException("Unknown route stage");
        }
        int minimum = MINIMUM_MONSTERS[stage];
        int range = MAXIMUM_MONSTERS[stage] - minimum + 1;
        if (range == 1) return minimum;
        int offset = random.nextInt(range);
        switch (java.util.Objects.requireNonNull(difficulty)) {
            case EASY -> offset = Math.min(offset, random.nextInt(range));
            case NORMAL -> { }
            case HARD -> offset = Math.max(offset, random.nextInt(range));
            case INSANE -> { offset = Math.max(offset, random.nextInt(range)); offset = Math.max(offset, random.nextInt(range)); }
        }
        return minimum + offset;
    }
}
