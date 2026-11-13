import game.model.*;
import game.view.ShopPanel;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
public final class AccessoryShopDisplayTest {
 public static void main(String[]args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
  ShopPanel panel=new ShopPanel(()->{});panel.open(new GameProgress(List.of(Student.ZACK)));
  Field f=ShopPanel.class.getDeclaredField("offers");f.setAccessible(true);JLabel[] labels=(JLabel[])f.get(panel);
  String text=labels[1].getText();if(!text.contains("Market Charm")||!text.contains("+10% Petot")||!text.contains("restore 3% SP"))throw new AssertionError("market charm lists both utilities: "+text);
  if(labels[0].getText().contains("AP fill"))throw new AssertionError("non-accessory does not advertise SP regeneration");
 }catch(Exception e){throw new RuntimeException(e);}});System.out.println("Accessory shop display checks passed");}
}
