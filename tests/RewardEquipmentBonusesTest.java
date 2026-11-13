import game.model.*;
import game.io.SaveStore;
import game.view.InventoryPanel;
import java.util.*;
import java.nio.file.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import javax.swing.*;

/** First-stage item bonuses, combat precision, preview and persistence. */
public final class RewardEquipmentBonusesTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void near(double a, double b, String message) { check(Math.abs(a-b)<1e-8, message + " " + a); }
    static Student member(GameProgress p,int id) { return p.getParty().stream().filter(s -> s.getId()==id).findFirst().orElseThrow(); }
    static void equip(GameProgress p, int id, Equipment.Slot slot, String item) {
        p.getInventory().addEquipment(item); check(p.equip(member(p,id),slot,item),"equip " + item);
    }
    static void layout(Container c) { c.doLayout(); for(Component child:c.getComponents()) if(child instanceof Container) layout((Container) child); }
    static void key(InventoryPanel panel,String action) {panel.getActionMap().get(action).actionPerformed(new ActionEvent(panel,0,action));}
    public static void main(String[] args) throws Exception {
        GameProgress p = new GameProgress(List.of(0,1,2,3));
        Student z=member(p,0), b=member(p,1), c=member(p,2), a=member(p,3);
        equip(p,0,Equipment.Slot.WEAPON,"scholar_staff"); equip(p,0,Equipment.Slot.ACCESSORY,"focus_charm");
        equip(p,1,Equipment.Slot.WEAPON,"cadet_blade"); equip(p,2,Equipment.Slot.WEAPON,"hunter_crossbows");
        equip(p,3,Equipment.Slot.WEAPON,"champion_wraps");
        check(z.getAttack()==29 && b.getAttack()==19 && c.getAttack()==14 && a.getAttack()==28, "attack bonuses");
        near(z.getEffectiveAccuracy(),9.2,"both accuracy bonuses stack"); near(c.getEffectiveAgility(),14.3,"agility bonus retains fraction");
        near(a.getEffectiveSpeed(),5.5,"speed bonus retains fraction"); check(HealthRegeneration.amount(b)==27,"cadet regeneration adds ten percentage points");
        GameProgress tank = new GameProgress(List.of(1)); equip(tank,1,Equipment.Slot.WEAPON,"cadet_blade");
        equip(tank,1,Equipment.Slot.ARMOR,"blade_mail");
        check(HealthRegeneration.amount(member(tank,1))==200,"weapon and armor regeneration bonuses stack");
        Random hitRoll = new Random() {public int nextInt(int bound) {return 0;} public double nextDouble() {return 0.735;}};
        GameProgress unarmed = new GameProgress(List.of(0)); Battle plain = new Battle(unarmed,hitRoll);
        Student plainMage=member(unarmed,0); Enemy plainEnemy=plain.getEnemies().get(0);
        check(plain.selectStudent(plainMage) && plain.attack(plainEnemy) && plainEnemy.getHp()==plainEnemy.getMaxHp(),"base accuracy misses");
        GameProgress boosted = new GameProgress(List.of(0)); equip(boosted,0,Equipment.Slot.WEAPON,"scholar_staff");
        equip(boosted,0,Equipment.Slot.ACCESSORY,"focus_charm"); Battle accurate = new Battle(boosted,hitRoll);
        Enemy accurateEnemy=accurate.getEnemies().get(0);
        check(accurate.selectStudent(member(boosted,0)) && accurate.attack(accurateEnemy) && accurateEnemy.getHp()<accurateEnemy.getMaxHp(),"bonus changes hit outcome");
        near(Battle.hitChance(c.getEffectiveAgility(),z.getEffectiveAccuracy(),100), 1 - Math.min(.60,14.3/(14.3+9.2)),"precise hit formula");
        near(z.accuracyWithEquipment(Equipment.Slot.WEAPON,null),8.4,"preview removal keeps charm");
        near(c.agilityWithEquipment(Equipment.Slot.WEAPON,null),13,"preview removes crossbows");
        near(a.effectiveSpeedWithEquipment(Equipment.Slot.WEAPON,null),5,"preview removes wraps");
        for (String id : List.of("focus_charm","scholar_staff","cadet_blade","hunter_crossbows","champion_wraps"))
            check(Equipment.fromId(id).getSpecialDescription().contains(id.equals("focus_charm") ? "+5%" : "+10%"), "item description " + id);
        p.advance(); Path dir=Files.createTempDirectory("item-bonus-");
        try { SaveStore store=new SaveStore(dir); for(SaveStore.Format fmt:SaveStore.Format.values()) {
            store.save(1,fmt,p); GameProgress loaded=store.load(1,fmt);
            near(member(loaded,0).getEffectiveAccuracy(),9.2,"saved ACC " + fmt);
            near(member(loaded,2).getEffectiveAgility(),14.3,"saved AGI " + fmt);
            near(member(loaded,3).getEffectiveSpeed(),5.5,"saved SPD " + fmt);
            check(HealthRegeneration.amount(member(loaded,1))==27,"saved regeneration " + fmt);
        }} finally {try(var files=Files.walk(dir)) {for(Path f:files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(f);}}
        SwingUtilities.invokeAndWait(() -> { try {
            game.view.BattlePanel battleUi = new game.view.BattlePanel(() -> {}, ignored -> {});
            Method rate = game.view.BattlePanel.class.getDeclaredMethod("studentRate",Student.class,double.class); rate.setAccessible(true);
            near((double)rate.invoke(battleUi,a,10.0),10.25,"speed bonus increases AP fill rate");
            Method format = InventoryPanel.class.getDeclaredMethod("formatStat",double.class); format.setAccessible(true);
            check("14".equals(format.invoke(null,13.65)),"stats display the ceiling without decimals");
            InventoryPanel ui=new InventoryPanel(()->{}); ui.open(p); ui.setSize(960,540); layout(ui);
            key(ui,"inventory-confirm"); layout(ui);
            Field field=InventoryPanel.class.getDeclaredField("details"); field.setAccessible(true); JTextArea details=(JTextArea)field.get(ui);
            JScrollPane scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,details);
            check(details.getLineWrap() && scroll.getHorizontalScrollBarPolicy()==JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,"no horizontal scrolling");
            check(details.getText().contains("Preview:\n") && details.getText().contains("AGI ") && details.getText().contains("ACC "),"column preview contains stats");
        } catch(ReflectiveOperationException e) {throw new RuntimeException(e);} });
        System.out.println("Reward equipment bonuses checks passed");
    }
}
