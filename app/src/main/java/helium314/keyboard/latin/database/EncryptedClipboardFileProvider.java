// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileNotFoundException;

/** Same grant-only FileProvider authority. Only clipboard image paths are intercepted. */
public final class EncryptedClipboardFileProvider extends FileProvider {
    private File clipboard(Uri uri) {
        final Context c = getContext();
        if (c == null) throw new SecurityException("Provider unavailable");
        try { return ClipboardImageVault.clipboardPath(c,uri); }
        catch (Exception e) { throw new SecurityException("Invalid provider path"); }
    }
    @NonNull @Override public ParcelFileDescriptor openFile(@NonNull Uri uri,@NonNull String mode)
            throws FileNotFoundException {
        File file=clipboard(uri);
        if(file==null) return super.openFile(uri,mode);
        if(!"r".equals(mode)) throw new SecurityException("Clipboard image is read-only");
        try { return ClipboardImageVault.openRead(getContext(),file); }
        catch (Exception e) { throw new FileNotFoundException("Clipboard image unavailable"); }
    }
    @Override public int delete(@NonNull Uri uri,@Nullable String selection,@Nullable String[] args) {
        if(clipboard(uri)!=null) throw new SecurityException("Clipboard image is read-only");
        return super.delete(uri,selection,args);
    }
    @Override public Cursor query(@NonNull Uri uri,@Nullable String[] projection,
            @Nullable String selection,@Nullable String[] args,@Nullable String sortOrder) {
        File file=clipboard(uri);
        if(file==null) return super.query(uri,projection,selection,args,sortOrder);
        final long size;
        try { size=ClipboardImageVault.plainSize(getContext(),file); }
        catch (Exception e) { throw new SecurityException("Clipboard image unavailable"); }
        String[] requested=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        java.util.ArrayList<String> cols=new java.util.ArrayList<>();
        java.util.ArrayList<Object> values=new java.util.ArrayList<>();
        for(String column:requested) {
            if(OpenableColumns.DISPLAY_NAME.equals(column)) { cols.add(column); values.add("clipboard-image."+extension(file)); }
            else if(OpenableColumns.SIZE.equals(column)) { cols.add(column); values.add(size); }
        }
        MatrixCursor result=new MatrixCursor(cols.toArray(new String[0]),1);
        result.addRow(values.toArray());
        return result;
    }
    private String extension(File f) {
        String n=f.getName(); int dot=n.lastIndexOf('.');
        return dot<0?"img":n.substring(dot+1);
    }
}
