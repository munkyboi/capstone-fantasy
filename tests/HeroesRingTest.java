import game.model.*;
import game.io.SaveStore;
import java.util.*;
import java.nio.file.*;

/** Hero's Ring bonuses, interception, combat boundaries and save/load. */
public final class HeroesRingTest {
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Student student(GameProgress p, int id) { return p.getParty().stream().filter(s -> s.getId()==id).findFirst().orElseThrow(); }
    static void equip(GameProgress p,int id,Equipment.Slot slot,String item) {p.getInventory().addEquipment(item);check(p.equip(student(p,id),slot,item),"equip " + item);}
    static final class Rolls extends Random {
        final double first; int rolls; final int targetIndex;
        Rolls(double first, int targetIndex) { this.first=first; this.targetIndex=targetIndex; }
        @Override public int nextInt(int bound) { return bound==3 ? targetIndex : 0; }
        @Override public double nextDouble() { return rolls++==0 ? first : 0.0; }
    }
    public static void main(String[] args) throws Exception {
        int index = java.util.stream.IntStream.range(0,CarbonMarket.offers().size()).filter(i -> CarbonMarket.offers().get(i).id().equals("heroes_ring")).findFirst().orElseThrow();
        check(CarbonMarket.offers().get(index).price()==2500,"price");
        GameProgress market=new GameProgress(List.of(0,1)); market.addPetot(5000);
        check(CarbonMarket.buy(market,index).contains("Purchased") && market.getPetot()==2500,"buy ring");
        check(CarbonMarket.buy(market,index).contains("already") && market.getPetot()==2500,"unique item");
        Student bearer=student(market,1); bearer.takeDamage(30);
        check(market.equip(bearer,Equipment.Slot.ACCESSORY,"heroes_ring"),"equip ring");
        check(bearer.getMaxHp()==180 && bearer.getHp()==144 && bearer.getDefense()==19 && bearer.getSpeed()==9,"bonuses and proportional HP");
        check(HealthRegeneration.amount(bearer)==50,"regen adds percentage points using boosted HP");
        check(bearer.getAttack()==16 && !bearer.isDefending(),"unrelated stats unchanged");
        GameProgress withArmor=new GameProgress(List.of(1)); equip(withArmor,1,Equipment.Slot.ARMOR,"blade_mail");equip(withArmor,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        Student tank=student(withArmor,1); check(tank.getMaxHp()==555 && tank.getDefense()==0 && tank.getSpeed()==6,"bonuses stack with armor");
        check(HealthRegeneration.amount(tank)==266,"regeneration bonuses stack");
        for(double roll : new double[] {0.0,0.499999,0.5,0.999999}) {
            GameProgress p=new GameProgress(List.of(0,1)); equip(p,1,Equipment.Slot.ACCESSORY,"heroes_ring");
            Battle battle=new Battle(p,new Rolls(roll,0)); Student ward=student(p,0), guard=student(p,1); Enemy w=battle.getEnemies().get(0);
            int wardHp=ward.getHp(), guardHp=guard.getHp(); check(battle.enemyAttack(w),"enemy action");
            if (roll < 0.5) {
                check(ward.getHp()==wardHp && guard.getHp()<guardHp,"ring intercepts below boundary " + roll);
                check(battle.getBlockingStudent()==guard && battle.getProtectedStudent()==ward && battle.getMessage().contains("protects"),"guard animation and log");
                check(!guard.isDefending() && battle.getGuardedAlly(guard)==null,"interception does not spend action or persist");
            } else check(ward.getHp()<wardHp && guard.getHp()==guardHp,"no intercept at boundary " + roll);
        }
        GameProgress restricted=new GameProgress(List.of(0,1));equip(restricted,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        Battle restrictionBattle=new Battle(restricted,new Random(0));Student wearer=student(restricted,1),ally=student(restricted,0);
        check(restrictionBattle.selectStudent(wearer) && !restrictionBattle.canUseDefendOrEscape() && !restrictionBattle.defend(ally)
                && !restrictionBattle.beginEscape() && restrictionBattle.getCurrent()==wearer,"ring blocks both actions without consuming the turn");
        GameProgress allowed=new GameProgress(List.of(0,1));equip(allowed,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        Battle allowedBattle=new Battle(allowed,new Random(0));Student protector=student(allowed,0),target=student(allowed,1);
        check(allowedBattle.selectStudent(protector) && allowedBattle.defend(target),"other heroes can defend wearer");
        GameProgress manual=new GameProgress(List.of(0,1,2)); equip(manual,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        Rolls manualRolls=new Rolls(0,1); Battle b=new Battle(manual,manualRolls); Student ward=student(manual,0), guardian=student(manual,2), ring=student(manual,1);
        check(b.selectStudent(guardian) && b.defend(ward),"manual defend"); int ringHp=ring.getHp(), guardHp=guardian.getHp();
        check(b.enemyAttack(b.getEnemies().get(0)) && guardian.getHp()<guardHp && ring.getHp()==ringHp,"manual guard takes priority");
        check(manualRolls.rolls==1,"no redundant auto-defend roll when manually guarded");
        GameProgress ko=new GameProgress(List.of(0,1)); equip(ko,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        student(ko,1).takeDamage(9999); Rolls koRoll=new Rolls(0,0); Battle koBattle=new Battle(ko,koRoll); int hp=student(ko,0).getHp();
        check(koBattle.enemyAttack(koBattle.getEnemies().get(0)) && student(ko,0).getHp()<hp && koRoll.rolls==1,"KO wearer cannot intercept");
        GameProgress vestParty=new GameProgress(List.of(1)); Student vestHero=student(vestParty,1);
        check(vestParty.equip(vestHero,Equipment.Slot.ARMOR,"campus_vest"),"equip starter vest");
        equip(vestParty,1,Equipment.Slot.ACCESSORY,"heroes_ring");
        check(vestHero.getMaxHp()==188,"ring HP stacks with vest percentage");
        market.advance(); withArmor.advance(); vestParty.advance(); Path dir=Files.createTempDirectory("heroes-ring-");
        try { SaveStore store=new SaveStore(dir); for(SaveStore.Format format:SaveStore.Format.values()) {
            store.save(1,format,market); GameProgress loaded=store.load(1,format); Student saved=student(loaded,1);
            check("heroes_ring".equals(saved.getEquipmentId(Equipment.Slot.ACCESSORY)) && saved.getMaxHp()==180
                    && saved.getHp()==144 && saved.getDefense()==19 && saved.getSpeed()==9,"equipped ring saved " + format);
            store.save(2,format,withArmor); check(student(store.load(2,format),1).getMaxHp()==555,"ring and Blade Mail save " + format);
            store.save(3,format,vestParty); check(student(store.load(3,format),1).getMaxHp()==188,"ring and Campus Vest save " + format);
        }} finally {try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println("Hero's Ring checks passed");
    }
}
