/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.minimap.Radar.GobBlink;
import haven.res.lib.tree.TreeSprite;

import java.awt.Color;
import java.util.*;

public class Gob implements Sprite.Owner, Skeleton.ModOwner, Rendered {
    public Coord rc, sc;
    public Coord3f sczu;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public int frame, initdelay = (int)(Math.random() * 3000) + 3000;
    public final Glob glob;
    Map<Class<? extends GAttrib>, GAttrib> attr = new HashMap<Class<? extends GAttrib>, GAttrib>();
    public Collection<Overlay> ols = new LinkedList<Overlay>();
    private GobPath path = null;
    private static List<Long> timeList = new LinkedList<Long>();
	
    public static class Overlay implements Rendered {
	public Indir<Resource> res;
	public Message sdt;
	public Sprite spr;
	public int id;
	public boolean delign = false;
	
	public Overlay(int id, Indir<Resource> res, Message sdt) {
	    this.id = id;
	    this.res = res;
	    this.sdt = sdt;
	    spr = null;
	}
	
	public Overlay(Sprite spr) {
	    this.id = -1;
	    this.res = null;
	    this.sdt = null;
	    this.spr = spr;
	}
	
	public static interface CDel {
	    public void delete();
	}
	
	public static interface CUpd {
	    public void update(Message sdt);
	}
	
	public static interface SetupMod {
	    public void setupgob(GLState.Buffer buf);
	    public void setupmain(RenderList rl);
	}

	public void draw(GOut g) {}
	public boolean setup(RenderList rl) {
	    if(spr != null)
		rl.add(spr, null);
	    return(false);
	}
    }
    
    public Gob(Glob glob, Coord c, long id, int frame) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	this.frame = frame;
    }
	
    public Gob(Glob glob, Coord c) {
	this(glob, c, -1, 0);
    }
	
    public static interface ANotif<T extends GAttrib> {
	public void ch(T n);
    }
	
    public void ctick(int dt) {
	int dt2 = dt + initdelay;
	initdelay = 0;
	for(GAttrib a : attr.values()) {
	    if(a instanceof Drawable)
		a.ctick(dt2);
	    else
		a.ctick(dt);
	}
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.spr == null) {
		try {
		    ol.sdt.off = 0;
		    ol.spr = Sprite.create(this, ol.res.get(), ol.sdt);
		} catch(Loading e) {
                    if (e.getMessage() != null && e.getMessage().contains("Too many sounds playing at once.")) {

                        synchronized(timeList) {

                            int counter = 0;
                            Long now = System.currentTimeMillis();

                            Iterator<Long> i_timeList = timeList.iterator();

                            while(i_timeList.hasNext()){
                               Long occurance = i_timeList.next();

                               if (now - occurance > 200 ) {
                                 i_timeList.remove();
                              }else{
                                 counter++;
                              }
                            }

                            timeList.add(now);

                            if (counter > 4) {
                                i.remove();
                            }
                        }
                    }
                } catch (Exception e) {
                   // guess just ignore...
                }
	    } else {
		boolean done = ol.spr.tick(dt);
		if((!ol.delign || (ol.spr instanceof Overlay.CDel)) && done)
		    i.remove();
	    }
	}
	if(virtual && ols.isEmpty())
	    glob.oc.remove(id);
	loc.tick();
    }
	
    public Overlay findol(int id) {
	for(Overlay ol : ols) {
	    if(ol.id == id)
		return(ol);
	}
	return(null);
    }

    public void tick() {
	for(GAttrib a : attr.values())
	    a.tick();
    }
    
    public void dispose() {
	for(GAttrib a : attr.values())
	    a.dispose();
    }
	
    public void move(Coord c, double a) {
	Moving m = getattr(Moving.class);
	if(m != null)
	    m.move(c);
	this.rc = c;
	this.a = a;
    }
	
    public Coord3f getc() {
	Moving m = getattr(Moving.class);
	Coord3f ret = (m != null) ? m.getc() : getrc();
	DrawOffset df = getattr(DrawOffset.class);
	if(df != null)
	    ret = ret.add(df.off);
	return(ret);
    }
    
    public Coord3f getrc() {
	return(new Coord3f(rc.x, rc.y, glob.map.getcz(rc)));
    }
	
    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
	while(true) {
	    Class<?> p = cl.getSuperclass();
	    if(p == GAttrib.class)
		return(cl);
	    cl = p.asSubclass(GAttrib.class);
	}
    }

    public void setattr(GAttrib a) {
	Class<? extends GAttrib> ac = attrclass(a.getClass());
	if (Config.gobpath) {
	    if (ac == Moving.class) {
		if (path == null) {
		    path = new GobPath(this);
		    ols.add(new Overlay(path));
		}
		path.move((Moving) a);
	    }
	}
	attr.put(ac, a);
    }
	
    public <C extends GAttrib> C getattr(Class<C> c) {
	GAttrib attr = this.attr.get(attrclass(c));
	if(!c.isInstance(attr))
	    return(null);
	return(c.cast(attr));
    }
	
    public void delattr(Class<? extends GAttrib> c) {
	Class<? extends GAttrib> aClass = attrclass(c);
	attr.remove(aClass);
	if(aClass == Moving.class && path != null) {
	    path.stop();
	}
    }
	
    public void draw(GOut g) {}

    public boolean setup(RenderList rl) {
        ResDrawable rd = this.getattr(ResDrawable.class);
	for(Overlay ol : ols)
	    rl.add(ol, null);
	for(Overlay ol : ols) {
	    if(ol.spr instanceof Overlay.SetupMod)
		((Overlay.SetupMod)ol.spr).setupmain(rl);
	}
	GobHealth hlt = getattr(GobHealth.class);
	if(hlt != null)
	    rl.prepc(hlt.getfx());
	if(Config.blink){
	    GobBlink blnk = getattr(GobBlink.class);
	    if(blnk != null && blnk.visible())
		rl.prepc(blnk.getfx());
	}
        
        if(Config.raidermodebraziers){
            boolean brazier = false;
            
            if(rd!=null && rd.res != null)
            {
                brazier = rd.res.get().name.contains("brazier");
            }
            
            if(brazier && hlt != null && hlt.asfloat()>0.5)
            {                
                Material.Colors fx = new Material.Colors();
                Color c = new Color(255, 105, 180, 200);
                fx.amb = Utils.c2fa(c);
                fx.dif = Utils.c2fa(c);
                fx.emi = Utils.c2fa(c);
                rl.prepc(fx);
            }
        }
        
        //highlight fruit trees with fruit and thornbushes with flowers
        if(Config.farmermodetrees){
            boolean thornbush = false;
            
            if(rd!=null && rd.res != null)
            {
                thornbush = rd.res.get().name.contains("thornbush");
            }
            
            if(thornbush)
            {            
                if(rd.spr!= null && ((StaticSprite)rd.spr).parts.length > 1)
                {
                    Material.Colors fx = new Material.Colors();
                    Color c = new Color(28, 255, 28, 200);
                    fx.amb = Utils.c2fa(c);
                    fx.dif = Utils.c2fa(c);
                    fx.emi = Utils.c2fa(c);
                    rl.prepc(fx);
                }
            }
            
            boolean fruittree = false;
            
            if(rd!=null && rd.res != null)
            {
                fruittree = rd.res.get().name.contains("apple") || 
                            rd.res.get().name.contains("cherry") || 
                            rd.res.get().name.contains("mulberry") || 
                            rd.res.get().name.contains("pear") || 
                            rd.res.get().name.contains("peach") || 
                            rd.res.get().name.contains("persimmon") || 
                            rd.res.get().name.contains("plum") || 
                            rd.res.get().name.contains("snozberry");
            }
            
            if(fruittree)
            {            
		if(rd.spr!= null && ((StaticSprite)rd.spr).parts.length > 2 && !rd.sdt.toString().equals("Message(0): 03 00 00 00 "))
		{
			Material.Colors fx = new Material.Colors();
			Color c = new Color(205, 205, 255, 200);
			fx.amb = Utils.c2fa(c);
			fx.dif = Utils.c2fa(c);
			fx.emi = Utils.c2fa(c);
			rl.prepc(fx);
		}
		else if(rd.spr!= null && ((StaticSprite)rd.spr).parts.length > 2 && rd.sdt.toString().equals("Message(0): 03 00 00 00 "))
		{
				((StaticSprite)rd.spr).prepc_location = TreeSprite.mkscale(0.5f);
		}
                else
                {
                    if(rd.spr!=null)
                            ((StaticSprite)rd.spr).prepc_location = TreeSprite.mkscale(0.2f);
                }
            }
        }

	GobHighlight highlight = getattr(GobHighlight.class);
	if(highlight != null){
	    if(highlight.duration > 0) {
		rl.prepc(highlight.getfx());
	    } else {
		delattr(GobHighlight.class);
	    }
	}

        Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.setup(rl);
	Speaking sp = getattr(Speaking.class);
	if(sp != null)
	    rl.add(sp.fx, null);
	KinInfo ki = getattr(KinInfo.class);
	if(ki != null)
	    rl.add(ki.fx, null);
	return(false);
    }

    public Random mkrandoom() {
	return(new Random(id));
    }
    
    public Resource.Neg getneg() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getneg());
	return(null);
    }

    public Glob glob() {
	return(glob);
    }

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
	Moving m = getattr(Moving.class);
	if(m == null)
	    return(0);
	return(m.getv());
    }
    
    public final GLState olmod = new GLState() {
	    public void apply(GOut g) {}
	    public void unapply(GOut g) {}
	    public void prep(Buffer buf) {
		for(Overlay ol : ols) {
		    if(ol.spr instanceof Overlay.SetupMod) {
			((Overlay.SetupMod)ol.spr).setupgob(buf);
		    }
		}
	    }
	};

    public static final GLState.Slot<Save> savepos = new GLState.Slot<Save>(GLState.Slot.Type.SYS, Save.class, PView.loc);
    public class Save extends GLState {
	public Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(),
	    mv = new Matrix4f();
	public Projection proj = null;
	
	public void apply(GOut g) {
	    mv.load(cam.load(g.st.cam)).mul1(wxf.load(g.st.wxf));
	    Projection proj = g.st.cur(PView.proj);
	    Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), g.sz);
	    Gob.this.sc = new Coord(s);
	    Gob.this.sczu = proj.toscreen(mv.mul4(Coord3f.zu), g.sz).sub(s);
	    this.proj = proj;
	}
	
	public void unapply(GOut g) {}
	
	public void prep(Buffer buf) {
	    buf.put(savepos, this);
	}
    }
    
    public final Save save = new Save();
    public class GobLocation extends Location {
	public Coord3f c = null;
	private double a = 0.0;
	private Matrix4f update = null;

	public GobLocation() {
	    super(Matrix4f.id);
	}
	
	public void tick() {
	    try {
		Coord3f c = getc();
		c.y = -c.y;
		if((this.c == null) || !c.equals(this.c) || (this.a != Gob.this.a)) {
		    update(makexlate(new Matrix4f(), this.c = c)
			   .mul1(makerot(new Matrix4f(), Coord3f.zu, (float)-(this.a = Gob.this.a))));
		}
	    } catch(Loading l) {}
	}

	public Location freeze() {
	    return(new Location(fin(Matrix4f.id)));
	}
    }
    public final GobLocation loc = new GobLocation();
    
}
