package haven;

import java.awt.event.KeyEvent;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.*;

class OverviewTool extends Window{
    static final String title = "Abacus";
    
    private final Label text;
    private ArrayList<Label> ls = new ArrayList<Label>();
    
    private Map<String,Entry<Float,String>> uniques = new HashMap<String,Entry<Float,String>>();
    private int sum;
    
    private static OverviewTool instance; 

    public OverviewTool(Coord c, Widget parent) {
	super(c, new Coord(300, 100), parent, title);
        
	this.text = new Label(Coord.z, this, "Creating overview. Please stand by...");
	toggle();
        
	this.pack();
    }

    public static OverviewTool instance(UI ui) {
	if(instance == null || instance.ui != ui){
	    instance = new OverviewTool(new Coord(100, 100), ui.gui);
	}
	return instance;
    }

    public static void close(){
	if(instance != null){
	    instance.ui.destroy(instance);
	    instance = null;
	}
    }

    public void toggle(){
        if(this.visible=!this.visible)
            update_text();
    }

    private void update_uniques()
    {
        uniques = new HashMap<String,Entry<Float,String>>();
        sum = ui.gui.maininv.wmap.size();
        for(GItem i : ui.gui.maininv.wmap.keySet())
        {
            String name = null,unit = null;
            float num = 1;
            try{ 
                name = ItemInfo.getContent(i.info());
                if(name!=null)
                {
                    String[] parts = name.split(" ",4);
                    num = Float.parseFloat(parts[0]);
                    unit = parts[1];
                    name = parts[3];
                }
                else
                {
                    name = i.name();
                }
            }
            catch(Loading l){continue;}
            catch(NumberFormatException n){name = i.name();}

            if(uniques.containsKey(name))
            {
                uniques.put(name,new SimpleEntry<Float,String>(uniques.get(name).getKey()+num,unit));
            }
            else
            {
                uniques.put(name,new SimpleEntry<Float,String>(num,unit));
            }
        }
    }
    private void update_text()
    {
        if(!this.visible)
            return;
        update_uniques();
        String t = String.format("Carrying %.2f/%.2f kg (%d items)", ui.gui.weight / 1000.0, ui.sess.glob.cattr.get("carry").comp / 1000.0, sum);
	this.text.settext(t);
        
        int height = 25;
        //destroy all previous labels
        Iterator<Label> itr = ls.iterator();
        while(itr.hasNext()) {
                Label l = itr.next();
                itr.remove();
                l.destroy();
        }
            
        ls = new ArrayList<Label>();
        ls.add(new Label(new Coord(0,height), this, "Overview of carried items:"));
        ArrayList<Entry<String,Entry<Float,String>>>  object_counts = new ArrayList(uniques.entrySet());
        Collections.sort(object_counts, new Comparator<Entry<String,Entry<Float,String>>>(){
            @Override
            public int compare(Entry<String, Entry<Float, String>> o1, Entry<String, Entry<Float, String>> o2) {
                String s1 = o1.getKey();
                String s2 = o2.getKey();
                if(s1 == null) s1="null";
                if(s2 == null) s2="null";
                return s1.compareTo(s2);
            }
        });
        for(Entry<String,Entry<Float,String>> e : object_counts)
        {
            height += 15;
            ls.add(new Label(new Coord(0,height), this, "   "+shortify(e.getKey())+":"));
            String unit = e.getValue().getValue();
            if(unit != null)
                ls.add(new Label(new Coord(150, height),this," " + String.format("%.2f",e.getValue().getKey())+" "+e.getValue().getValue()));
            else
                ls.add(new Label(new Coord(150, height),this," " + e.getValue().getKey().intValue()));
        }
        
        this.pack();
    }
    
    public static String shortify(String text)
    {
        int maxlength = 22;
        if (text == null) return "";
        if(text.length()<=maxlength)
        {
            return text;
        }
        else
        {
            return text.substring(0, maxlength+1)+"...";
        }
    }
    
    private boolean invalidated = false;
    public void force_update()
    {
        invalidated = true;
    }
    
    @Override
    public void tick(double dt) {
        if(invalidated)
        {
            invalidated = false;
            update_text();
        }
        
    }
    
    @Override
    public void destroy() {
	instance = null;
	super.destroy();
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
	if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) {
	    close();
	    return true;
	}
	return super.type(key, ev);
    }

    @Override
    public void wdgmsg(Widget wdg, String msg, Object... args) {
	if (wdg == cbtn) {
	    ui.destroy(this);
	} else {
	    super.wdgmsg(wdg, msg, args);
	}
    }
}
