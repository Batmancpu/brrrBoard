// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import android.graphics.Rect;
import helium314.keyboard.latin.brain.AdaptiveTouchModel;
import java.util.WeakHashMap;

/** Actual alphabet geometry, isolated by layout and size; no editor metadata. */
public final class AdaptiveTouchGeometry {
    private static final WeakHashMap<Keyboard, AdaptiveTouchGeometry> cache = new WeakHashMap<>();
    private final int[] codes = new int[26], groups = new int[26];
    private final double[] cx = new double[26], cy = new double[26], w = new double[26], h = new double[26];
    public final long signature;
    private AdaptiveTouchGeometry(Keyboard keyboard) {
        double left = Double.MAX_VALUE, top = Double.MAX_VALUE, right = -Double.MAX_VALUE, bottom = -Double.MAX_VALUE;
        long hash = 0xcbf29ce484222325L;
        for (Key k : keyboard.getSortedKeys()) {
            int i = index(k.getCode());
            if (i < 0 || !k.isEnabled()) continue;
            if (codes[i] != 0) throw new IllegalArgumentException("duplicate");
            Rect r = k.getHitBox();
            if (r.width() <= 0 || r.height() <= 0) throw new IllegalArgumentException("empty");
            codes[i] = k.getCode(); cx[i] = r.exactCenterX(); cy[i] = r.exactCenterY();
            w[i] = r.width(); h[i] = r.height();
            left = Math.min(left, r.left); top = Math.min(top, r.top);
            right = Math.max(right, r.right); bottom = Math.max(bottom, r.bottom);
        }
        for (int i=0; i<26; i++) {
            if (codes[i] == 0) throw new IllegalArgumentException("incomplete alphabet");
            groups[i] = AdaptiveTouchModel.cluster((cx[i]-left)/(right-left), (cy[i]-top)/(bottom-top));
            for (double v : new double[]{cx[i], cy[i], w[i], h[i]})
                hash = (hash ^ Double.doubleToLongBits(v)) * 0x100000001b3L;
        }
        signature = hash;
    }
    public static int index(int code) {
        if (code >= 'A' && code <= 'Z') return code - 'A';
        return code >= 'a' && code <= 'z' ? code - 'a' : -1;
    }
    public static AdaptiveTouchGeometry of(Keyboard k) {
        if (k == null || !k.mId.isAlphabetKeyboard() || k.mId.mInternalAction != null) return null;
        if (!cache.containsKey(k)) {
            try { cache.put(k, new AdaptiveTouchGeometry(k)); }
            catch (IllegalArgumentException e) { cache.put(k, null); }
        }
        return cache.get(k);
    }
    private double rx(int i,int x) { return (x-cx[i])/w[i]; }
    private double ry(int i,int y) { return (y-cy[i])/h[i]; }
    private boolean adjacent(int i,int j) {
        return Math.abs(cx[i]-cx[j]) <= 1.5*Math.max(w[i],w[j])
            && Math.abs(cy[i]-cy[j]) <= 1.5*Math.max(h[i],h[j]);
    }
    public int choose(AdaptiveTouchModel.Snapshot s,int literal,int x,int y) {
        int i=index(literal);
        if (s==null || i<0 || !s.ready(groups[i])) return literal;
        double a=rx(i,x), b=ry(i,y);
        int best=i; double cost=s.cost(groups[i],a,b);
        for(int j=0;j<26;j++) if(j!=i && adjacent(i,j)) {
            double u=rx(j,x),v=ry(j,y);
            if(s.preferNeighbor(groups[i],a,b,groups[j],u,v) && s.cost(groups[j],u,v)<cost) {
                best=j; cost=s.cost(groups[j],u,v);
            }
        }
        return best==i ? literal : codes[best];
    }
    /** Accept only aligned, explicitly selected short words; discard ambiguous edits entirely. */
    public boolean aggregate(String typed,String selected,int[] xs,int[] ys,int[] n,double[] sx,double[] sy) {
        int len=typed.length(), differences=0;
        if(len<2 || len>32 || selected.length()!=len || xs.length<len || ys.length<len) return false;
        for(int p=0;p<len;p++) {
            int i=index(typed.charAt(p)),j=index(selected.charAt(p));
            if(i<0 || j<0 || (i!=j && (++differences>1 || !adjacent(i,j)))) return false;
            double x=rx(j,xs[p]),y=ry(j,ys[p]);
            if(Math.abs(x)>.8 || Math.abs(y)>.8) return false;
            int group=groups[j]; n[group]++; sx[group]+=x; sy[group]+=y;
        }
        return true;
    }
}
