package game.model;

/** Shared shop catalog and purchase rules. */
public final class CarbonMarket {
    /** An item offered at the market. */
    public record Offer(String id, String name, int price, String description) { }
    private static final java.util.List<Offer> OFFERS = java.util.List.of(
            new Offer("first_aid_kit", "First-Aid Kit", 20, "Restores HP to one student."),
            new Offer("market_charm", "Market Charm", 60, "+1 ATK and +1 DEF. One per party."));
    private CarbonMarket() { }
    /** Returns the items stocked at Carbon Market. */
    public static java.util.List<Offer> offers() { return OFFERS; }
    /** Purchases an offer, preserving balance if the purchase cannot be completed. */
    public static String buy(GameProgress progress, int index) {
        if (index < 0 || index >= OFFERS.size()) { throw new IllegalArgumentException("Unknown offer"); }
        Offer offer = OFFERS.get(index);
        if (progress.getPetot() < offer.price()) { return "Not enough Petot."; }
        if ("first_aid_kit".equals(offer.id())) {
            if (progress.getKits() >= Inventory.MAX_KITS) { return "Your kit bag is full."; }
            progress.spendPetot(offer.price()); progress.getInventory().addKit();
        } else {
            boolean owned = progress.getInventory().getEquipmentIds().contains(offer.id())
                    || progress.getParty().stream().anyMatch(student -> java.util.Arrays.stream(Equipment.Slot.values())
                            .anyMatch(slot -> offer.id().equals(student.getEquipmentId(slot))));
            if (owned) { return "The party already owns this item."; }
            progress.spendPetot(offer.price()); progress.getInventory().addEquipment(offer.id());
        }
        return "Purchased " + offer.name() + ".";
    }
}
