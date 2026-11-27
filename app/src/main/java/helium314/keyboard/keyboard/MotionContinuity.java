// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;
/** Bounded, finite mathematics for keyboard-owned presentation. */
public final class MotionContinuity {
    private MotionContinuity() {}
    public static float clamp(float p) { return Math.max(0f,Math.min(1f,p)); }
    public static float smooth(float p) { p=clamp(p); return p*p*(3f-2f*p); }
    public static float lerp(float a,float b,float p) { return a+(b-a)*clamp(p); }
    public static float taper(float tip,float tail,float age) { return lerp(tip,tail,smooth(age)); }
    public static float position(float from,float to,float velocity,float p,long durationMs) {
        p=clamp(p);
        if (p==0f) return from;
        if (p==1f) return to;
        final float distance=to-from, limit=Math.abs(distance);
        if (limit < .001f) return to;
        final float momentum=Float.isFinite(velocity) ? velocity*durationMs/1000f : 0f;
        final float bound=momentum*distance >= 0f ? 3f*limit : .30f*limit;
        final float tangent=Math.max(-bound,Math.min(bound,momentum));
        return lerp(from,to,smooth(p))+tangent*p*(1f-p)*(1f-p);
    }
    public static float slotOffset(float from,float to,float width) {
        final float bound=Math.max(0f,width*.16f);
        return Math.max(-bound,Math.min(bound,from-to));
    }
}
