// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned bounded record. Legacy model means migrate without fabricated heatmap evidence. */
final class AdaptiveTouchRecord {
    static final int MAX_LAYOUTS=12, MAX_BYTES=16*1024-64;
    private static final int LEGACY=0x41545031, CURRENT=0x41545032;
    final LinkedHashMap<Long,AdaptiveTouchModel> models=new LinkedHashMap<>();
    final LinkedHashMap<Long,AdaptiveTouchEvidence> evidence=new LinkedHashMap<>();
    static AdaptiveTouchRecord read(byte[] bytes) throws IOException {
        if(bytes.length>MAX_BYTES) throw new IOException("oversize");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        int magic=in.readInt(), size=in.readInt();
        if((magic!=LEGACY && magic!=CURRENT) || size<0 || size>MAX_LAYOUTS)
            throw new IOException("invalid record");
        AdaptiveTouchRecord result=new AdaptiveTouchRecord();
        for(int i=0;i<size;i++) {
            long id=in.readLong();
            if(result.models.put(id,AdaptiveTouchModel.readFrom(in))!=null)
                throw new IOException("duplicate layout");
            result.evidence.put(id,magic==CURRENT ? AdaptiveTouchEvidence.readFrom(in) : new AdaptiveTouchEvidence());
        }
        if(in.available()!=0) throw new IOException("trailing bytes");
        return result;
    }
    static byte[] encode(Map<Long,AdaptiveTouchModel> models,Map<Long,AdaptiveTouchEvidence> evidence) throws IOException {
        if(models.size()>MAX_LAYOUTS || !models.keySet().equals(evidence.keySet()))
            throw new IOException("invalid layouts");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(CURRENT); out.writeInt(models.size());
        for(Map.Entry<Long,AdaptiveTouchModel> entry:models.entrySet()) {
            out.writeLong(entry.getKey()); entry.getValue().writeTo(out);
            evidence.get(entry.getKey()).writeTo(out);
        }
        out.flush();
        if(bytes.size()>MAX_BYTES) throw new IOException("oversize");
        return bytes.toByteArray();
    }
}
