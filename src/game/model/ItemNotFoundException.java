package game.model;

/** Reports an unknown item identifier without exposing a stack trace to players. */
public final class ItemNotFoundException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /** Creates an error identifying the missing item.
     * @param id unrecognized identifier
     */
    public ItemNotFoundException(String id) { super("Unknown equipment: " + id); }
}
