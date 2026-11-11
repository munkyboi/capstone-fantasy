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
            "wisp", List.of(new Drop("first_aid_kit", 0.30)),
            "sentinel", List.of(new Drop("sentinel_band", 0.25)));
    private MonsterLoot() { }
    /** Returns the immutable drop table for an enemy. */
    public static List<Drop> tableFor(Enemy enemy) {
        return TABLES.getOrDefault(enemy.isSentinel() ? "sentinel" : "wisp", List.of());
    }
    /** Awards rolled drops only when inventory limits and uniqueness permit them. */
    public static List<String> award(List<Enemy> enemies, GameProgress progress, Random random) {
        List<String> drops = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) { continue; }
            for (Drop drop : tableFor(enemy)) {
                if (random.nextDouble() >= drop.chance()) { continue; }
                if ("first_aid_kit".equals(drop.itemId())) {
                    if (progress.getInventory().addKit()) { drops.add(drop.itemId()); }
                } else if (!owns(progress, drop.itemId())) {
                    progress.getInventory().addEquipment(drop.itemId()); drops.add(drop.itemId());
                }
            }
        }
        return List.copyOf(drops);
    }
    private static boolean owns(GameProgress progress, String id) {
        return progress.getInventory().getEquipmentIds().contains(id)
                || progress.getRoster().stream().anyMatch(s -> java.util.Arrays.stream(Equipment.Slot.values())
                        .anyMatch(slot -> id.equals(s.getEquipmentId(slot))));
    }
}
