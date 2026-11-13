import game.model.*;
import game.io.SaveStore;
import java.nio.file.*;
import java.util.*;

public final class LooperRingTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static final class Rolls extends Random {
        final double chance; final int percent; int doubles;
        Rolls(double chance, int percent) { this.chance=chance; this.percent=percent; }
        @Override public int nextInt(int bound) { return bound == 21 ? percent : 0; }
        @Override public double nextDouble() { return doubles++ == 0 ? 0 : chance; }
    }
    private static GameProgress equipped() {
        GameProgress p = new GameProgress(List.of(Student.BOGART));
        p.getInventory().addEquipment("looper_ring");
        check(p.equip(p.getParty().get(0), Equipment.Slot.ACCESSORY, "looper_ring"), "equip");
        p.advance(); // Keep the test target alive for all five hits.
        return p;
    }
    private static void proc(double chance, int percent) {
        GameProgress p=equipped(); Student s=p.getParty().get(0);
        Battle b=new Battle(p,new Rolls(chance,percent)); Enemy e=b.getEnemies().get(0);
        int before=e.getHp(); check(b.selectStudent(s) && b.attack(e), "attack"); int original=before-e.getHp();
        long count=b.getEffects().stream().filter(x -> x.getEnemy()!=null && !x.isManaLeak()).count();
        check(count==1 && b.hasPendingLooperHits()==(chance < .7), "proc boundary " + chance);
        if (chance < .7) {
            int previous=0;
            for(int i=0;i<5;i++) { int bonus=(int)Math.ceil(original * 0.5 + previous * 0.2 - 1e-9); check(b.advanceLooperHit(), "queued hit " + i); final int expected=bonus; check(b.getEffects().stream().anyMatch(x -> x.getAmount()==-expected), "loop damage " + i); previous=bonus; }
            check(!b.hasPendingLooperHits(), "sequence finishes after five hits");
        }
    }
    public static void main(String[] args) throws Exception {
        int index=java.util.stream.IntStream.range(0,CarbonMarket.offers().size()).filter(i -> CarbonMarket.offers().get(i).id().equals("looper_ring")).findFirst().orElseThrow();
        GameProgress p=new GameProgress(List.of(Student.BOGART)); p.addPetot(2500);
        check(CarbonMarket.buy(p,index).contains("Purchased") && p.getPetot()==0,"purchase");
        Student s=p.getParty().get(0); check(p.equip(s,Equipment.Slot.ACCESSORY,"looper_ring"),"equip purchased");
        check(s.getMaxSp()==35 && s.getSp()==35 && s.getDefense()==13 && s.getSpeed()==17 && s.getAbilitySpCost()==6,"bonuses");
        s.restoreSp(35); p.advance();
        Path path=Files.createTempDirectory("looper-test");
        try { SaveStore store=new SaveStore(path); for (SaveStore.Format f:SaveStore.Format.values()) {
            store.save(1,f,p); Student loaded=store.load(1,f).getParty().get(0);
            check(loaded.getSp()==35 && loaded.getMaxSp()==35 && "looper_ring".equals(loaded.getEquipmentId(Equipment.Slot.ACCESSORY)),"save " + f);
        }} finally {try(var paths=Files.walk(path)){for(Path item:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(item);}}
        check(p.equip(s,Equipment.Slot.ACCESSORY,null) && s.getMaxSp()==35 && s.getSp()==35,"unequip preserves SP");
        proc(0,0); proc(.699999,20); proc(.7,0);
        GameProgress abilityParty=equipped(); Student attacker=abilityParty.getParty().get(0);
        Battle ability=new Battle(abilityParty,new Rolls(0,0)); Enemy target=ability.getEnemies().get(0);
        check(ability.selectStudent(attacker) && ability.ability(null,target),"ability with ring");
        check(ability.getMessage().contains("Looper Ring activates!") && ability.hasPendingLooperHits(),"ring activates after damaging ability");
        for(int i=0;i<5 && ability.hasPendingLooperHits();i++) check(ability.advanceLooperHit(),"ability extra hit");
        GameProgress healerParty=new GameProgress(List.of(Student.ANGEL)); Student healer=healerParty.getParty().get(0);
        healerParty.getInventory().addEquipment("looper_ring"); check(healerParty.equip(healer,Equipment.Slot.ACCESSORY,"looper_ring"),"healer equips");
        Battle healing=new Battle(healerParty,new Rolls(0,0));
        check(healing.selectStudent(healer) && healing.ability(healer,null) && !healing.getMessage().contains("Looper Ring"),"healing is not an attack");
        System.out.println("Looper Ring checks passed");
    }
}
