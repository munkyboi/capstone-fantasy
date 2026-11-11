package game.model;

/** A participant that can take damage. */
public interface Targetable {
    /** Returns whether the target remains active.
     * @return true when HP remains
     */
    boolean isAlive();
    /** Applies damage.
     * @param amount incoming damage
     * @return actual HP lost
     */
    int takeDamage(int amount);
}
