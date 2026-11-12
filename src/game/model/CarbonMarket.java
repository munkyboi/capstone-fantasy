package game.model;

/** Shared shop catalog and purchase rules. */
public final class CarbonMarket {
    /** An item offered at the market. */
    public record Offer(String id, String name, int price, String description) { }
    private static final java.util.List<Offer> OFFERS = java.util.List.of(
            new Offer("first_aid_kit", "First-Aid Kit", 20, "Restores HP to one student."),
            new Offer("market_charm", "Market Charm", 60, "+1 ATK and +1 DEF."),
            new Offer("ring_of_power", "Ring of Power", 2500, "+50% ATK/DEF/SPD; 50% chance to refill AP when hit."),
            new Offer("light_saber", "Light Saber", 2500, "Warrior: +100 ATK, +20% SPD; 100% hit damage over 5s."),
            new Offer("demon_hunter", "Demon Hunter", 2500, "Hunter: +20% ATK, +75% SPD."),
            new Offer("great_sage_staff", "Great Sage Staff", 2500, "Mage: +20% ATK; 50% chance to cast twice."),
            new Offer("giant_slayer_gloves", "Giant Slayer Gloves", 2500, "Monk: +50% ATK, +15 SPD, extra hit."),
            new Offer("blade_mail", "Blade Mail", 2500, "-50% DEF, -20% SPD, +200% HP; 50% reflect; +50% regen."),
            new Offer("ultra_instinct_shirt", "Ultra Instinct Shirt", 2500, "-20% DEF, +20% SPD; 50% dodge; +30% regen."),
            new Offer("designer_leather_vest", "Designer Leather Vest", 2500, "+10% DEF, +20% SPD, +50 HP; 2x XP; +10% regen."),
            new Offer("ethereal_robe", "Ethereal Robe", 2500, "+15% DEF, +100 HP; +10% regen; damage empowers next attack."));
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
                    || progress.getRoster().stream().anyMatch(student -> java.util.Arrays.stream(Equipment.Slot.values())
                            .anyMatch(slot -> offer.id().equals(student.getEquipmentId(slot))));
            if (owned) { return "The party already owns this item."; }
            progress.spendPetot(offer.price()); progress.getInventory().addEquipment(offer.id());
        }
        return "Purchased " + offer.name() + ".";
    }
}
