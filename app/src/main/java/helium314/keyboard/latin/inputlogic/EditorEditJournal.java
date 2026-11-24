// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;
import java.util.ArrayList;
import java.util.Arrays;

/** Bounded RAM-only journal for verified explicit keyboard edits, not typing history. */
final class EditorEditJournal {
    static final long TTL = 30_000;
    static final int CONTEXT = 8;
    private static final int LIMIT=8, CHARS=4096;
    private final ArrayList<Entry> entries=new ArrayList<>();
    private int cursor;
    static final class Entry {
        final int start;
        final long created, epoch;
        final Object connection;
        private final char[] before, after, prefix, suffix;
        Entry(int start,String before,String after,String prefix,String suffix,long now,long epoch,Object c){
            this.start=start;created=now;this.epoch=epoch;connection=c;
            this.before=before.toCharArray();this.after=after.toCharArray();
            this.prefix=prefix.toCharArray();this.suffix=suffix.toCharArray();
        }
        String before(){return new String(before);}
        String after(){return new String(after);}
        String prefix(){return new String(prefix);}
        String suffix(){return new String(suffix);}
        int size(){return before.length+after.length+prefix.length+suffix.length;}
        boolean valid(long now,long epoch,Object c){return c!=null&&c==connection&&epoch==this.epoch&&now>=created&&now-created<TTL;}
        void erase(){Arrays.fill(before,'\0');Arrays.fill(after,'\0');Arrays.fill(prefix,'\0');Arrays.fill(suffix,'\0');}
    }
    boolean remember(int start,String before,String after,String prefix,String suffix,long now,long epoch,Object c){
        if(start<0||before==null||after==null||prefix==null||suffix==null||c==null||before.equals(after)
            ||before.length()>2048||after.length()>2048||prefix.length()>CONTEXT||suffix.length()>CONTEXT
            ||before.length()+after.length()+prefix.length()+suffix.length()>CHARS
            ||start>Integer.MAX_VALUE-Math.max(before.length(),after.length())){clear();return false;}
        prune(now,epoch,c);
        while(entries.size()>cursor)entries.remove(entries.size()-1).erase();
        Entry entry=new Entry(start,before,after,prefix,suffix,now,epoch,c);
        while(!entries.isEmpty()&&(entries.size()>=LIMIT||total()+entry.size()>CHARS)){
            entries.remove(0).erase();cursor--;
        }
        entries.add(entry);cursor=entries.size();return true;
    }
    private int total(){int n=0;for(Entry e:entries)n+=e.size();return n;}
    Entry next(boolean redo,long now,long epoch,Object c){
        prune(now,epoch,c);
        return redo?(cursor<entries.size()?entries.get(cursor):null):(cursor>0?entries.get(cursor-1):null);
    }
    void applied(boolean redo){cursor+=redo?1:-1;}
    private void prune(long now,long epoch,Object c){
        for(Entry e:entries)if(!e.valid(now,epoch,c)){clear();return;}
    }
    long nextExpiry(long now){long n=Long.MAX_VALUE;for(Entry e:entries)n=Math.min(n,Math.max(0,TTL-(now-e.created)));return n;}
    boolean isEmpty(){return entries.isEmpty();}
    void clear(){for(Entry e:entries)e.erase();entries.clear();cursor=0;}
}
