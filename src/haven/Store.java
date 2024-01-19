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

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Store extends Window {
    public static final Text.Foundry textf = new Text.Foundry(new Font("Sans", Font.BOLD, 16), Color.BLACK).aa(true);
    public static final Text.Foundry texto = new Text.Foundry(new Font("Sans", Font.BOLD, 14), Color.BLACK).aa(true);
    public static final RichText.Foundry descfnd = new RichText.Foundry(java.awt.font.TextAttribute.FAMILY, "SansSerif",
									java.awt.font.TextAttribute.SIZE, 12,
									java.awt.font.TextAttribute.FOREGROUND, Button.defcol).aa(true);
    public static final SslHelper ssl;
    public final URI base;

    static {
	ssl = new SslHelper();
	try {
	    ssl.trust(Resource.class.getResourceAsStream("ressrv.crt"));
	} catch(java.security.cert.CertificateException e) {
	    throw(new Error("Invalid built-in certificate", e));
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }

    public Store(Coord c, Widget parent, URI base) {
	super(c, new Coord(750, 450), parent, "Salem Store");
	this.base = base;
	new Loader();
    }

    public static abstract class Currency {
	private static final Map<String, Currency> defined = new HashMap<>();
	public final String symbol;

	public Currency(String symbol) {
	    this.symbol = symbol;
	}

	public abstract String format(int amount);

	public static Currency define(Currency c) {
	    defined.put(c.symbol, c);
	    return(c);
	}
	public static Currency decimal(String symbol, int dec, String sep, String fmt) {
	    return(define(new Currency(symbol) {
		    int u = (int)Math.round(Math.pow(10, dec));
		    private String formata(int amount) {
			int a = amount / u, b = amount - (a * u);
			return(String.format("%d%s%0" + dec + "d", a, sep, b));
		    }

		    public String format(int amount) {
			String a = (amount < 0) ? "-" + formata(-amount) : formata(amount);
			return(String.format(fmt, a));
		    }
		}));
	}

	public static Currency get(String symbol) {
	    Currency ret = defined.get(symbol);
	    if(ret == null)
		throw(new IllegalArgumentException(symbol));
	    return(ret);
	}
    }
    static {Currency.decimal("USD", 2, ".", "$%s");}

    public static class Price {
	public final Currency c;
	public final int a;

	public Price(Currency c, int a) {
	    this.c = c;
	    this.a = a;
	}

	public String toString() {
	    return(c.format(a));
	}

	public static Price parse(Object[] enc) {
	    return(new Price(Currency.get((String)enc[0]), (Integer)enc[1]));
	}
    }

    public static class Offer {
	public final String id, ver;
	public String name = "", desc = null, category = null;
	public Price price;
	public Defer.Future<BufferedImage> img = null;
	public boolean singleton;
	public int sortkey;

	public Offer(String id, String ver) {
	    this.id = id;
	    this.ver = ver;
	}
    }

    public static class Category {
	public final String id;
	public String name = "", parent = null;
	public int sortkey;

	public Category(String id) {
	    this.id = id;
	}
    }

    public static class Catalog {
	public final List<Offer> offers;
	public final List<Category> catgs;
	public final Map<String, Category> catgid;

	public Catalog(List<Offer> offers, List<Category> catgs) {
	    this.offers = offers;
	    this.catgs = catgs;
	    catgid = new HashMap<>();
	    for(Category catg : catgs)
		catgid.put(catg.id, catg);
	}
    }

    public class Loader extends Widget {
	private final Defer.Future<Catalog> cat;

	public Loader() {
	    super(Coord.z, Store.this.asz, Store.this);
	    this.cat = Defer.later(Store.this::catalog);
	    Label l = new Label(Coord.z, this, "Loading...", textf);
	    l.c = sz.sub(l.sz).div(2);
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(cat.done()) {
		Catalog cat;
		try {
		    cat = this.cat.get();
		} catch(Defer.DeferredException exc) {
		    new Reloader(exc.getCause());
		    ui.destroy(this);
		    return;
		}
		new Browser(cat);
		ui.destroy(this);
	    }
	}
    }

    public class Reloader extends Widget {
	private boolean done = false;

	public Reloader(Throwable exc) {
	    super(Coord.z, Store.this.asz, Store.this);
	    Label l = new Label(Coord.z, this, "Error loading catalog", textf);
	    l.c = sz.sub(l.sz).div(2);
	    new Button(Coord.z, 75, this, "Reload") {
		public void click() {
		    done = true;
		}
	    }.c = new Coord((sz.x - 75) / 2, l.c.y + l.sz.y + 10);
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(done) {
		new Loader();
		ui.destroy(this);
	    }
	}
    }

    public static abstract class OButton extends Widget {
	public static final int imgsz = 40;
	public static final PUtils.Convolution filter = new PUtils.Lanczos(2);
	public final Text text;
	public final Defer.Future<BufferedImage> img;
	private boolean a;
	private Tex rimg;

	public OButton(Coord c, Coord sz, Widget parent, String text, Defer.Future<BufferedImage> img) {
	    super(c, sz, parent);
	    this.img = img;
	    int w = (img == null) ? sz.x - 20 : sz.x - 25 - imgsz;
	    this.text = texto.renderwrap(text, Button.defcol, w);
	}

	public void draw(GOut g) {
	    Coord off = a ? new Coord(2, 2) : Coord.z;
	    if(img == null) {
		g.image(text.tex(), sz.sub(text.sz()).div(2).add(off));
	    } else {
		try {
		    if(this.rimg == null) {
			BufferedImage rimg = this.img.get();
			Coord rsz = Utils.imgsz(rimg);
			Coord ssz = (rsz.x > rsz.y) ? new Coord(imgsz, (rsz.y * imgsz) / rsz.x) : new Coord((rsz.x * imgsz) / rsz.y, imgsz);
			BufferedImage simg = PUtils.convolvedown(rimg, ssz, filter);
			this.rimg = new TexI(simg);
		    }
		    g.image(this.rimg, new Coord(10 + ((imgsz - rimg.sz().x) / 2), (sz.y - rimg.sz().y) / 2).add(off));
		} catch(Loading l) {
		} catch(Defer.DeferredException e) {
		}
		g.image(text.tex(), new Coord(imgsz + 15, (sz.y - text.sz().y) / 2).add(off));
	    }
	    Window.wbox.draw(g, Coord.z, sz);
	}

	public abstract void click();

	public boolean mousedown(Coord c, int btn) {
	    if(btn != 1)
		return(false);
	    a = true;
	    ui.grabmouse(this);
	    return(true);
	}

	public boolean mouseup(Coord c, int btn) {
	    if(a && (btn == 1)) {
		a = false;
		ui.grabmouse(null);
		if(c.isect(Coord.z, sz))
		    click();
		return(true);
	    }
	    return(false);
	}
    }

    public class Browser extends Widget {
	public final Coord bsz = new Coord(175, 80);
	public final Catalog cat;
	public final HScrollport btns;
	private Img clbl;
	private IButton bbtn;

	public Browser(Catalog cat) {
	    super(Coord.z, Store.this.asz, Store.this);
	    this.cat = cat;
	    this.btns = new HScrollport(new Coord(10, sz.y - 180), new Coord(sz.x - 20, 180), this);
	    point(null);
	}

	public class OfferButton extends OButton {
	    public final Offer offer;

	    public OfferButton(Coord c, Coord sz, Widget parent, Offer offer) {
		super(c, sz, parent, offer.name, offer.img);
		this.offer = offer;
	    }

	    public void click() {
		new Viewer(offer, Browser.this);
	    }
	}

	public class CategoryButton extends OButton {
	    public final Category catg;

	    public CategoryButton(Coord c, Coord sz, Widget parent, Category catg) {
		super(c, sz, parent, catg.name, null);
		this.catg = catg;
	    }

	    public void click() {
		point(catg.id);
	    }
	}

	private void linebtns(Widget p, List<? extends Widget> btns, int y) {
	    int tw = 0;
	    for(Widget w : btns)
		tw += w.sz.x;
	    int e = 0, x = 0;
	    for(Widget w : btns) {
		e += p.sz.x - tw;
		int a = e / (btns.size() + 1);
		e -= a * (btns.size() + 1);
		x += a;
		w.c = new Coord(x, y);
		x += w.sz.x;
	    }
	}

	public void point(String catg) {
	    if(clbl != null) {ui.destroy(clbl); clbl = null;}
	    if(bbtn != null) {ui.destroy(bbtn); bbtn = null;}
	    while(btns.cont.child != null)
		ui.destroy(btns.cont.child);

	    if(catg != null) {
		Category ccat = cat.catgid.get(catg);
		String catp = ccat.name;
		for(Category pcat = cat.catgid.get(ccat.parent); pcat != null; pcat = cat.catgid.get(pcat.parent))
		    catp = pcat.name + " / " + catp;
		clbl = new Img(btns.c.add(25, -25), textf.render(catp, Button.defcol).tex(), this);
		bbtn = new IButton(clbl.c.add(-25, 0).add(new Coord(25, clbl.sz.y).sub(Utils.imgsz(Window.lbtni[0])).div(2)), this,
				   Window.lbtni[0], Window.lbtni[1], Window.lbtni[2]) {
			public void click() {
			    point(ccat.parent);
			}
		    };
	    }
	    List<OButton> nbtns = new ArrayList<>();
	    int x = 0, y = 0;
	    for(Category sub : cat.catgs) {
		if(sub.parent == catg) {
		    nbtns.add(new CategoryButton(new Coord(x, y).mul(bsz.add(10, 10)), bsz, btns.cont, sub));
		    if(++y > 1) {
			x++;
			y = 0;
		    }
		}
	    }
	    for(Offer offer : cat.offers) {
		if(offer.category == catg) {
		    nbtns.add(new OfferButton(new Coord(x, y).mul(bsz.add(10, 10)), bsz, btns.cont, offer));
		    if(++y > 1) {
			x++;
			y = 0;
		    }
		}
	    }
	    if(nbtns.size() <= 4) {
		linebtns(btns.cont, nbtns, 0);
	    } else if(nbtns.size() <= 8) {
		int fn = (nbtns.size() + 1) / 2;
		linebtns(btns.cont, nbtns.subList(0, fn), 0);
		linebtns(btns.cont, nbtns.subList(fn, nbtns.size()), bsz.y + 10);
	    }
	    btns.cont.update();
	    btns.bar.ch(-btns.bar.val);
	}
    }

    public class Viewer extends Widget {
	public final Offer offer;
	public final Widget back;
	private Defer.Future<Object[]> status;
	private Tex rimg;

	public Viewer(Offer offer, Widget back) {
	    super(Coord.z, Store.this.asz, Store.this);
	    this.offer = offer;
	    this.back = back;
	    this.status = Defer.later(() -> fetch("validate", "offer", offer.id, "ver", offer.ver));
	    Widget prev = new Img(new Coord(25, 50), textf.render(offer.name, Button.defcol).tex(), this);
	    new IButton(new Coord(0, 50).add(new Coord(25, prev.sz.y).sub(Utils.imgsz(Window.lbtni[0])).div(2)), this,
			Window.lbtni[0], Window.lbtni[1], Window.lbtni[2]) {
		public void click() {
		    back();
		}
	    };
	    if(offer.desc != null) {
		RichTextBox dbox = new RichTextBox(new Coord(0, 75), new Coord(500, 200), this, offer.desc, descfnd);
		dbox.bg = null;
	    }
	    back.hide();
	}

	private void back() {
	    ui.destroy(this);
	    back.show();
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(this.status != null) {
		Object[] status;
		try {
		    status = this.status.get();
		} catch(Loading l) {
		    return;
		} catch(Defer.DeferredException e) {
		    status = new Object[] {"status", "ok"};
		}
		this.status = null;
		Map<String, Object> stat = Utils.mapdecf(status, String.class, Object.class);
		if(Utils.eq(stat.get("status"), "ok")) {
		    if(!offer.singleton) {
			new Label(new Coord(300, 330), this, "Quantity:");
			new TextEntry(new Coord(350, 327), 25, this, "1");
		    }
		    new Button(new Coord(400, 325), 100, this, "Add to cart") {
			public void click() {
			    back();
			}
		    };
		} else if(Utils.eq(stat.get("status"), "invalid")) {
		} else if(Utils.eq(stat.get("status"), "obsolete")) {
		    ui.destroy(this);
		    new Loader();
		}
	    }
	}

	public void draw(GOut g) {
	    if(offer.img != null) {
		try {
		    if(rimg == null)
			rimg = new TexI(offer.img.get());
		    g.image(rimg, new Coord(sz.x - 25 - rimg.sz().x, 175 - (rimg.sz().y / 2)));
		} catch(Loading l) {
		}
	    }
	    super.draw(g);
	}
    }

    public static class IOError extends RuntimeException {
	public IOError(Throwable cause) {
	    super(cause);
	}
    }

    private URL fun(String fun, String... pars) {
	try {
	    URL ret = base.resolve(fun).toURL();
	    if(pars.length > 0)
		ret = Utils.urlparam(ret, pars);
	    return(ret);
	} catch(IOException e) {
	    throw(new IOError(e));
	}
    }

    private URLConnection req(URL url) {
	try {
	    URLConnection conn;
	    if(url.getProtocol().equals("https"))
		conn = ssl.connect(url);
	    else
		conn = url.openConnection();
	    Message auth = new Message(0);
	    auth.addstring(ui.sess.username);
	    auth.addbytes(ui.sess.sesskey);
	    conn.setRequestProperty("Authorization", "Haven " + Utils.base64enc(auth.blob));
	    return(conn);
	} catch(IOException e) {
	    throw(new IOError(e));
	}
    }

    private URLConnection req(String fun, String... pars) {
	return(req(fun(fun, pars)));
    }

    private Object[] fetch(String fun, String... pars) {
	URLConnection conn = req(fun, pars);
	try(InputStream fp = conn.getInputStream()) {
	    if(!conn.getContentType().equals("application/x-haven-ttol"))
		throw(new IOException("unexpected content-type: " + conn.getContentType()));
	    return(new Message(0, Utils.readall(fp)).list());
	} catch(IOException e) {
	    throw(new IOError(e));
	}
    }

    private Defer.Future<BufferedImage> image(URI uri) {
	return(Defer.later(() -> {
		    try {
			try(InputStream fp = req(uri.toURL()).getInputStream()) {
			    return(ImageIO.read(fp));
			}
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		}, false));
    }

    private Catalog catalog() {
	List<Offer> offers = new ArrayList<>();
	List<Category> catgs = new ArrayList<>();
	Object[] ls = fetch("offers");
	int order = 0;
	for(Object item : ls) {
	    Object[] enc = (Object[])item;
	    String type = (String)enc[0];
	    if(type.equals("offer")) {
		String id = (String)enc[1];
		String ver = (String)enc[2];
		Offer offer = new Offer(id, ver);
		offer.sortkey = order++;
		for(int a = 3; a < enc.length; a += 2) {
		    String key = (String)enc[a];
		    Object val = enc[a + 1];
		    switch(key) {
		    case "name":    offer.name      = (String)val; break;
		    case "desc":    offer.desc      = (String)val; break;
		    case "img":     offer.img       = image(base.resolve((String)val)); break;
		    case "cat":     offer.category  = ((String)val).intern(); break;
		    case "price":   offer.price     = Price.parse((Object[])val);; break;
		    case "monad":   offer.singleton = true;; break;
		    }
		}
		offers.add(offer);
	    } else if(type.equals("cat")) {
		String id = ((String)enc[1]).intern();
		Category catg = new Category(id);
		catg.sortkey = order++;
		for(int a = 2; a < enc.length; a += 2) {
		    String key = (String)enc[a];
		    Object val = enc[a + 1];
		    switch(key) {
		    case "name": catg.name   = (String)val; break;
		    case "cat":  catg.parent = ((String)val).intern(); break;
		    }
		}
		catgs.add(catg);
	    }
	}
	Collections.sort(offers, (a, b) -> (a.sortkey - b.sortkey));
	Collections.sort(catgs, (a, b) -> (a.sortkey - b.sortkey));
	return(new Catalog(offers, catgs));
    }
}
