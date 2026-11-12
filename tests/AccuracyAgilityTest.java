import game.model.*;
import game.io.SaveStore;
import java.util.*;
import java.nio.file.*;

/** Exercises the specified hit formula, per-hit rolls, progression and saves. */
public class AccuracyAgilityTest {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Random rolls(double roll) { return new Random() {
        public int nextInt(int bound) { return 0; }
        public double nextDouble() { return roll; }
    }; }
    public static void main(String[] args) throws Exception {
        int[][] stats = {{60,180,24,4,17,7,8},{150,35,16,16,15,4,9},
                {90,60,11,5,19,13,12},{125,30,25,10,5,10,10}};
        for (int id=0; id<4; id++) {
            Student s = new Student(id); int[] v=stats[id];
            check(s.getMaxHp()==v[0] && s.getMaxSp()==v[1] && s.getAttack()==v[2] && s.getDefense()==v[3]
                    && s.getSpeed()==v[4] && s.getAgility()==v[5] && s.getAccuracy()==v[6], "starting stats " + id);
            s.gainExperience(100); check(s.getAgility()==v[5]+1 && s.getAccuracy()==v[6]+1, "level stats " + id);
        }
        double dodge = 7.0 / (7.0 + 8.0 / 100.0);
        double expected = (8.0 - dodge) / 8.0;
        check(Math.abs(Battle.hitChance(7,8,100)-expected)<1e-12, "exact supplied formula");
        check(Battle.hitChance(7,8,200) < Battle.hitChance(7,8,75), "difficulty scales factor");
        Battle miss = new Battle(List.of(Student.ZACK), rolls(0.999999)); Student z=miss.getParty().get(0); Enemy e=miss.getEnemies().get(0);
        int hp=e.getHp(); check(miss.selectStudent(z) && miss.attack(e) && e.getHp()==hp, "student misses");
        check(miss.getEffects().stream().anyMatch(f -> f.getEnemy()==e && "Dodge".equals(f.getLabel())), "enemy dodge effect");
        int zh=z.getHp(); check(miss.enemyAttack(e) && z.getHp()==zh, "enemy misses");
        check(miss.getEffects().stream().anyMatch(f -> f.getStudent()==z && "Dodge".equals(f.getLabel())), "student dodge effect");
        Battle hit = new Battle(List.of(Student.ZACK), rolls(0)); Student a=hit.getParty().get(0); Enemy target=hit.getEnemies().get(0);
        check(hit.selectStudent(a) && hit.attack(target) && target.getHp()<target.getMaxHp(), "successful hit");
        Random alternating = new Random() {
            int rolls;
            public int nextInt(int bound) { return 0; }
            public double nextDouble() { return rolls++ % 2 == 0 ? 0 : 0.999999; }
        };
        Battle multi = new Battle(List.of(Student.ANGEL), alternating); Student angel = multi.getParty().get(0);
        Enemy multiTarget = multi.getEnemies().get(0);
        check(multi.selectStudent(angel) && multi.attack(multiTarget), "multi-hit attack");
        check(multi.getEffects().size() == 2 && multi.getEffects().get(0).getAmount() < 0
                && "Dodge".equals(multi.getEffects().get(1).getLabel()), "each hit rolls independently");
        Path dir=Files.createTempDirectory("accuracy-saves-");
        try { GameProgress p=new GameProgress(List.of(0,1,2)); p.advance(); p.getParty().forEach(s -> s.gainExperience(300));
            SaveStore store=new SaveStore(dir); for (SaveStore.Format f: SaveStore.Format.values()) { store.save(1,f,p);
                for (Student loaded:store.load(1,f).getParty()) { Student source=p.getParty().stream().filter(s -> s.getId()==loaded.getId()).findFirst().orElseThrow();
                    check(source.getAgility()==loaded.getAgility() && source.getAccuracy()==loaded.getAccuracy(), "save " + f); } }
        } finally { try(var paths=Files.walk(dir)) { for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
        System.out.println("Accuracy and agility checks passed");
    }
}
