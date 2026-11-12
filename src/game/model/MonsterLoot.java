package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Loot tables for defeated monsters. Each entry is rolled independently on victory. */
public final class MonsterLoot {
    /** One possible item and its probability for each defeated monster of a type. */
    public record Drop(String itemId, double chance) { }
    private static final Map<String, List<Drop>> TABLES = Map.of(
            "wisp", List.of(new Drop("first_aid_kit", 0.30), new Drop("white_flower", 0.05)),
            "sentinel", List.of(new Drop("sentinel_band", 0.25), new Drop("white_flower", 0.05)));
    private MonsterLoot() { }
    /** Returns the immutable drop table for an enemy. */
    public static List<Drop> tableFor(Enemy enemy) {
        return enemy.isLootGoblin() ? List.of() : TABLES.getOrDefault(enemy.isSentinel() ? "sentinel" : "wisp", List.of());
    }
    /** Awards rolled drops only when inventory limits and uniqueness permit them. */
    public static List<String> award(List<Enemy> enemies, GameProgress progress, Random random) {
        List<String> drops = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (!enemy.isDefeated()) { continue; }
            if (enemy.isLootGoblin()) { awardGoblinLoot(progress, random, drops); continue; }
            for (Drop drop : tableFor(enemy)) {
                if (random.nextDouble() >= drop.chance()) { continue; }
                if ("first_aid_kit".equals(drop.itemId())) {
                    if (progress.getInventory().addKit()) { drops.add(drop.itemId()); }
                } else if ("white_flower".equals(drop.itemId())) {
                    if (progress.getInventory().addWhiteFlower()) drops.add(drop.itemId());
                } else if (!owns(progress, drop.itemId())) {
                    progress.getInventory().addEquipment(drop.itemId()); drops.add(drop.itemId());
                }
            }
        }
        return List.copyOf(drops);
    }
    private static void awardGoblinLoot(GameProgress progress, Random random, List<String> drops) {
        if (random.nextDouble() >= 0.20) return;
        int amount = random.nextDouble() < 0.05 ? 2 : 1;
        java.util.List<String> available = new ArrayList<>();
        for (CarbonMarket.Offer offer : CarbonMarket.offers()) {
            if ("first_aid_kit".equals(offer.id())) {
                if (progress.getKits() < Inventory.MAX_KITS) available.add(offer.id());
            } else if (!owns(progress, offer.id())) available.add(offer.id());
        }
        for (int i = 0; i < amount && !available.isEmpty(); i++) {
            String id = available.remove(random.nextInt(available.size()));
            if ("first_aid_kit".equals(id)) progress.getInventory().addKit();
            else progress.getInventory().addEquipment(id);
            drops.add(id);
        }
    }
    private static boolean owns(GameProgress progress, String id) {
        return progress.getInventory().getEquipmentIds().contains(id)
                || progress.getRoster().stream().anyMatch(s -> java.util.Arrays.stream(Equipment.Slot.values())
                        .anyMatch(slot -> id.equals(s.getEquipmentId(slot))));
    }
}
