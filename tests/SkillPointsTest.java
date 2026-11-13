import game.io.SaveStore;
import game.model.*;
import java.nio.file.*;
import java.util.*;

/** Checks character stats, ability costs, validation, and SP save migration. */
public final class SkillPointsTest {
    private static void check(boolean ok, String message) { if (!ok) { throw new AssertionError(message); } }
    public static void main(String[] args) throws Exception {
        int[] hp = {60,150,90,125}, atk = {24,16,11,25}, def = {4,16,5,10}, speed = {17,15,19,5};
        int[] sp = {180,35,60,30}, cost = {15,5,5,10};
        for (int id = 0; id < 4; id++) {
            Student s = new Student(id);
            check(s.getMaxHp() == hp[id] && s.getBaseAttack() == atk[id] && s.getBaseDefense() == def[id]
                    && s.getSpeed() == speed[id] && s.getSp() == sp[id] && s.getAbilitySpCost() == cost[id], "configured stats " + id);
            GameProgress p = new GameProgress(List.of(id, (id + 1) % 4, (id + 2) % 4));
            Battle b = new Battle(p, new Random(3));
            Student actor = b.getParty().stream().filter(x -> x.getId() == s.getId()).findFirst().orElseThrow();
            Enemy enemy = b.getEnemies().get(0); check(b.selectStudent(actor), "select actor");
            check(!b.ability(null, null) || id == Student.ZACK, "invalid targets cannot spend SP");
            if (id == Student.ZACK) { check(actor.getSp() == sp[id] - cost[id], "all-target cost"); continue; }
            check(actor.getSp() == sp[id], "invalid target preserves SP");
            actor.restoreSp(cost[id] - 1);
            check(!b.ability(actor, enemy) && actor.getSp() == cost[id] - 1 && b.getCurrent() == actor,
                    "insufficient SP leaves turn and points intact");
            actor.restoreSp(cost[id]);
            check(b.ability(actor, enemy) && actor.getSp() == 0, "ability consumes exact SP cost");
        }
        Path dir = Files.createTempDirectory("capstone-sp-");
        try {
            SaveStore store = new SaveStore(dir);
            GameProgress p = new GameProgress(List.of(0, 1, 3)); p.advance();
            p.getParty().get(0).restoreSp(7);
            for (SaveStore.Format fmt : SaveStore.Format.values()) {
                store.save(1, fmt, p);
                check(store.load(1, fmt).getParty().get(0).getSp() == 7, "SP persists in " + fmt);
            }
            Path text = dir.resolve("slot-1.txt");
            String old = Files.readString(text).replace("version=9", "version=2").replaceAll("(?m)^student\\.[123]\\.sp=.*\\R", "");
            Files.writeString(text, old);
            check(store.load(1, SaveStore.Format.TEXT).getParty().stream().allMatch(s -> s.getSp() == s.getMaxSp()), "old save defaults to full SP");
            // A pre-SP binary has old stats and no initialized SP field.
            Student legacy = p.getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
            java.lang.reflect.Field attack = Student.class.getDeclaredField("attack"); attack.setAccessible(true); attack.setInt(legacy, 18);
            java.lang.reflect.Field speedField = Student.class.getDeclaredField("speed"); speedField.setAccessible(true); speedField.setInt(legacy, 10);
            java.lang.reflect.Field initialized = Student.class.getDeclaredField("skillPointsInitialized"); initialized.setAccessible(true); initialized.setBoolean(legacy, false);
            try (var out = new java.io.ObjectOutputStream(Files.newOutputStream(dir.resolve("slot-2.dat")))) { out.writeObject(p); }
            Student migrated = store.load(2, SaveStore.Format.BINARY).getParty().stream().filter(s -> s.getId() == Student.ZACK).findFirst().orElseThrow();
            check(migrated.getBaseAttack() == 24 && migrated.getSpeed() == 17 && migrated.getSp() == 180, "old binary stats and SP migrate");
        } finally {
            try (var files = Files.walk(dir)) { for (Path f : files.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(f); } }
        }
        check(MonsterLoot.tableFor(new Enemy("Wisp")).get(0).chance() == 0.30, "Wisp loot table");
        check(MonsterLoot.tableFor(new Enemy("Sentinel", true)).get(0).chance() == 0.25, "Sentinel loot table");
        System.out.println("Skill points and loot table checks passed");
    }
}
