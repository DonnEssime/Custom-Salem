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

import java.awt.RenderingHints;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.lang.reflect.*;
import java.util.prefs.*;
import java.util.*;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.*;

public class Utils {
    private static final SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    public static final java.nio.charset.Charset utf8 = java.nio.charset.Charset.forName("UTF-8");
    public static final java.nio.charset.Charset ascii = java.nio.charset.Charset.forName("US-ASCII");
    public static final java.awt.image.ColorModel rgbm = java.awt.image.ColorModel.getRGBdefault();
    private static Preferences prefs = null;

    static Coord imgsz(BufferedImage img) {
        if(img==null)
            return new Coord(0,0);
        
	return(new Coord(img.getWidth(), img.getHeight()));
    }
	
    public static void defer(final Runnable r) {
	Defer.later(new Defer.Callable<Object>() {
		public Object call() {
		    r.run();
		    return(null);
		}
	    });
    }
	
    static void drawgay(BufferedImage t, BufferedImage img, Coord c) {
	Coord sz = imgsz(img);
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		int p = img.getRGB(x, y);
		if(Utils.rgbm.getAlpha(p) > 128) {
		    if((p & 0x00ffffff) == 0x00ff0080)
			t.setRGB(x + c.x, y + c.y, 0);
		    else
			t.setRGB(x + c.x, y + c.y, p);
		}
	    }
	}
    }
	
    public static int drawtext(Graphics g, String text, Coord c) {
	java.awt.FontMetrics m = g.getFontMetrics();
	g.drawString(text, c.x, c.y + m.getAscent());
	return(m.getHeight());
    }
	
    static Coord textsz(Graphics g, String text) {
	java.awt.FontMetrics m = g.getFontMetrics();
	java.awt.geom.Rectangle2D ts = m.getStringBounds(text, g);
	return(new Coord((int)ts.getWidth(), (int)ts.getHeight()));
    }
	
    static void aligntext(Graphics g, String text, Coord c, double ax, double ay) {
	java.awt.FontMetrics m = g.getFontMetrics();
	java.awt.geom.Rectangle2D ts = m.getStringBounds(text, g);
	g.drawString(text, (int)(c.x - ts.getWidth() * ax), (int)(c.y + m.getAscent() - ts.getHeight() * ay));
    }
    
    public static String datef(long time){
	return datef.format(new Date(time));
    }
    
    public static String current_date(){
	return datef(System.currentTimeMillis());
    }
    
    public static String fpformat(int num, int div, int dec) {
	StringBuilder buf = new StringBuilder();
	boolean s = false;
	if(num < 0) {
	    num = -num; s = true;
	}
	for(int i = 0; i < div - dec; i++)
	    num /= 10;
	for(int i = 0; i < dec; i++) {
	    buf.append((char)('0' + (num % 10)));
	    num /= 10;
	}
	buf.append('.');
	if(num == 0) {
	    buf.append('0');
	} else {
	    while(num > 0) {
		buf.append((char)('0' + (num % 10)));
		num /= 10;
	    }
	}
	if(s)
	    buf.append('-');
	return(buf.reverse().toString());
    }
    
    static void line(Graphics g, Coord c1, Coord c2) {
	g.drawLine(c1.x, c1.y, c2.x, c2.y);
    }
	
    static void AA(Graphics g) {
	java.awt.Graphics2D g2 = (java.awt.Graphics2D)g;
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);		
    }
    
    public static String getClipboard() {
	Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
	try {
	    if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
		String text = (String)t.getTransferData(DataFlavor.stringFlavor);
		return text;
	    }
	} catch (UnsupportedFlavorException e) {
	} catch (IOException e) {
	}
	return "";
    }
    
	

    public static URI uri(String uri) {
	try {
	    return(new URI(uri));
	} catch(URISyntaxException e) {
	    throw(new IllegalArgumentException(uri, e));
	}
    }

    public static URL url(String url) {
	try {
	    return(uri(url).toURL());
	} catch(MalformedURLException e) {
	    throw(new IllegalArgumentException(url, e));
	}
    }

    public static Path path(String path) {
	if(path == null)
	    return(null);
	return(FileSystems.getDefault().getPath(path));
    }

    public static Path pj(Path base, String... els) {
	for(String el : els)
	    base = base.resolve(el);
	return(base);
    }

    static synchronized Preferences prefs() {
	if(prefs == null) {
	    Preferences node = Preferences.userNodeForPackage(Utils.class);
	    if(Config.prefspec != null)
		node = node.node(Config.prefspec);
	    prefs = node;
	}
	return(prefs);
    }

    static String getpref(String prefname, String def) {
	try {
	    return(prefs().get(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }
	
    static void setpref(String prefname, String val) {
	try {
	    prefs().put(prefname, val);
	} catch(SecurityException e) {
	}
    }
    
    static boolean getprefb(String prefname, boolean def) {
	try {
	    return(prefs().getBoolean(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }
    
    static void setprefb(String prefname, boolean val) {
	try {
	    prefs().putBoolean(prefname, val);
	} catch(SecurityException e) {
	}
    }

    static void setpreff(String prefname, float val) {
	try {
	    prefs().putFloat(prefname, val);
	} catch(SecurityException e) {
	}
    }
    
    static Coord getprefc(String prefname, Coord def) {
	try {
	    String val = prefs().get(prefname, null);
	    if(val == null)
		return(def);
	    int x = val.indexOf('x');
	    if(x < 0)
		return(def);
	    return(new Coord(Integer.parseInt(val.substring(0, x)), Integer.parseInt(val.substring(x + 1))));
	} catch(SecurityException e) {
	    return(def);
	}
    }
    
    static void setprefc(String prefname, Coord val) {
	try {
	    prefs().put(prefname, val.x + "x" + val.y);
	} catch(SecurityException e) {
	}
    }

    static byte[] getprefb(String prefname, byte[] def) {
	try {
	    return(prefs().getByteArray(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }
	
    static void setprefb(String prefname, byte[] val) {
	try {
	    prefs().putByteArray(prefname, val);
	} catch(SecurityException e) {
	}
    }
    
    static float getpreff(String prefname, float def) {
	try {
	    return(prefs().getFloat(prefname, def));
	} catch(SecurityException e) {
	    return(def);
	}
    }
    
    public static String getprop(String propname, String def) {
	try {
	    String ret;
	    if((ret = System.getProperty(propname)) != null)
		return(ret);
	    if((ret = System.getProperty("jnlp." + propname)) != null)
		return(ret);
	    return(def);
	} catch(SecurityException e) {
	    return(def);
	}
    }
	
    public static int ub(byte b) {
	return(((int)b) & 0xff);
    }

    /* Nested format: [[KEY, VALUE], [KEY, VALUE], ...] */
    public static <K, V> Map<K, V> mapdecn(Object ob, Class<K> kt, Class<V> vt) {
	Map<K, V> ret = new HashMap<>();
	Object[] enc = (Object[])ob;
	for(Object sob : enc) {
	    Object[] ent = (Object[])sob;
	    ret.put(kt.cast(ent[0]), vt.cast(ent[1]));
	}
	return(ret);
    }
    public static Map<Object, Object> mapdecn(Object ob) {
	return(mapdecn(ob, Object.class, Object.class));
    }
    public static Object[] mapencn(Map<?, ?> map) {
	Object[] ret = new Object[map.size()];
	int a = 0;
	for(Map.Entry<?, ?> ent : map.entrySet())
	    ret[a++] = new Object[] {ent.getKey(), ent.getValue()};
	return(ret);
    }

    /* Flat format: [KEY, VALUE, KEY, VALUE, ...] */
    public static <K, V> Map<K, V> mapdecf(Object ob, Class<K> kt, Class<V> vt) {
	Map<K, V> ret = new HashMap<>();
	Object[] enc = (Object[])ob;
	for(int a = 0; a < enc.length - 1; a += 2)
	    ret.put(kt.cast(enc[a]), vt.cast(enc[a + 1]));
	return(ret);
    }
    public static Map<Object, Object> mapdecf(Object ob) {
	return(mapdecf(ob, Object.class, Object.class));
    }
    public static Object[] mapencf(Map<?, ?> map) {
	Object[] ret = new Object[map.size() * 2];
	int a = 0;
	for(Map.Entry<?, ?> ent : map.entrySet()) {
	    ret[a + 0] = ent.getKey();
	    ret[a + 1] = ent.getValue();
	    a += 2;
	}
	return(ret);
    }

    public static byte sb(int b) {
	return((byte)b);
    }
	
    public static int uint16d(byte[] buf, int off) {
	return(ub(buf[off]) | (ub(buf[off + 1]) << 8));
    }
	
    public static int int16d(byte[] buf, int off) {
	return((int)(short)uint16d(buf, off));
    }
	
    public static long uint32d(byte[] buf, int off) {
	return((long)ub(buf[off]) | ((long)ub(buf[off + 1]) << 8) | ((long)ub(buf[off + 2]) << 16) | ((long)ub(buf[off + 3]) << 24));
    }
	
    public static void uint32e(long num, byte[] buf, int off) {
	buf[off] = (byte)(num & 0xff);
	buf[off + 1] = (byte)((num & 0x0000ff00) >> 8);
	buf[off + 2] = (byte)((num & 0x00ff0000) >> 16);
	buf[off + 3] = (byte)((num & 0xff000000) >> 24);
    }
	
    public static int int32d(byte[] buf, int off) {
	return((int)uint32d(buf, off));
    }
    
    public static long int64d(byte[] buf, int off) {
	long b = 0;
	for(int i = 0; i < 8; i++)
	    b |= ((long)ub(buf[i])) << (i * 8);
	return(b);
    }
	
    public static void int32e(int num, byte[] buf, int off) {
	uint32e(((long)num) & 0xffffffff, buf, off);
    }
	
    public static void uint16e(int num, byte[] buf, int off) {
	buf[off] = sb(num & 0xff);
	buf[off + 1] = sb((num & 0xff00) >> 8);
    }
	
    public static String strd(byte[] buf, int[] off) {
	int i;
	for(i = off[0]; buf[i] != 0; i++);
	String ret;
	try {
	    ret = new String(buf, off[0], i - off[0], "utf-8");
	} catch(UnsupportedEncodingException e) {
	    throw(new IllegalArgumentException(e));
	}
	off[0] = i + 1;
	return(ret);
    }
    
    public static double floatd(byte[] buf, int off) {
	int e = buf[off];
	long t = uint32d(buf, off + 1);
	int m = (int)(t & 0x7fffffffL);
	boolean s = (t & 0x80000000L) != 0;
	if(e == -128) {
	    if(m == 0)
		return(0.0);
	    throw(new RuntimeException("Invalid special float encoded (" + m + ")"));
	}
	double v = (((double)m) / 2147483648.0) + 1.0;
	if(s)
	    v = -v;
	return(Math.pow(2.0, e) * v);
    }

    public static float float32d(byte[] buf, int off) {
	return(Float.intBitsToFloat(int32d(buf, off)));
    }

    public static double float64d(byte[] buf, int off) {
	return(Double.longBitsToDouble(int64d(buf, off)));
    }

    public static float hfdec(short bits) {
	int b = ((int)bits) & 0xffff;
	int e = (b & 0x7c00) >> 10;
	int m = b & 0x03ff;
	int ee;
	if(e == 0) {
	    if(m == 0) {
		ee = 0;
	    } else {
		int n = Integer.numberOfLeadingZeros(m) - 22;
		ee = (-15 - n) + 127;
		m = (m << (n + 1)) & 0x03ff;
	    }
	} else if(e == 0x1f) {
	    ee = 0xff;
	} else {
	    ee = e - 15 + 127;
	}
	int f32 = ((b & 0x8000) << 16) |
	    (ee << 23) |
	    (m << 13);
	return(Float.intBitsToFloat(f32));
    }

    public static short hfenc(float f) {
	int b = Float.floatToIntBits(f);
	int e = (b & 0x7f800000) >> 23;
	int m = b & 0x007fffff;
	int ee;
	if(e == 0) {
	    ee = 0;
	    m = 0;
	} else if(e == 0xff) {
	    ee = 0x1f;
	} else if(e < 113) {
	    ee = 0;
	    m = (m | 0x00800000) >> (113 - e);
	} else if(e > 142) {
	    return(((b & 0x80000000) == 0)?((short)0x7c00):((short)0xfc00));
	} else {
	    ee = e - 127 + 15;
	}
	int f16 = ((b >> 16) & 0x8000) |
	    (ee << 10) |
	    (m >> 13);
	return((short)f16);
    }

    static char num2hex(int num) {
	if(num < 10)
	    return((char)('0' + num));
	else
	    return((char)('A' + num - 10));
    }
	
    static int hex2num(char hex) {
	if((hex >= '0') && (hex <= '9'))
	    return(hex - '0');
	else if((hex >= 'a') && (hex <= 'f'))
	    return(hex - 'a' + 10);
	else if((hex >= 'A') && (hex <= 'F'))
	    return(hex - 'A' + 10);
	else
	    throw(new IllegalArgumentException());
    }

    static String byte2hex(byte[] in) {
	StringBuilder buf = new StringBuilder();
	for(byte b : in) {
	    buf.append(num2hex((b & 0xf0) >> 4));
	    buf.append(num2hex(b & 0x0f));
	}
	return(buf.toString());
    }

    static byte[] hex2byte(String hex) {
	if(hex.length() % 2 != 0)
	    throw(new IllegalArgumentException("Invalid hex-encoded string"));
	byte[] ret = new byte[hex.length() / 2];
	for(int i = 0, o = 0; i < hex.length(); i += 2, o++)
	    ret[o] = (byte)((hex2num(hex.charAt(i)) << 4) | hex2num(hex.charAt(i + 1)));
	return(ret);
    }
    
    private final static String base64set = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static int[] base64rev;
    static {
	int[] rev = new int[128];
	for(int i = 0; i < 128; rev[i++] = -1);
	for(int i = 0; i < base64set.length(); i++)
	    rev[base64set.charAt(i)] = i;
	base64rev = rev;
    }
    public static String base64enc(byte[] in) {
	StringBuilder buf = new StringBuilder();
	int p = 0;
	while(in.length - p >= 3) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt(((in[p + 0] & 0x03) << 4) | ((in[p + 1] & 0xf0) >> 4)));
	    buf.append(base64set.charAt(((in[p + 1] & 0x0f) << 2) | ((in[p + 2] & 0xc0) >> 6)));
	    buf.append(base64set.charAt(  in[p + 2] & 0x3f));
	    p += 3;
	}
	if(in.length == p + 1) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt( (in[p + 0] & 0x03) << 4));
	    buf.append("==");
	} else if(in.length == p + 2) {
	    buf.append(base64set.charAt( (in[p + 0] & 0xfc) >> 2));
	    buf.append(base64set.charAt(((in[p + 0] & 0x03) << 4) | ((in[p + 1] & 0xf0) >> 4)));
	    buf.append(base64set.charAt( (in[p + 1] & 0x0f) << 2));
	    buf.append("=");
	}
	return(buf.toString());
    }
    public static byte[] base64dec(String in) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	int cur = 0, b = 8;
	for(int i = 0; i < in.length(); i++) {
	    char c = in.charAt(i);
	    if(c >= 128)
		throw(new IllegalArgumentException());
	    if(c == '=')
		break;
	    int d = base64rev[c];
	    if(d == -1)
		throw(new IllegalArgumentException());
	    b -= 6;
	    if(b <= 0) {
		cur |= d >> -b;
		buf.write(cur);
		b += 8;
		cur = 0;
	    }
	    cur |= d << b;
	}
	return(buf.toByteArray());
    }
	
    public static String[] splitwords(String text) {
	ArrayList<String> words = new ArrayList<String>();
	StringBuilder buf = new StringBuilder();
	String st = "ws";
	int i = 0;
	while(i < text.length()) {
	    char c = text.charAt(i);
	    if(st == "ws") {
		if(!Character.isWhitespace(c))
		    st = "word";
		else
		    i++;
	    } else if(st == "word") {
		if(c == '"') {
		    st = "quote";
		    i++;
		} else if(c == '\\') {
		    st = "squote";
		    i++;
		} else if(Character.isWhitespace(c)) {
		    words.add(buf.toString());
		    buf = new StringBuilder();
		    st = "ws";
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "quote") {
		if(c == '"') {
		    st = "word";
		    i++;
		} else if(c == '\\') {
		    st = "sqquote";
		    i++;
		} else {
		    buf.append(c);
		    i++;
		}
	    } else if(st == "squote") {
		buf.append(c);
		i++;
		st = "word";
	    } else if(st == "sqquote") {
		buf.append(c);
		i++;
		st = "quote";
	    }
	}
	if(st == "word")
	    words.add(buf.toString());
	if((st != "ws") && (st != "word"))
	    return(null);
	return(words.toArray(new String[0]));
    }
	
    public static String[] splitlines(String text) {
	ArrayList<String> ret = new ArrayList<String>();
	int p = 0;
	while(true) {
	    int p2 = text.indexOf('\n', p);
	    if(p2 < 0) {
		ret.add(text.substring(p));
		break;
	    }
	    ret.add(text.substring(p, p2));
	    p = p2 + 1;
	}
	return(ret.toArray(new String[0]));
    }

    static int atoi(String a) {
	try {
	    return(Integer.parseInt(a));
	} catch(NumberFormatException e) {
	    return(0);
	}
    }
    
    static void readtileof(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        while(true) {
            if(in.read(buf, 0, buf.length) < 0)
                return;
        }
    }
    
    static byte[] readall(InputStream in) throws IOException {
	byte[] buf = new byte[4096];
	int off = 0;
	while(true) {
	    if(off == buf.length) {
		byte[] n = new byte[buf.length * 2];
		System.arraycopy(buf, 0, n, 0, buf.length);
		buf = n;
	    }
	    int ret = in.read(buf, off, buf.length - off);
	    if(ret < 0) {
		byte[] n = new byte[off];
		System.arraycopy(buf, 0, n, 0, off);
		return(n);
	    }
	    off += ret;
	}
    }
    
    public static interface IOFunction<T> {
	/* Checked exceptions banzai :P */
	public T run() throws IOException;
    }

    /* XXX: Sometimes, the client is getting strange and weird OS
     * errors on Windows. For example, file sharing violations are
     * sometimes returned even though Java always opens
     * RandomAccessFiles in non-exclusive mode, and other times,
     * permission is spuriously denied. I've had zero luck in trying
     * to find a root cause for these errors, so just assume the error
     * is transient and retry. :P */
    public static <T> T ioretry(IOFunction<? extends T> task) throws IOException {
	double[] retimes = {0.01, 0.1, 0.5, 1.0, 5.0};
	Throwable last = null;
	boolean intr = false;
	try {
	    for(int r = 0; true; r++) {
		try {
		    return(task.run());
		} catch(RuntimeException | IOException exc) {
		    if(last == null)
			new Throwable("weird I/O error occurred on " + String.valueOf(task), exc).printStackTrace();
		    if(last != null)
			exc.addSuppressed(last);
		    last = exc;
		    if(r < retimes.length) {
			try {
			    Thread.sleep((long)(retimes[r] * 1000));
			} catch(InterruptedException irq) {
			    Thread.currentThread().interrupted();
			    intr = true;
			}
		    } else {
			throw(exc);
		    }
		}
	    }
	} finally {
	    if(intr)
		Thread.currentThread().interrupt();
	}
    }

    private static void dumptg(ThreadGroup tg, PrintWriter out, int indent) {
	for(int o = 0; o < indent; o++)
	    out.print("    ");
	out.println("G: \"" + tg.getName() + "\"");
	Thread[] ths = new Thread[tg.activeCount() * 2];
	ThreadGroup[] tgs = new ThreadGroup[tg.activeGroupCount() * 2];
	int nt = tg.enumerate(ths, false);
	int ng = tg.enumerate(tgs, false);
	for(int i = 0; i < nt; i++) {
	    Thread ct = ths[i];
	    for(int o = 0; o < indent + 1; o++)
		out.print("    ");
	    out.println("T: \"" + ct.getName() + "\"");
	}
	for(int i = 0; i < ng; i++) {
	    ThreadGroup cg = tgs[i];
	    dumptg(cg, out, indent + 1);
	}
    }

    public static void dumptg(ThreadGroup tg, PrintWriter out) {
	if(tg == null) {
	    tg = Thread.currentThread().getThreadGroup();
	    while(tg.getParent() != null)
		tg = tg.getParent();
	}
	dumptg(tg, out, 0);
	out.flush();
    }

    public static Resource myres(Class<?> c) {
	ClassLoader cl = c.getClassLoader();
	if(cl instanceof Resource.ResClassLoader) {
	    return(((Resource.ResClassLoader)cl).getres());
	} else {
	    return(null);
	}
    }
    
    public static String titlecase(String str) {
	return(Character.toTitleCase(str.charAt(0)) + str.substring(1));
    }
    
    public static Color contrast(Color col) {
	int max = Math.max(col.getRed(), Math.max(col.getGreen(), col.getBlue()));
	if(max > 128) {
	    return(new Color(col.getRed() / 2, col.getGreen() / 2, col.getBlue() / 2, col.getAlpha()));
	} else if(max == 0) {
	    return(Color.WHITE);
	} else {
	    int f = 128 / max;
	    return(new Color(col.getRed() * f, col.getGreen() * f, col.getBlue() * f, col.getAlpha()));
	}
    }

    public static Color clipcol(int r, int g, int b, int a) {
	if(r < 0)   r = 0;
	if(r > 255) r = 255;
	if(g < 0)   g = 0;
	if(g > 255) g = 255;
	if(b < 0)   b = 0;
	if(b > 255) b = 255;
	if(a < 0)   a = 0;
	if(a > 255) a = 255;
	return(new Color(r, g, b, a));
    }

    public static BufferedImage outline(BufferedImage img, Color col) {
	return outline(img, col, false);
    }
    
    public static BufferedImage outline(BufferedImage img, Color col, boolean thick) {
	Coord sz = imgsz(img).add(2, 2);
	BufferedImage ol = TexI.mkbuf(sz);
	Object fcol = ol.getColorModel().getDataElements(col.getRGB(), null);
	Raster src = img.getRaster();
	WritableRaster dst = ol.getRaster();
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		boolean t;
		if((y == 0) || (x == 0) || (y == sz.y - 1) || (x == sz.x - 1)) {
		    t = true;
		} else {
		    t = src.getSample(x - 1, y - 1, 3) < 250;
		}
		if(!t)
		    continue;
		if(((x > 1) && (y > 0) && (y < sz.y - 1) && (src.getSample(x - 2, y - 1, 3) >= 250)) ||
		   ((x > 0) && (y > 1) && (x < sz.x - 1) && (src.getSample(x - 1, y - 2, 3) >= 250)) ||
		   ((x < sz.x - 2) && (y > 0) && (y < sz.y - 1) && (src.getSample(x, y - 1, 3) >= 250)) ||
		   ((x > 0) && (y < sz.y - 2) && (x < sz.x - 1) && (src.getSample(x - 1, y, 3) >= 250)))
		    dst.setDataElements(x, y, fcol);
		if(thick){
		    if(((x > 1) && (y > 1) && (src.getSample(x - 2, y - 2, 3)) >= 250) ||
			    ((x < sz.x - 2) && (y < sz.y - 2) && (src.getSample(x, y, 3) >= 250)) ||
			    ((x < sz.x - 2) && (y > 1) && (src.getSample(x, y - 2, 3) >= 250)) ||
			    ((x > 1) && (y < sz.y - 2) && (src.getSample(x - 2, y, 3) >= 250)))
			dst.setDataElements(x, y, fcol);
		}
	    }
	}
	return(ol);
    }
    
    public static BufferedImage outline2(BufferedImage img, Color col) {
	return outline2(img, col, false);
    }
    
    public static BufferedImage outline2(BufferedImage img, Color col, boolean thick) {
	BufferedImage ol = outline(img, col, thick);
	Graphics g = ol.getGraphics();
	g.drawImage(img, 1, 1, null);
	g.dispose();
	return(ol);
    }

    public static int floordiv(int a, int b) {
	if(a < 0)
	    return(((a + 1) / b) - 1);
	else
	    return(a / b);
    }
    
    public static int floormod(int a, int b) {
	int r = a % b;
	if(r < 0)
	    r += b;
	return(r);
    }

    /* XXX: These are not actually correct, since an exact integer
     * will round downwards, but I don't actually expect that to be a
     * problem given how I use these, and it turns out that
     * java.lang.Math.floor is actually surprisingly slow (it
     * delegates for StrictMath.float for some reason). */
    public static int floordiv(float a, float b) {
	float q = a / b;
	return((q < 0)?(((int)q) - 1):((int)q));
    }
    
    public static float floormod(float a, float b) {
	float r = a % b;
	return((a < 0)?(r + b):r);
    }

    public static double cangle(double a) {
	while(a > Math.PI)
	    a -= Math.PI * 2;
	while(a < -Math.PI)
	    a += Math.PI * 2;
	return(a);
    }

    public static double cangle2(double a) {
	while(a > Math.PI * 2)
	    a -= Math.PI * 2;
	while(a < 0)
	    a += Math.PI * 2;
	return(a);
    }

    public static double clip(double d, double min, double max) {
	if(d < min)
	    return(min);
	if(d > max)
	    return(max);
	return(d);
    }
    
    public static float clip(float d, float min, float max) {
	if(d < min)
	    return(min);
	if(d > max)
	    return(max);
	return(d);
    }
    
    public static int clip(int i, int min, int max) {
	if(i < min)
	    return(min);
	if(i > max)
	    return(max);
	return(i);
    }
    
    public static Color blendcol(Color in, Color bl) {
	int f1 = bl.getAlpha();
	int f2 = 255 - bl.getAlpha();
	return(new Color(((in.getRed() * f2) + (bl.getRed() * f1)) / 255,
			 ((in.getGreen() * f2) + (bl.getGreen() * f1)) / 255,
			 ((in.getBlue() * f2) + (bl.getBlue() * f1)) / 255,
			 in.getAlpha()));
    }
    
    public static void serialize(Object obj, OutputStream out) throws IOException {
	ObjectOutputStream oout = new ObjectOutputStream(out);
	oout.writeObject(obj);
	oout.flush();
    }
    
    public static byte[] serialize(Object obj) {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    serialize(obj, out);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	return(out.toByteArray());
    }
    
    public static Object deserialize(InputStream in) throws IOException {
	ObjectInputStream oin = new ObjectInputStream(in);
	try {
	    return(oin.readObject());
	} catch(ClassNotFoundException e) {
	    return(null);
	}
    }
    
    public static Object deserialize(byte[] buf) {
	if(buf == null)
	    return(null);
	InputStream in = new ByteArrayInputStream(buf);
	try {
	    return(deserialize(in));
	} catch(IOException e) {
	    return(null);
	}
    }
    
    public static boolean parsebool(String s) {
	if(s == null)
	    throw(new IllegalArgumentException(s));
	else if(s.equalsIgnoreCase("1") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"))
	    return(true);
	else if(s.equalsIgnoreCase("0") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no"))
	    return(false);
	throw(new IllegalArgumentException(s));
    }

    public static boolean eq(Object a, Object b) {
	return(((a == null) && (b == null)) ||
	       ((a != null) && (b != null) && a.equals(b)));
    }

    public static boolean parsebool(String s, boolean def) {
	try {
	    return(parsebool(s));
	} catch(IllegalArgumentException e) {
	    return(def);
	}
    }
    
    /* Just in case anyone doubted that Java is stupid. :-/ */
    public static FloatBuffer bufcp(float[] a) {
	FloatBuffer b = mkfbuf(a.length);
	b.put(a);
	b.rewind();
	return(b);
    }
    public static ShortBuffer bufcp(short[] a) {
	ShortBuffer b = mksbuf(a.length);
	b.put(a);
	b.rewind();
	return(b);
    }
    public static FloatBuffer bufcp(FloatBuffer a) {
	a.rewind();
	FloatBuffer ret = mkfbuf(a.remaining());
	ret.put(a).rewind();
	return(ret);
    }
    public static IntBuffer bufcp(IntBuffer a) {
	a.rewind();
	IntBuffer ret = mkibuf(a.remaining());
	ret.put(a).rewind();
	return(ret);
    }
    public static ByteBuffer mkbbuf(int n) {
	try {
	    return(ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder()));
	} catch(OutOfMemoryError e) {
	    /* At least Sun's class library doesn't try to collect
	     * garbage if it's out of direct memory, which is pretty
	     * stupid. So do it for it, then. */
	    System.gc();
	    return(ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder()));
	}
    }
    public static FloatBuffer mkfbuf(int n) {
	return(mkbbuf(n * 4).asFloatBuffer());
    }
    public static ShortBuffer mksbuf(int n) {
	return(mkbbuf(n * 2).asShortBuffer());
    }
    public static IntBuffer mkibuf(int n) {
	return(mkbbuf(n * 4).asIntBuffer());
    }

    /*
    public static ByteBuffer wbbuf(int n) {
	return(mkbbuf(n));
    }
    public static IntBuffer wibuf(int n) {
	return(mkibuf(n));
    }
    public static FloatBuffer wfbuf(int n) {
	return(mkfbuf(n));
    }
    public static ShortBuffer wsbuf(int n) {
	return(mksbuf(n));
    }
    */
    public static ByteBuffer wbbuf(int n) {
	return(ByteBuffer.wrap(new byte[n]));
    }
    public static IntBuffer wibuf(int n) {
	return(IntBuffer.wrap(new int[n]));
    }
    public static FloatBuffer wfbuf(int n) {
	return(FloatBuffer.wrap(new float[n]));
    }
    public static ShortBuffer wsbuf(int n) {
	return(ShortBuffer.wrap(new short[n]));
    }

    public static float[] c2fa(Color c) {
	return(new float[] {
		((float)c.getRed() / 255.0f),
		((float)c.getGreen() / 255.0f),
		((float)c.getBlue() / 255.0f),
		((float)c.getAlpha() / 255.0f)
	    });
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] mkarray(Class<T> cl, int len) {
	return((T[])Array.newInstance(cl, len));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] splice(T[] src, int off, int len) {
	T[] dst = (T[])Array.newInstance(src.getClass().getComponentType(), len);
	System.arraycopy(src, off, dst, 0, len);
	return(dst);
    }
    
    public static void rgb2hsl(int r, int g, int b, int hsl[]) {

	float var_R = ( r / 255f );
	float var_G = ( g / 255f );
	float var_B = ( b / 255f );

	float var_Min;    //Min. value of RGB
	float var_Max;    //Max. value of RGB
	float del_Max;    //Delta RGB value

	if (var_R > var_G) 
	{ var_Min = var_G; var_Max = var_R; }
	else 
	{ var_Min = var_R; var_Max = var_G; }

	if (var_B > var_Max) var_Max = var_B;
	if (var_B < var_Min) var_Min = var_B;

	del_Max = var_Max - var_Min; 

	float H = 0, S, L;
	L = ( var_Max + var_Min ) / 2f;

	if ( del_Max == 0 ) { H = 0; S = 0; } // gray
	else {                                //Chroma
	    if ( L < 0.5 ) 
		S = del_Max / ( var_Max + var_Min );
	    else           
		S = del_Max / ( 2 - var_Max - var_Min );

	    float del_R = ( ( ( var_Max - var_R ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
	    float del_G = ( ( ( var_Max - var_G ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
	    float del_B = ( ( ( var_Max - var_B ) / 6f ) + ( del_Max / 2f ) ) / del_Max;

	    if ( var_R == var_Max ) 
		H = del_B - del_G;
	    else if ( var_G == var_Max ) 
		H = ( 1 / 3f ) + del_R - del_B;
	    else if ( var_B == var_Max ) 
		H = ( 2 / 3f ) + del_G - del_R;
	    if ( H < 0 ) H += 1;
	    if ( H > 1 ) H -= 1;
	}
	hsl[0] = (int)(360*H);
	hsl[1] = (int)(S*100);
	hsl[2] = (int)(L*100);
    }
    
    public static int[] hsl2rgb(final int[] hsl) {
	double h = hsl[0] / 360d;
	final double s = hsl[1] / 100d;
	double l = hsl[2] / 100d;
	double r = 0d;
	double g = 0d;
	double b;

	if (s > 0d) {
	    if (h >= 1d) {
		h = 0d;
	    }

	    h = h * 6d;
	    final double f = h - Math.floor(h);
	    final double a = Math.round(l * 255d * (1d - s));
	    b = Math.round(l * 255d * (1d - (s * f)));
	    final double c = Math.round(l * 255d * (1d - (s * (1d - f))));
	    l = Math.round(l * 255d);

	    switch ((int) Math.floor(h)) {
		case 0:
		    r = l;
		    g = c;
		    b = a;
		    break;
		case 1:
		    r = b;
		    g = l;
		    b = a;
		    break;
		case 2:
		    r = a;
		    g = l;
		    b = c;
		    break;
		case 3:
		    r = a;
		    g = b;
		    b = l;
		    break;
		case 4:
		    r = c;
		    g = a;
		    b = l;
		    break;
		case 5:
		    r = l;
		    g = a;
		    break;
	    }
	    return new int[] { (int) Math.round(r), (int) Math.round(g), (int) Math.round(b) };
	}

	l = Math.round(l * 255d);
	return new int[] { (int) l, (int) l, (int) l };
    }
    
    public static <T> T[] splice(T[] src, int off) {
	return(splice(src, off, src.length - off));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] extend(T[] src, int off, int nl) {
	T[] dst = (T[])Array.newInstance(src.getClass().getComponentType(), nl);
	System.arraycopy(src, off, dst, 0, Math.min(src.length - off, dst.length));
	return(dst);
    }

    public static <T> T[] extend(T[] src, int nl) {
	return(extend(src, 0, nl));
    }
    
    public static <T> T el(Iterable<T> c) {
	return(c.iterator().next());
    }
    
    public static <T> T construct(Constructor<T> cons, Object... args) {
	try {
	    return(cons.newInstance(args));
	} catch(InstantiationException e) {
	    throw(new RuntimeException(e));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	} catch(InvocationTargetException e) {
	    if(e.getCause() instanceof RuntimeException)
		throw((RuntimeException)e.getCause());
	    throw(new RuntimeException(e.getCause()));
	}
    }

    public static String urlencode(String in) {
	StringBuilder buf = new StringBuilder();
	byte[] enc;
	try {
	    enc = in.getBytes("utf-8");
	} catch(java.io.UnsupportedEncodingException e) {
	    /* ¦] */
	    throw(new Error(e));
	}
	for(byte c : enc) {
	    if(((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
	       ((c >= '0') && (c <= '9')) || (c == '.')) {
		buf.append((char)c);
	    } else {
		buf.append("%" + Utils.num2hex((c & 0xf0) >> 4) + Utils.num2hex(c & 0x0f));
	    }
	}
	return(buf.toString());
    }

    public static URL urlparam(URL base, String... pars) {
	/* Why is Java so horribly bad? */
	String file = base.getFile();
	int p = file.indexOf('?');
	StringBuilder buf = new StringBuilder();
	if(p >= 0) {
	    /* For now, only add; don't augment. Since Java sucks. */
	    buf.append('&');
	} else {
	    buf.append('?');
	}
	for(int i = 0; i < pars.length; i += 2) {
	    if(i > 0)
		buf.append('&');
	    buf.append(urlencode(pars[i]));
	    buf.append('=');
	    buf.append(urlencode(pars[i + 1]));
	}
	try {
	    return(new URL(base.getProtocol(), base.getHost(), base.getPort(), file + buf.toString()));
	} catch(java.net.MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static Inet4Address in4_pton(CharSequence as) {
	int dbuf = -1, o = 0;
	byte[] abuf = new byte[4];
	for(int i = 0; i < as.length(); i++) {
	    char c = as.charAt(i);
	    if((c >= '0') && (c <= '9')) {
		dbuf = (((dbuf < 0) ? 0 : dbuf) * 10) + (c - '0');
		if(dbuf >= 256)
		    throw(new IllegalArgumentException("illegal octet"));
	    } else if(c == '.') {
		if(dbuf < 0)
		    throw(new IllegalArgumentException("dot without preceding octet"));
		if(o >= 3)
		    throw(new IllegalArgumentException("too many address octets"));
		abuf[o++] = (byte)dbuf;
		dbuf = -1;
	    } else {
		throw(new IllegalArgumentException("illegal address character"));
	    }
	}
	if(dbuf < 0)
	    throw(new IllegalArgumentException("end without preceding octet"));
	if(o != 3)
	    throw(new IllegalArgumentException("too few address octets"));
	abuf[o++] = (byte)dbuf;
	try {
	    return((Inet4Address)InetAddress.getByAddress(abuf));
	} catch(UnknownHostException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static InetAddress in6_pton(CharSequence as) {
	int hbuf = -1, dbuf = -1, p = 0, v4map = -1;
	int[] o = {0, 0};
	byte[][] abuf = {new byte[16], new byte[16]};
	String scope = null;
	for(int i = 0; i < as.length(); i++) {
	    char c = as.charAt(i);
	    int dv = -1;
	    if((c >= '0') && (c <= '9'))
		dv = c - '0';
	    else if((c >= 'A') && (c <= 'F'))
		dv = c + 10 - 'A';
	    else if((c >= 'a') && (c <= 'f'))
		dv = c + 10 - 'a';
	    if(dv >= 0) {
		if(hbuf < 0)
		    hbuf = dbuf = 0;
		hbuf = (hbuf * 16) + dv;
		if(hbuf >= 65536)
		    throw(new IllegalArgumentException("illegal address number"));
		if(dbuf >= 0)
		    dbuf = (dv >= 10) ? -1 : ((dbuf * 10) + dv);
		if(dbuf >= 256)
		    dbuf = -1;
	    } else if(c == ':') {
		if(v4map >= 0)
		    throw(new IllegalArgumentException("illegal embedded v4 address"));
		if(hbuf < 0) {
		    if(p == 0) {
			if(o[p] == 0) {
			    if((i < as.length() - 1) && (as.charAt(i + 1) == ':')) {
				p = 1;
				i++;
			    } else {
				throw(new IllegalArgumentException("colon without preceeding address number"));
			    }
			} else {
			    p = 1;
			}
		    } else {
			throw(new IllegalArgumentException("duplicate zero-string"));
		    }
		} else {
		    if(o[p] >= 14)
			throw(new IllegalArgumentException("too many address numbers"));
		    abuf[p][o[p]++] = (byte)((hbuf & 0xff00) >> 8);
		    abuf[p][o[p]++] = (byte) (hbuf & 0x00ff);
		    hbuf = -1;
		}
	    } else if(c == '.') {
		if((hbuf < 0) || (dbuf < 0))
		    throw(new IllegalArgumentException("illegal embedded v4 octet"));
		if((p == 0) && (o[p] == 0))
		    throw(new IllegalArgumentException("embedded v4 at start of address"));
		if(v4map++ >= 2)
		    throw(new IllegalArgumentException("too many embedded v4 octets"));
		if(o[p] >= 15)
		    throw(new IllegalArgumentException("too many address numbers"));
		abuf[p][o[p]++] = (byte)dbuf;
		hbuf = -1;
	    } else if(c == '%') {
		scope = as.subSequence(i + 1, as.length()).toString();
		break;
	    } else {
		throw(new IllegalArgumentException("illegal address character"));
	    }
	}
	if(hbuf < 0) {
	    if((p < 1) || (o[p] > 0))
		throw(new IllegalArgumentException("unterminated address"));
	} else {
	    if(v4map < 0) {
		if(o[p] >= 15)
		    throw(new IllegalArgumentException("too many address numbers"));
		abuf[p][o[p]++] = (byte)((hbuf & 0xff00) >> 8);
		abuf[p][o[p]++] = (byte) (hbuf & 0x00ff);
	    } else {
		if(dbuf < 0)
		    throw(new IllegalArgumentException("illegal embedded v4 octet"));
		if(v4map != 2)
		    throw(new IllegalArgumentException("too few embedded v4 octets"));
		if(o[p] >= 16)
		    throw(new IllegalArgumentException("too many address numbers"));
		abuf[p][o[p]++] = (byte)dbuf;
	    }
	}
	byte[] fbuf;
	if(p == 0) {
	    if(o[0] != 16)
		throw(new IllegalArgumentException("too few address numbers"));
	    fbuf = abuf[0];
	} else {
	    if((o[0] + o[1]) >= 16)
		throw(new IllegalArgumentException("illegal zero-string"));
	    fbuf = new byte[16];
	    System.arraycopy(abuf[0], 0, fbuf, 0, o[0]);
	    System.arraycopy(abuf[1], 0, fbuf, 16 - o[1], o[1]);
	}
	try {
	    if(scope == null)
		return(InetAddress.getByAddress(fbuf));
	    try {
		return(Inet6Address.getByAddress(null, fbuf, Integer.parseInt(scope)));
	    } catch(NumberFormatException e) {
		try {
		    NetworkInterface iface = NetworkInterface.getByName(scope);
		    if(iface == null)
			throw(new IllegalArgumentException("could not resolve scoped interface: " + scope));
		    return(Inet6Address.getByAddress(null, fbuf, iface));
		} catch(SocketException e2) {
		    throw(new IllegalArgumentException("could not resolve scoped interface: " + scope, e));
		}
	    }
	} catch(UnknownHostException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static InetAddress inet_pton(CharSequence as) {
	try {
	    return(in4_pton(as));
	} catch(IllegalArgumentException e) {
	    try {
		return(in6_pton(as));
	    } catch(IllegalArgumentException e2) {
		e2.addSuppressed(e);
		throw(e2);
	    }
	}
    }

    static {
	Console.setscmd("die", new Console.Command() {
		public void run(Console cons, String[] args) {
		    throw(new Error("Triggered death"));
		}
	    });
	Console.setscmd("threads", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Utils.dumptg(null, cons.out);
		}
	    });
	Console.setscmd("gc", new Console.Command() {
		public void run(Console cons, String[] args) {
		    System.gc();
		}
	    });
	/*
	Console.setscmd("script", new Console.Command() {
		public void run(Console cons, String[] args) throws IOException {
		    haven.test.ScriptDebug.start(args[1], Integer.parseInt(args[2]), true);
		}
	    });
	*/
	Console.setscmd("cscript", new Console.Command() {
		public void run(Console cons, String[] args) throws IOException {
		    haven.test.ScriptDebug.connect(args[1], Config.defserv, Integer.parseInt(args[2]));
		}
	    });
    }
    
    public static String timestamp() {
	return new SimpleDateFormat("HH:mm").format(new Date());
    }
    
    public static String timestamp(String text) {
	return String.format("[%s] %s", timestamp(), text);
    }

    public static String stream2str(InputStream is) {
	Scanner s = new Scanner(is).useDelimiter("\\A");
	return s.hasNext() ? s.next() : "";
    }

    public static Color hex2color(String hex, Color def){
	Color c = def;
	if (hex != null) {
	    try {
		int col = (int) Long.parseLong(hex, 16);
		boolean hasAlpha = (0xff000000 & col) != 0;
		c = new Color(col, hasAlpha);
	    } catch (Exception ignored) {}
	}
	return c;
    }

    public static String color2hex(Color col){
	if(col != null){
	    return Integer.toHexString(col.getRGB());
	}
	return null;
    }
}
