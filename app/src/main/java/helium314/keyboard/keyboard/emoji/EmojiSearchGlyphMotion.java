// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.MotionContinuity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Match complete public result glyphs, including skin tones and ZWJ sequences. */
final class EmojiSearchGlyphMotion {
    static final long DURATION_MS=145L;
    private static final int MAX=24;
    static final class Pose {
        final Key key;
        final float x,scale,alpha;
        Pose(Key k,float x,float scale,float alpha) { key=k;this.x=x;this.scale=scale;this.alpha=alpha; }
    }
    static final class Track {
        final float offset,scale,alpha;
        final Key departing;
        final float departingScale;
        Track(float offset,float scale,float alpha,Key departing,float departingScale) {
            this.offset=offset;this.scale=scale;this.alpha=alpha;
            this.departing=departing;this.departingScale=departingScale;
        }
    }
    private final Map<String,Pose> prior=new HashMap<>();
    private final Map<String,Track> tracks=new HashMap<>();
    private long started;
    boolean active() { return !tracks.isEmpty(); }
    float progress(long now) { return MotionContinuity.clamp((now-started)/(float)DURATION_MS); }
    Track track(String label) { return label==null?null:tracks.get(label); }
    boolean clear() { boolean had=active()||!prior.isEmpty();tracks.clear();prior.clear();return had; }
    void capture(Keyboard keyboard,float drawn) {
        prior.clear();
        if (keyboard!=null) {
            float p=MotionContinuity.smooth(drawn);
            for (Key key:keyboard.getSortedKeys()) {
                String label=key.getLabel();
                if (label==null||label.isEmpty()||prior.size()>=MAX) continue;
                Track t=track(label);
                prior.put(label,new Pose(key,key.getDrawX()+(t==null?0f:t.offset*(1f-p)),
                        t==null?1f:MotionContinuity.lerp(t.scale,1f,p),
                        t==null?1f:MotionContinuity.lerp(t.alpha,1f,p)));
            }
        }
        tracks.clear();
    }
    void retarget(Keyboard keyboard,long now) {
        tracks.clear();
        started=now;
        if (keyboard==null) { prior.clear();return; }
        HashSet<String> next=new HashSet<>();
        for(Key key:keyboard.getSortedKeys()) if(key.getLabel()!=null) next.add(key.getLabel());
        for(Key key:keyboard.getSortedKeys()) {
            String label=key.getLabel();
            if(label==null||label.isEmpty()||tracks.size()>=MAX) continue;
            Pose pose=prior.get(label);
            float x=key.getDrawX();
            float offset=pose==null?0f:MotionContinuity.slotOffset(pose.x,x,key.getDrawWidth());
            float scale=pose==null?.55f:pose.scale, alpha=pose==null?.76f:pose.alpha;
            Key departing=null;
            float departingScale=1f;
            if(pose==null) for(Pose old:prior.values()) {
                if(!next.contains(old.key.getLabel()) && old.key.getDrawX()==key.getDrawX()
                        && old.key.getY()==key.getY() && old.key.getDrawWidth()==key.getDrawWidth()
                        && old.key.getHeight()==key.getHeight()) {
                    departing=old.key;departingScale=old.scale;break;
                }
            }
            if(Math.abs(offset)>.01f||scale<.999f||alpha<.999f||departing!=null)
                tracks.put(label,new Track(offset,scale,alpha,departing,departingScale));
        }
        prior.clear();
    }
}
