import game.model.*;
import game.io.SaveStore;
import game.view.CampPanel;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Camp entry restores SP once per camp round, including fallen heroes. */
public final class CampSpRestorationTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        GameProgress p=new GameProgress(List.of(Student.ZACK,Student.CHARICE));
        Student z=p.getParty().stream().filter(s->s.getId()==Student.ZACK).findFirst().orElseThrow();
        Student c=p.getParty().stream().filter(s->s.getId()==Student.CHARICE).findFirst().orElseThrow();
        z.restoreSp(3); z.takeDamage(30); c.restoreSp(0); c.takeDamage(c.getHp());
        SwingUtilities.invokeAndWait(()->{
            CampPanel camp=new CampPanel(ignored->{},()->{},ignored->{},ignored->{},new Random(){public double nextDouble(){return 0;}});
            camp.open(p,false);
            check(z.getSp()==z.getMaxSp()&&c.getSp()==c.getMaxSp()&&!c.isAlive(),"entry restores SP without reviving fallen heroes");
            check(z.getHp()==45,"camp HP regeneration still runs five times");
            z.restoreSp(7);camp.resume(p,false);check(z.getSp()==7 && z.getHp()==45,"subscreen return does not start a new camp round");
            camp.open(p,false);check(z.getSp()==z.getMaxSp(),"new entry restores SP again");
        });
        p.advance();
        Path dir=Files.createTempDirectory("camp-sp-");
        try {SaveStore store=new SaveStore(dir);for(SaveStore.Format f:SaveStore.Format.values()){
            store.save(1,f,p);GameProgress loaded=store.load(1,f);
            check(loaded.getParty().stream().allMatch(s->s.getSp()==s.getMaxSp()),"restored SP persists in "+f);
        }} finally {try(var paths=Files.walk(dir)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println("Camp SP restoration checks passed");
    }
}
