package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** A typed collection of items held by the party.
 * @param <T> type of item held in the bag
 */
public final class ItemBag<T extends Item> {
    private final List<T> items = new ArrayList<>();

    /** Creates an empty item bag. */
    public ItemBag() { }

    /** Adds an item to the bag.
     * @param item item to add
     */
    public void add(T item) { items.add(Objects.requireNonNull(item)); }

    /** Removes the first matching item, if any.
     * @param condition item selector
     * @return true when an item was removed
     */
    public boolean removeFirst(Predicate<? super T> condition) {
        Objects.requireNonNull(condition);
        for (int i = 0; i < items.size(); i++) {
            if (condition.test(items.get(i))) { items.remove(i); return true; }
        }
        return false;
    }

    /** Returns an immutable snapshot of the items.
     * @return carried items
     */
    public List<T> items() { return List.copyOf(items); }
}
