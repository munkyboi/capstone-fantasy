package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Loot tables for defeated monsters. Each entry is rolled independently on victory. */
public final class MonsterLoot {
    /** One possible item and its probability for each defeated monster of a type.
     * @param itemId stable item identifier
     * @param chance base drop probability
     */
    public record Drop(String itemId, double chance) { }
    private static final List<String> NON_SHOP_GEAR = List.of(
            "student_staff", "cadet_sword", "hand_crossbows", "monk_wraps",
            "scholar_staff", "cadet_blade", "hunter_crossbows", "champion_wraps",
            "campus_vest", "focus_charm", "training_uniform", "sentinel_band",
            "guardian_band", "courage_charm", "campus_medal");
    private static List<Drop> table() {
        List<Drop> drops = new ArrayList<>();
        drops.add(new Drop("white_flower", 0.15));
        for (String id : NON_SHOP_GEAR) drops.add(new Drop(id, 0.15));
        return List.copyOf(drops);
    }
    private static final Map<String, List<Drop>> TABLES = Map.of("wisp", table(), "sentinel", table());
    private MonsterLoot() { }
    /** Returns the immutable drop table for an enemy.
     * @param enemy enemy involved in the action
     * @return the immutable drop table for an enemy
     */
    public static List<Drop> tableFor(Enemy enemy) {
        return enemy.isLootGoblin() ? List.of() : TABLES.getOrDefault(enemy.isSentinel() ? "sentinel" : "wisp", List.of());
    }
    /** Awards rolled drops only when inventory limits and uniqueness permit them.
     * @param enemies the enemies
     * @param progress current game progress
     * @param random random source
     * @return the result of award
     */
    public static List<String> award(List<Enemy> enemies, GameProgress progress, Random random) {
        return award(enemies, progress, random, GameSettings.Difficulty.NORMAL);
    }
    /** Rolls defeated-enemy loot using the current difficulty's reward multiplier.
     * @param enemies the enemies
     * @param progress current game progress
     * @param random random source
     * @param difficulty selected difficulty
     * @return the result of award
     */
    public static List<String> award(List<Enemy> enemies, GameProgress progress, Random random, GameSettings.Difficulty difficulty) {
        double multiplier = java.util.Objects.requireNonNull(difficulty).multiplier();
        List<String> drops = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (!enemy.isDefeated()) { continue; }
            if (enemy.isLootGoblin()) { awardGoblinLoot(progress, random, drops, multiplier); continue; }
            for (Drop drop : tableFor(enemy)) {
                if (random.nextDouble() >= Math.min(1.0, drop.chance() * multiplier)) { continue; }
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
    private static void awardGoblinLoot(GameProgress progress, Random random, List<String> drops, double multiplier) {
        if (random.nextDouble() >= Math.min(1.0, 0.50 * multiplier)) return;
        int amount = random.nextDouble() < Math.min(1.0, 0.25 * multiplier) ? 2 : 1;
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
    static boolean owns(GameProgress progress, String id) {
        return progress.getInventory().getEquipmentIds().contains(id)
                || progress.getRoster().stream().anyMatch(s -> java.util.Arrays.stream(Equipment.Slot.values())
                        .anyMatch(slot -> id.equals(s.getEquipmentId(slot))));
    }
}
