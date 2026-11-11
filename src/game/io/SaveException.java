package game.io;

/** Reports a save or load failure in a form suitable for the interface. */
public final class SaveException extends Exception {
    /** Creates an error with a helpful message and its cause.
     * @param message user-facing explanation
     * @param cause underlying error
     */
    public SaveException(String message, Throwable cause) { super(message, cause); }
}
