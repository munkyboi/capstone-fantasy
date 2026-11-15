import game.io.SaveStore;
import game.model.*;
import game.view.BattlePanel;
import game.view.InventoryPanel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.StyleConstants;

/** Tests Focus Charm, Campus set bonuses, direct-hit recovery and save compatibility. */
public final class FocusAndCampusSetTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Object field(Object target, String name) throws Exception { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
    static void equip(GameProgress p, Student s, Equipment.Slot slot, String id) { p.getInventory().addEquipment(id); check(p.equip(s,slot,id),"equip "+id); }
    @SuppressWarnings("unchecked") static void setEnemy(Battle battle, Enemy enemy) throws Exception { List<Enemy> enemies=(List<Enemy>)field(battle,"enemies"); enemies.clear(); enemies.add(enemy); }
    static class Rolls extends Random { double chance=.99; @Override public double nextDouble(){ return chance; } @Override public int nextInt(int n){return 0;} }
    public static void main(String[] args) throws Exception {
        Rolls rolls=new Rolls(); GameProgress p=new GameProgress(List.of(Student.BOGART)); Student hero=p.getParty().get(0);
        equip(p,hero,Equipment.Slot.ACCESSORY,"focus_charm"); check(hero.getAttack()==hero.getBaseAttack()+5,"focus ATK");
        check(Math.abs(hero.getEffectiveAccuracy()-hero.getAccuracy()*1.5)<.001,"focus ACC");
        check(hero.getSpeed()==11,"focus SPD rounded consistently");
        Battle battle=new Battle(p,rolls); Enemy enemy=Enemy.sentinel("Target",false,GameSettings.Difficulty.NORMAL,0,1,0); setEnemy(battle,enemy);
        for(int i=1;i<=4;i++){
            check(battle.selectStudent(hero)&&battle.attack(enemy),"miss attack");
            check(battle.getFocusPenaltyStacks(hero)==Math.min(i,3),"stack increments and caps");
            check(battle.consumeApRefills().contains(hero),"miss grants ready AP");
        }
        int hp=enemy.getHp(); rolls.chance=0;
        check(battle.selectStudent(hero)&&battle.attack(enemy),"penalized attack");
        int base=Battle.damageAfterDefense(hero.getAttack(),enemy.getDefense());
        check(hp-enemy.getHp()==(int)Math.ceil(base*.1-1e-9),"three stacks reduce next action damage by 90%");
        check(battle.getFocusPenaltyStacks(hero)==0&&battle.consumeApRefills().isEmpty(),"successful attack clears penalty");

        Rolls twinRolls=new Rolls(); GameProgress t=new GameProgress(List.of(Student.CHARICE)); Student hunter=t.getParty().get(0);
        equip(t,hunter,Equipment.Slot.ACCESSORY,"focus_charm"); Battle twin=new Battle(t,twinRolls); Enemy twinEnemy=Enemy.sentinel("Twin target",false,GameSettings.Difficulty.NORMAL,0,1,0); setEnemy(twin,twinEnemy);
        check(twin.selectStudent(hunter)&&twin.attack(twinEnemy)&&twin.hasPendingTwinShot(),"twin shot starts");
        check(twin.consumeApRefills().isEmpty(),"no extra turn until the action ends");
        check(twin.advanceTwinShot()&&!twin.hasPendingTwinShot(),"second guaranteed hit completes action");
        check(twin.getFocusPenaltyStacks(hunter)==1&&twin.consumeApRefills().contains(hunter),"one of two misses triggers focus");

        Rolls thirdRolls=new Rolls(){ private int draw; @Override public double nextDouble(){ return draw++ == 0 ? .99 : 0; } };
        GameProgress three=new GameProgress(List.of(Student.CHARICE));Student threeHero=three.getParty().get(0);equip(three,threeHero,Equipment.Slot.ACCESSORY,"focus_charm");
        Battle threeShot=new Battle(three,thirdRolls);Enemy threeTarget=Enemy.sentinel("Three shots",false,GameSettings.Difficulty.NORMAL,0,1,0);setEnemy(threeShot,threeTarget);
        check(threeShot.selectStudent(threeHero)&&threeShot.attack(threeTarget)&&threeShot.advanceTwinShot()&&threeShot.advanceTwinShot(),"three shot action");
        check(threeShot.getFocusPenaltyStacks(threeHero)==0&&threeShot.consumeApRefills().isEmpty(),"one miss out of three does not grant a turn");

        Rolls setRolls=new Rolls(); setRolls.chance=0; GameProgress q=new GameProgress(List.of(Student.ZACK)); Student mage=q.getParty().get(0);
        int hpBase=mage.getMaxHp(), atkBase=mage.getAttack(), defBase=mage.getDefense(), spBase=mage.getMaxSp(), speedBase=mage.getSpeed();
        q.getInventory().addEquipment("campus_medal"); check(q.equip(mage,Equipment.Slot.ACCESSORY,"campus_medal"),"medal");
        check(mage.getMaxHp()==Math.round(hpBase*1.1)&&mage.getAttack()==atkBase&&mage.getDefense()==defBase,"medal HP without set");
        check(Math.abs(mage.getEffectiveAccuracy()-mage.getAccuracy()*1.1)<.001&&Math.abs(mage.getEffectiveAgility()-mage.getAgility()*1.1)<.001,"medal AGI and ACC");
        check(q.equip(mage,Equipment.Slot.ARMOR,"campus_vest")&&mage.hasCampusSet(),"campus set equips");
        check(mage.getMaxHp()==Math.round(hpBase*1.15)&&mage.getAttack()==Math.round(atkBase*1.1),"set HP and ATK");
        check(mage.getDefense()==Math.round((defBase+4)*1.1)&&mage.getSpeed()==Math.round(speedBase*1.1),"set DEF and SPD");
        check(mage.getMaxSp()==Math.round(spBase*1.1),"set maximum SP");
        Battle setBattle=new Battle(q,setRolls); Enemy victim=Enemy.sentinel("SP target",false,GameSettings.Difficulty.NORMAL,0,1,0); setEnemy(setBattle,victim);mage.restoreSp(0);
        int victimHp=victim.getHp(); check(setBattle.selectStudent(mage)&&setBattle.attack(victim),"set direct attack");int direct=victimHp-victim.getHp();
        check(mage.getSp()==(int)Math.ceil(direct*.03),"direct hit returns 3% damage as SP");
        mage.restoreSp(0);Object dots=field(setBattle,"dots");Method add=dots.getClass().getDeclaredMethod("add",GameCharacter.class,GameCharacter.class,int.class,boolean.class,int.class,int.class);add.setAccessible(true);
        add.invoke(dots,mage,victim,50,false,5,0);check(setBattle.tickDamageOverTime(victim),"DOT ticks");check(mage.getSp()==0,"DOT does not trigger set SP");

        mage.restoreSp(40);int beforeAbility=victim.getHp();
        check(setBattle.selectStudent(mage)&&setBattle.ability(null,null),"set direct ability");
        int abilityDamage=beforeAbility-victim.getHp();
        check(mage.getSp()==40-mage.getAbilitySpCost()+(int)Math.ceil(abilityDamage*.03),"direct ability damage restores SP");

        q.advance();
        Path dir=Files.createTempDirectory("campus-set-save-");
        try { SaveStore store=new SaveStore(dir); for(SaveStore.Format fmt:SaveStore.Format.values()){
            mage.restoreSp(mage.getMaxSp()); store.save(1,fmt,q);Student loaded=store.load(1,fmt).getParty().get(0);
            check(loaded.hasCampusSet()&&loaded.getMaxHp()==mage.getMaxHp()&&loaded.getSp()==mage.getMaxSp(),"set survives "+fmt);
        }
        Path file=dir.resolve("slot-1.txt");String text=Files.readString(file);int oldMax=(int)Math.round(hpBase*1.05);
        text=text.replace("student.1.maxHp="+mage.getMaxHp(),"student.1.maxHp="+oldMax).replace("student.1.hp="+mage.getHp(),"student.1.hp="+oldMax).replace("student.1.sp="+mage.getMaxSp(),"student.1.sp="+spBase);Files.writeString(file,text);
        Student legacy=store.load(1,SaveStore.Format.TEXT).getParty().get(0);check(legacy.hasCampusSet()&&legacy.getMaxHp()==mage.getMaxHp()&&legacy.getHp()==legacy.getMaxHp(),"old medal HP migrates");
        } finally {try(var files=Files.walk(dir)){for(Path f:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}

        SwingUtilities.invokeAndWait(()->{try{
            InventoryPanel ui=new InventoryPanel(()->{});ui.open(q);JTextPane bag=(JTextPane)field(ui,"bagContents");
            q.equip(mage,Equipment.Slot.ACCESSORY,null);ui.open(q);String bagText=bag.getText();int offset=bagText.indexOf("Campus Medal");
            check(offset>=0&&new Color(168,230,163).equals(StyleConstants.getForeground(bag.getStyledDocument().getCharacterElement(offset).getAttributes())),"set item name is green in bag");
            Rolls uiRolls=new Rolls();GameProgress uiProgress=new GameProgress(List.of(Student.BOGART));Student uiHero=uiProgress.getParty().get(0);equip(uiProgress,uiHero,Equipment.Slot.ACCESSORY,"focus_charm");
            BattlePanel panel=new BattlePanel(()->{},ignored->{},new GameSettings(),uiRolls);panel.start(uiProgress,false,0);Battle uiBattle=(Battle)field(panel,"battle");
            panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));panel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(panel,0,""));
            check(uiBattle.getFocusPenaltyStacks(uiHero)==1&&uiBattle.getCurrent()==uiHero,"miss grants immediately selectable extra turn");
            Rolls priorityRolls=new Rolls();GameProgress team=new GameProgress(List.of(Student.CHARICE,Student.BOGART));
            Student slower=team.getParty().stream().filter(student -> student.getId()==Student.BOGART).findFirst().orElseThrow();
            equip(team,slower,Equipment.Slot.ACCESSORY,"focus_charm");BattlePanel teamPanel=new BattlePanel(()->{},ignored->{},new GameSettings(),priorityRolls);teamPanel.start(team,false,0);
            Battle teamBattle=(Battle)field(teamPanel,"battle");Field turn=Battle.class.getDeclaredField("turn");turn.setAccessible(true);turn.setInt(teamBattle,-1);
            check(teamBattle.selectStudent(slower),"select slower hero despite teammate being ready");
            teamPanel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(teamPanel,0,""));teamPanel.getActionMap().get("battle-confirm").actionPerformed(new ActionEvent(teamPanel,0,""));
            check(teamBattle.getCurrent()==slower,"Focus extra turn takes priority over another ready hero");
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(teamPanel,name)).stop();
            for(String name:List.of("effectTimer","ambushAlertTimer","victoryTimer","apTimer","secondCastTimer","looperHitTimer","angelAttackTimer","twinShotTimer","sparkExplosionTimer","monsterTurnTimer","abilityAlertTimer"))((javax.swing.Timer)field(panel,name)).stop();
        }catch(Exception e){throw new RuntimeException(e);}});
        System.out.println("Focus Charm and Campus set checks passed");
    }
}
