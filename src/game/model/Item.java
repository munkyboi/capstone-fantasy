package game.model;

/** A named item carried by the party. */
public abstract class Item implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /** Serialized state: name.
     * @serial
     */
    private final String name;
    /** Creates an item.
     * @param name display name
     */
    protected Item(String name) { this.name = name; }
    /** Returns the item name.
     * @return display name
     */
    public String getName() { return name; }
    /** Describes the item category.
     * @return category label
     */
    public abstract String getCategory();
}
