package game.model;

/** Shared shop catalog and purchase rules. */
public final class CarbonMarket {
    /** An item offered at the market. */
    public record Offer(String id, String name, int price, String description) { }
    private static final java.util.List<Offer> OFFERS = java.util.List.of(
            new Offer("first_aid_kit", "First-Aid Kit", 20, "Restores HP to one student."),
            new Offer("market_charm", "Market Charm", 60, "+1 ATK/DEF; +10% Petot after victory while wearer is alive."),
            new Offer("ring_of_power", "Ring of Power", 2500, "+50% ATK/DEF/SPD; 50% chance to refill AP when hit."),
            new Offer("light_saber", "Light Saber", 2500, "Warrior: +100 ATK, +20% SPD; 100% hit damage over 5 turns."),
            new Offer("demon_hunter", "Demon Hunter", 2500, "Hunter: +20% ATK, +75% SPD; Piercing Bolts ignore 25% of positive DEF."),
            new Offer("great_sage_staff", "Great Sage Staff", 2500, "Mage: +20% ATK; 50% chance to cast twice."),
            new Offer("giant_slayer_gloves", "Giant Slayer Gloves", 2500, "Monk: +50% ATK, +15 SPD, two extra hits."),
            new Offer("blade_mail", "Blade Mail", 2500, "0 DEF; +250% HP; +20% regen; -20% SPD; reflect 50% x (1 + regen rate)."),
            new Offer("ultra_instinct_shirt", "Ultra Instinct Shirt", 2500, "-20% DEF, +20% SPD; 50% dodge; +30% regen."),
            new Offer("designer_leather_vest", "Designer Leather Vest", 2500, "+10% DEF, +20% SPD, +50 HP; 2x XP; +10% regen."),
            new Offer("ethereal_robe", "Ethereal Robe", 2500, "+15% DEF, +100 HP; +10% regen; damage empowers next attack."),
            new Offer("heroes_ring", "Hero’s Ring", 2500, "+20% HP/DEF/regen; -40% SPD; 50% auto defend; no Defend/Escape."),
            new Offer("looper_ring", "Looper Ring", 2500, "+10% ability cost; -20% DEF; +10% SPD; hits leak 5–10% max SP; up to 5 damage loops, 70% chance each."),
            new Offer("scholars_ward", "Scholar’s Ward", 2500, "Zack only: +30% HP, +20% DEF; 25% chance to restore 10% max SP when AP fills."));
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
