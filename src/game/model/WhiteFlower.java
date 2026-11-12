package game.model;

/** A rare consumable that revives a fallen student. */
public final class WhiteFlower extends Item {
    private static final long serialVersionUID = 1L;
    public WhiteFlower() { super("White Flower"); }
    @Override public String getCategory() { return "Consumable"; }
}
