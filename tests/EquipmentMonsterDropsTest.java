import game.model.*;
import game.io.SaveStore;
import java.nio.file.*;
import java.util.*;

public final class EquipmentMonsterDropsTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Random fixed(double value){return new Random(){public double nextDouble(){return value;}};}
    public static void main(String[]args)throws Exception {
        Enemy w=new Enemy("Wisp"); Enemy s=new Enemy("Sentinel",true); w.takeDamage(99999); s.takeDamage(99999);
        List<MonsterLoot.Drop> table=MonsterLoot.tableFor(w);
        check(table.size()==16 && MonsterLoot.tableFor(s).equals(table),"same 15 equipment and flower for both monster types");
        for(MonsterLoot.Drop item:table){check(item.chance()==.15,"base rate " + item.itemId()); check(CarbonMarket.offers().stream().noneMatch(o->o.id().equals(item.itemId())),"shop item excluded " + item.itemId());}
        for(GameSettings.Difficulty d:GameSettings.Difficulty.values()){
            double limit=.15*d.multiplier(); GameProgress p=new GameProgress(List.of(Student.ZACK));
            check(MonsterLoot.award(List.of(w),p,fixed(limit-0.00001),d).contains("white_flower"),"chance below " + d);
            p=new GameProgress(List.of(Student.ZACK)); check(!MonsterLoot.award(List.of(w),p,fixed(limit),d).contains("white_flower"),"chance boundary " + d);
        }
        GameProgress p=new GameProgress(List.of(Student.ZACK)); List<String> drops=MonsterLoot.award(List.of(w),p,fixed(0));
        check(drops.contains("focus_charm")&&drops.contains("scholar_staff"),"stage rewards can drop before stage completion");
        check(p.equip(p.getParty().get(0),Equipment.Slot.ACCESSORY,"focus_charm"),"equip early drop");
        check(p.claimStageRewardItems().isEmpty(),"owned rewards omitted whether in bag or equipped");
        check(p.getInventory().isRewardClaimed(0),"stage remains claimed");
        p.advance(); Path dir=Files.createTempDirectory("loot-unique"); try {SaveStore store=new SaveStore(dir); for(SaveStore.Format f:SaveStore.Format.values()){store.save(1,f,p);check(store.load(1,f)!=null,"save loads " + f);}} finally {try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println("Equipment monster drop checks passed");
    }
}
