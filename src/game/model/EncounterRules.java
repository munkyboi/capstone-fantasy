package game.model;

import java.util.Random;

/** Defines encounter sizes for the five planned route stages. */
public final class EncounterRules {
    private static final int[] MINIMUM_MONSTERS = {1, 2, 3, 4, 6};
    private static final int[] MAXIMUM_MONSTERS = {3, 4, 5, 6, 6};
    private EncounterRules() { }

    /** Rolls the number of monsters for a route stage.
     * @param stage zero-based route position
     * @param random random source
     * @return encounter size, including any boss
     */
    public static int rollMonsterCount(int stage, Random random) {
        if (stage < 0 || stage >= MINIMUM_MONSTERS.length) {
            throw new IllegalArgumentException("Unknown route stage");
        }
        int minimum = MINIMUM_MONSTERS[stage];
        int range = MAXIMUM_MONSTERS[stage] - minimum + 1;
        return range == 1 ? minimum : minimum + random.nextInt(range);
    }
}
