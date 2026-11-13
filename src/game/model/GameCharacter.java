package game.model;

/** Common battle participant. */
public abstract class GameCharacter implements java.io.Serializable, Combatant, Targetable {
    private static final long serialVersionUID = 1L;
    /** Initializes a combat participant. */
    public GameCharacter() { }
    /** Returns the participant's name.
     * @return display name
     */
    public abstract String getName();
    /** Returns the participant's attack.
     * @return attack power
     */
    public abstract int getAttack();
    /** Returns agility used to evade attacks.
     * @return agility used to evade attacks
     */
    public abstract int getAgility();
    /** Returns accuracy used to land attacks.
     * @return accuracy used to land attacks
     */
    public abstract int getAccuracy();
    /** Returns agility after applicable combat modifiers.
     * @return agility after applicable combat modifiers
     */
    public double getEffectiveAgility() { return getAgility(); }
    /** Returns accuracy after applicable combat modifiers.
     * @return accuracy after applicable combat modifiers
     */
    public double getEffectiveAccuracy() { return getAccuracy(); }
    /** Returns whether the participant can fight.
     * @return true when HP remains
     */
    public abstract boolean isAlive();
}
