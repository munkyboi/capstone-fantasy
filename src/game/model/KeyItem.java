package game.model;

/** A keepsake that cannot be consumed or equipped. */
public final class KeyItem extends Item {
    private static final long serialVersionUID = 1L;
    /** Creates the campus pass. */
    public KeyItem() { super("Campus Pass"); }
    /** {@inheritDoc} */    @Override public String getCategory() { return "Key item"; }
}
