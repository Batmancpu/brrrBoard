// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/** RAM-only, exact-range receipts. Never updates timestamps on recall. */
final class CorrectionSessionMemory {
    static final long TTL = 30_000L;
    private static final int MAX_RECORDS = 8, MAX_WORD = 64;
    private final ArrayList<Receipt> records = new ArrayList<>(MAX_RECORDS);
    static final class Receipt {
        final int start, end;
        final long created, epoch;
        final Object connection;
        private final char[] original, corrected;
        Receipt(String original, String corrected, int start, long now, long epoch, Object connection) {
            this.start = start; this.end = start + corrected.length();
            created = now; this.epoch = epoch; this.connection = connection;
            this.original = original.toCharArray(); this.corrected = corrected.toCharArray();
        }
        String original() { return new String(original); }
        String corrected() { return new String(corrected); }
        boolean correctedEquals(CharSequence text) { return same(corrected, text); }
        boolean originalEquals(CharSequence text) { return same(original, text); }
        private static boolean same(char[] chars, CharSequence text) {
            if (text == null || chars.length != text.length()) return false;
            for (int i=0;i<chars.length;i++) if(chars[i]!=text.charAt(i)) return false;
            return true;
        }
        boolean valid(long now, long editorEpoch, Object currentConnection) {
            return connection != null && connection == currentConnection && epoch == editorEpoch
                    && now >= created && now - created < TTL;
        }
        void erase() { Arrays.fill(original,'\0'); Arrays.fill(corrected,'\0'); }
    }
    static boolean isWord(String text) {
        if (text == null || text.isEmpty() || text.length() > MAX_WORD) return false;
        boolean letter = false;
        for (int i=0;i<text.length();) {
            int cp=text.codePointAt(i); i+=Character.charCount(cp);
            if (Character.isLetter(cp)) letter = true;
            else if (cp!='\'' && cp!=0x2019 && Character.getType(cp)!=Character.NON_SPACING_MARK
                    && Character.getType(cp)!=Character.COMBINING_SPACING_MARK) return false;
        }
        return letter;
    }
    boolean remember(String original, String corrected, int start, long now, long epoch, Object connection) {
        if (!isWord(original) || !isWord(corrected) || original.equals(corrected)
                || start<0 || start>Integer.MAX_VALUE-corrected.length() || connection==null) return false;
        prune(now,epoch,connection);
        invalidateFrom(start);
        if (records.size() == MAX_RECORDS) records.remove(0).erase();
        records.add(new Receipt(original,corrected,start,now,epoch,connection));
        return true;
    }
    Receipt find(int start,int end,CharSequence liveWord,long now,long epoch,Object connection) {
        prune(now,epoch,connection);
        for (Receipt r:records) if(r.start==start && r.end==end && r.correctedEquals(liveWord)) return r;
        return null;
    }
    boolean contains(Receipt receipt,long now,long epoch,Object connection) {
        prune(now,epoch,connection);
        return receipt!=null && records.contains(receipt);
    }
    void invalidateFrom(int editStart) {
        Iterator<Receipt> it=records.iterator();
        while(it.hasNext()) { Receipt r=it.next(); if(editStart<r.end) { r.erase();it.remove(); } }
    }
    void prune(long now,long epoch,Object connection) {
        Iterator<Receipt> it=records.iterator();
        while(it.hasNext()) { Receipt r=it.next(); if(!r.valid(now,epoch,connection)){r.erase();it.remove();} }
    }
    long nextExpiry(long now) {
        long delay=Long.MAX_VALUE;
        for(Receipt r:records) delay=Math.min(delay,Math.max(0,TTL-(now-r.created)));
        return delay;
    }
    boolean isEmpty() { return records.isEmpty(); }
    void clear() { for(Receipt r:records) r.erase(); records.clear(); }
}
