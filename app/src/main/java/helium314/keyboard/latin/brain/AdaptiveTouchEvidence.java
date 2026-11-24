// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/** Unordered per-letter aggregate, independent of words, timing and editor identity. */
public final class AdaptiveTouchEvidence {
    public static final int LETTERS = 26, SIDE = 5, CELLS = SIDE * SIDE;
    private static final int CAP = 255;
    private final int[][] bins = new int[LETTERS][CELLS];
    private final int[] wrong = new int[LETTERS], total = new int[LETTERS], group = new int[LETTERS];
    public AdaptiveTouchEvidence() { Arrays.fill(group,-1); }

    public static int cell(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || Math.abs(x) > .8 || Math.abs(y) > .8) return -1;
        return Math.min(SIDE-1,(int)((y+.8)/1.6*SIDE))*SIDE
                + Math.min(SIDE-1,(int)((x+.8)/1.6*SIDE));
    }
    public boolean observe(int intended,int region,double x,double y,boolean mismatch) {
        int bin = cell(x,y);
        if (intended < 0 || intended >= LETTERS || region < 0 || region >= AdaptiveTouchModel.CLUSTERS
                || bin < 0 || (group[intended] >= 0 && group[intended] != region)) return false;
        group[intended] = region;
        if (total[intended] >= CAP) halve(intended);
        bins[intended][bin]++;
        total[intended]++;
        if (mismatch) wrong[intended]++;
        return true;
    }
    private void halve(int letter) {
        int old = total[letter], next = 0;
        for (int i=0;i<CELLS;i++) { bins[letter][i] /= 2; next += bins[letter][i]; }
        wrong[letter] = old == 0 ? 0 : (int)((long)wrong[letter]*next/old);
        total[letter] = next;
    }
    public boolean compatible(AdaptiveTouchEvidence batch) {
        for(int k=0;k<LETTERS;k++) if(batch.total[k]>0 && group[k]>=0 && group[k]!=batch.group[k]) return false;
        return true;
    }
    public void merge(AdaptiveTouchEvidence batch) {
        if(!compatible(batch)) throw new IllegalArgumentException("geometry");
        for (int k=0;k<LETTERS;k++) {
            int n=batch.total[k];
            if(n==0) continue;
            group[k]=batch.group[k];
            while(total[k]+n>CAP) halve(k);
            for(int i=0;i<CELLS;i++) bins[k][i]+=batch.bins[k][i];
            total[k]+=n; wrong[k]+=batch.wrong[k];
        }
    }
    public AdaptiveTouchEvidence copy() {
        AdaptiveTouchEvidence out=new AdaptiveTouchEvidence();
        for(int k=0;k<LETTERS;k++) {
            System.arraycopy(bins[k],0,out.bins[k],0,CELLS);
            out.total[k]=total[k]; out.wrong[k]=wrong[k]; out.group[k]=group[k];
        }
        return out;
    }
    public int count(int letter) { return total[letter]; }
    public int mismatch(int letter) { return wrong[letter]; }
    public int region(int letter) { return group[letter]; }
    public int countAt(int letter,int cell) { return bins[letter][cell]; }

    public void writeTo(DataOutput out) throws IOException {
        for(int k=0;k<LETTERS;k++) {
            out.writeByte(group[k]+1); out.writeByte(wrong[k]);
            for(int n:bins[k]) out.writeByte(n);
        }
    }
    public static AdaptiveTouchEvidence readFrom(DataInput in) throws IOException {
        AdaptiveTouchEvidence out=new AdaptiveTouchEvidence();
        for(int k=0;k<LETTERS;k++) {
            int region=in.readUnsignedByte()-1, wrong=in.readUnsignedByte(), total=0;
            for(int i=0;i<CELLS;i++) { int n=in.readUnsignedByte(); out.bins[k][i]=n; total+=n; }
            if(region < -1 || region >= AdaptiveTouchModel.CLUSTERS || total>CAP || wrong>total
                    || (total>0 && region<0)) throw new IOException("invalid touch evidence");
            out.group[k]=region; out.wrong[k]=wrong; out.total[k]=total;
        }
        return out;
    }
}
