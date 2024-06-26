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

import java.awt.Color;
import java.util.*;
import java.text.Collator;

public class BuddyWnd extends Window implements Iterable<BuddyWnd.Buddy> {
    private final List<Buddy> buddies = new ArrayList<Buddy>();
    private Map<Integer, Buddy> idmap = new HashMap<Integer, Buddy>();
    private BuddyList bl;
    private Button sbalpha;
    private Button sbgroup;
    private Button sbstatus;
    private CTextEntry nicksel, pname, charpass;
    private Buddy editing = null;
    private TextEntry opass;
    private GroupSelector grpsel;
    private FlowerMenu menu;
    public int serial = 0;
    public static final Tex online = Resource.loadtex("gfx/hud/online");
    public static final Tex offline = Resource.loadtex("gfx/hud/offline");
    public static final Color[] gc = new Color[] {
	    new Color(255, 255, 255),
	    new Color(64, 255, 64),
	    new Color(255, 64, 64),
	    new Color(96, 160, 255),
	    new Color(0, 255, 255),
	    new Color(255, 255, 0),
	    new Color(211, 64, 255),
	    new Color(255, 128, 16),
    };
    private Comparator<Buddy> bcmp;
    private Comparator<Buddy> alphacmp = new Comparator<Buddy>() {
	private Collator c = Collator.getInstance();
	public int compare(Buddy a, Buddy b) {
	    return(c.compare(a.name, b.name));
	}
    };
    private Comparator<Buddy> groupcmp = new Comparator<Buddy>() {
	public int compare(Buddy a, Buddy b) {
	    if(a.group == b.group) return(alphacmp.compare(a, b));
	    else                   return(a.group - b.group);
	}
    };
    private Comparator<Buddy> statuscmp = new Comparator<Buddy>() {
	public int compare(Buddy a, Buddy b) {
	    if(a.online == b.online) return(alphacmp.compare(a, b));
	    else                     return(b.online - a.online);
	}
    };
    
    @RName("buddy")
    public static class $_ implements Factory {
	public Widget create(Coord c, Widget parent, Object[] args) {
	    return(new BuddyWnd(c, parent));
	}
    }
    
    public class Buddy {
	public int id;
	public String name;
	Text rname = null;
	public int online;
	public int group;
	public boolean seen;
	
	public Buddy(int id, String name, int online, int group, boolean seen) {
	    this.id = id;
	    this.name = name;
	    this.online = online;
	    this.group = group;
	    this.seen = seen;
	}
	
	public void forget() {
	    wdgmsg("rm", id);
	}
	
	public void endkin() {
	    wdgmsg("rm", id);
	}
	
	public void chat() {
	    wdgmsg("chat", id);
	}
	
	public void invite() {
	    wdgmsg("inv", id);
	}
	
	public void describe() {
	    wdgmsg("desc", id);
	}
	
	public void chname(String name) {
	    wdgmsg("nick", id, name);
	}
	
	public void chgrp(int grp) {
	    wdgmsg("grp", id, grp);
	}
	
	public Text rname() {
	    if((rname == null) || !rname.text.equals(name))
		rname = Text.render(name);
	    return(rname);
	}
    }
    
    public Iterator<Buddy> iterator() {
	synchronized(buddies) {
	    return(new ArrayList<Buddy>(buddies).iterator());
	}
    }
    
    public Buddy find(int id) {
	synchronized(buddies) {
	    return(idmap.get(id));
	}
    }

    public static class CTextEntry extends TextEntry {
	public String lastset = "";
	public boolean ch = false;

	public CTextEntry(Coord c, int w, Widget parent) {
	    super(c, w, parent, "");
	}

	public void update(String text) {
	    settext(lastset = text);
	    ch = false;
	}

	protected void changed() {
	    ch = true;
	}

	protected void drawbg(GOut g) {
	    if(ch) {
		g.chcolor(32, 24, 8, 255);
		g.frect(Coord.z, sz);
		g.chcolor();
	    } else {
		g.chcolor(0, 0, 0, 255);
		g.frect(Coord.z, sz);
		g.chcolor();
	    }
	}

	public boolean type(char c, java.awt.event.KeyEvent ev) {
	    if((c == 27) && ch) {
		settext(lastset);
		ch = false;
		return(true);
	    } else {
		return(super.type(c, ev));
	    }
	}
    }

    public static class GroupSelector extends Widget {
	public int group;
	
	public GroupSelector(Coord c, Widget parent, int group) {
	    super(c, new Coord((gc.length * 20) + 20, 20), parent);
	    this.group = group;
	}
	
	public void draw(GOut g) {
	    for(int i = 0; i < gc.length; i++) {
		if(i == group) {
		    g.chcolor();
		    g.frect(new Coord(i * 20, 0), new Coord(19, 19));
		}
		g.chcolor(gc[i]);
		g.frect(new Coord(2 + (i * 20), 2), new Coord(15, 15));
	    }
	    g.chcolor();
	}
	
	public boolean mousedown(Coord c, int button) {
	    if((c.y >= 2) && (c.y < 17)) {
		int g = (c.x - 2) / 20;
		if((g >= 0) && (g < gc.length) && (c.x >= 2 + (g * 20)) && (c.x < 17 + (g * 20))) {
		    changed(g);
		    return(true);
		}
	    }
	    return(super.mousedown(c, button));
	}
	
	protected void changed(int group) {
	    this.group = group;
	}
    }

    private class BuddyList extends Listbox<Buddy> {
	public BuddyList(Coord c, int w, int h, Widget parent) {
	    super(c, parent, w, h, 20);
	}

	public Buddy listitem(int idx) {return(buddies.get(idx));}
	public int listitems() {return(buddies.size());}

	public void drawitem(GOut g, Buddy b) {
	    if(b.online == 1)
		g.image(online, Coord.z);
	    else if(b.online == 0)
		g.image(offline, Coord.z);
	    g.chcolor(gc[b.group]);
	    g.aimage(b.rname().tex(), new Coord(25, 10), 0, 0.5);
	    g.chcolor();
	}

	public void draw(GOut g) {
	    if(buddies.size() == 0)
		g.atext("You are alone in the world", sz.div(2), 0.5, 0.5);
	    super.draw(g);
	}
	
	public void change(Buddy b) {
	    sel = b;
	    if(b == null) {
		if(editing != null) {
		    editing = null;
		    ui.destroy(nicksel);
		    ui.destroy(grpsel);
		}
	    } else {
		if(editing == null) {
		    nicksel = new CTextEntry(new Coord(6, 165), 188, BuddyWnd.this) {
			public void activate(String text) {
			    editing.chname(text);
			}
		    };
		    grpsel = new GroupSelector(new Coord(6, 190), BuddyWnd.this, 0) {
			public void changed(int group) {
			    editing.chgrp(group);
			}
		    };
		    BuddyWnd.this.setfocus(nicksel);
		}
		editing = b;
		nicksel.update(b.name);
		nicksel.buf.point = nicksel.buf.line.length();
		grpsel.group = b.group;
	    }
	}

	public void opts(final Buddy b, Coord c) {
	    List<String> opts = new ArrayList<String>();
	    if(b.online >= 0) {
		opts.add("Chat");
		if(b.online == 1)
		    opts.add("Invite");
		opts.add("End kinship");
	    } else {
		opts.add("Forget");
	    }
	    if(b.seen)
		opts.add("Describe");
	    if(menu == null) {
		menu = new FlowerMenu(c, ui.root, opts.toArray(new String[opts.size()])) {
		    public void destroy() {
			menu = null;
			super.destroy();
		    }

		    public void choose(Petal opt) {
			if(opt != null) {
			    if(opt.name.equals("End kinship")) {
				b.endkin();
			    } else if(opt.name.equals("Chat")) {
				b.chat();
			    } else if(opt.name.equals("Invite")) {
				b.invite();
			    } else if(opt.name.equals("Forget")) {
				b.forget();
			    } else if(opt.name.equals("Describe")) {
				b.describe();
			    }
			    uimsg("act", opt.num);
			} else {
			    uimsg("cancel");
			}
		    }
		};
	    }
	}

	public void itemclick(Buddy b, int button) {
	    if(button == 1) {
		change(b);
	    } else if(button == 3) {
		opts(b, ui.mc);
	    }
	}
    }

    public BuddyWnd(Coord c, Widget parent) {
	super(c, new Coord(200, 450), parent, "Kin");
	bl = new BuddyList(new Coord(6, 5), 180, 7, this);
	new Label(new Coord(0, 223), this, "Sort by:");
	sbstatus = new Button(new Coord(50,  220), 48, this, "Status")      { public void click() { setcmp(statuscmp); } };
	sbgroup  = new Button(new Coord(100, 220), 48, this, "Group")       { public void click() { setcmp(groupcmp); } };
	sbalpha  = new Button(new Coord(150, 220), 48, this, "Name")        { public void click() { setcmp(alphacmp); } };
	String sort = Utils.getpref("buddysort", "");
	if(sort.equals("")) {
	    bcmp = statuscmp;
	} else {
	    if(sort.equals("alpha"))  bcmp = alphacmp;
	    if(sort.equals("group"))  bcmp = groupcmp;
	    if(sort.equals("status")) bcmp = statuscmp;
	}
	new HRuler(new Coord(0, 245), 200, this);
	new Label(new Coord(0, 250), this, "Presentation name:");
	pname = new CTextEntry(new Coord(0, 265), 200, this) {
	    public void activate(String text) {
		BuddyWnd.this.wdgmsg("pname", text);
	    }
	};
	new Button(new Coord(68, 290), 64, this, "Set") {
	    public void click() {
		BuddyWnd.this.wdgmsg("pname", pname.text);
	    }
	};
	new HRuler(new Coord(0, 315), 200, this);
	new Label(new Coord(0, 320), this, "My homestead secret:");
	charpass = new CTextEntry(new Coord(0, 335), 200, this) {
	    public void activate(String text) {
		BuddyWnd.this.wdgmsg("pwd", text);
	    }
	};
	new Button(new Coord(0  , 360), 64, this, "Set")    { public void click() {sendpwd(charpass.text);} };
	new Button(new Coord(68 , 360), 64, this, "Clear")  { public void click() {sendpwd("");} };
	new Button(new Coord(136, 360), 64, this, "Random") { public void click() {sendpwd(randpwd());} };
	new HRuler(new Coord(0, 385), 200, this);
	new Label(new Coord(0, 390), this, "Make kin by homestead secret:");
	opass = new TextEntry(new Coord(0, 405), 200, this, "") {
	    public void activate(String text) {
		BuddyWnd.this.wdgmsg("bypwd", text);
		settext("");
	    }
	};
	new Button(new Coord(68, 430), 64, this, "Add kin") {
	    public void click() {
		BuddyWnd.this.wdgmsg("bypwd", opass.text);
		opass.settext("");
	    }
	};
    }
    
    private String randpwd() {
	String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < 8; i++)
	    buf.append(charset.charAt((int)(Math.random() * charset.length())));
	return(buf.toString());
    }
    
    private void sendpwd(String pass) {
	wdgmsg("pwd", pass);
    }

    private void setcmp(Comparator<Buddy> cmp) {
	bcmp = cmp;
	String val = "";
	if(cmp == alphacmp)  val = "alpha";
	if(cmp == groupcmp)  val = "group";
	if(cmp == statuscmp) val = "status";
	Utils.setpref("buddysort", val);
	synchronized(buddies) {
	    Collections.sort(buddies, bcmp);
	}
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg.equals("add")) {
	    int id = (Integer)args[0];
	    String name = ((String)args[1]).intern();
	    int online = (Integer)args[2];
	    int group = (Integer)args[3];
	    boolean seen = ((Integer)args[4]) != 0;
	    Buddy b = new Buddy(id, name, online, group, seen);
	    synchronized(buddies) {
		buddies.add(b);
		idmap.put(b.id, b);
		Collections.sort(buddies, bcmp);
	    }
	    serial++;
	} else if(msg.equals("rm")) {
	    int id = (Integer)args[0];
	    Buddy b;
	    synchronized(buddies) {
		b = idmap.get(id);
		if(b != null) {
		    buddies.remove(b);
		    idmap.remove(id);
		}
	    }
	    if(b == editing) {
		editing = null;
		ui.destroy(nicksel);
		ui.destroy(grpsel);
	    }
	    serial++;
	} else if(msg.equals("chst")) {
	    int id = (Integer)args[0];
	    int online = (Integer)args[1];
	    Buddy b = find(id);
	    b.online = online;
	    ui.message(String.format("%s is %s now.", b.name, online>0?"ONLINE":"OFFLINE"), gc[b.group]);
	} else if(msg.equals("upd")) {
	    int id = (Integer)args[0];
	    String name = (String)args[1];
	    int online = (Integer)args[2];
	    int grp = (Integer)args[3];
	    boolean seen = ((Integer)args[4]) != 0;
	    Buddy b = find(id);
	    synchronized(b) {
		b.name = name;
		b.online = online;
		b.group = grp;
		b.seen = seen;
	    }
	    if(b == editing) {
		nicksel.update(b.name);
		grpsel.group = b.group;
	    }
	    serial++;
	} else if(msg.equals("sel")) {
	    int id = (Integer)args[0];
	    show();
	    raise();
	    bl.change(find(id));
	} else if(msg.equals("pwd")) {
	    charpass.update((String)args[0]);
	} else if(msg.equals("pname")) {
	    pname.update((String)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }
    
    public void hide() {
	if(menu != null) {
	    ui.destroy(menu);
	    menu = null;
	}
	super.hide();
    }
    
    public void destroy() {
	if(menu != null) {
	    ui.destroy(menu);
	    menu = null;
	}
	super.destroy();
    }
}
