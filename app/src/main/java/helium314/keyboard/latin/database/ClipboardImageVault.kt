// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.database

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.UserManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import androidx.annotation.RequiresApi
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import helium314.keyboard.compat.isDeviceLocked
import java.io.File
import java.io.InputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Authenticated image bytes, same logical URI, distinct sealed physical file. Never creates a plaintext temporary file. */
object ClipboardImageVault {
    private const val ALIAS = "com.mangoloads.board.clipboard_image_v1"
    internal const val MAX_IMAGE_BYTES = 12 * 1024 * 1024
    private const val OVERHEAD = 32
    private val magic = byteArrayOf(66,82,73,1)
    private val namePattern = Regex("clip_[0-9]+_[a-f0-9]{16}\\.[a-zA-Z0-9]{1,12}")
    private val reader = ThreadPoolExecutor(1,2,15,TimeUnit.SECONDS,ArrayBlockingQueue(2))
    private val leases = Semaphore(2)
    private val proxyHandler by lazy { Handler(HandlerThread("brrClipboardImageRead").apply { start() }.looper) }
    private fun available(c: Context): Boolean = !c.isDeviceProtectedStorage &&
        c.getSystemService(UserManager::class.java)?.isUserUnlocked == true && !isDeviceLocked(c)
    private fun dir(c: Context) = File(c.cacheDir,"clipboard").canonicalFile
    /** Match FileProvider's canonical path, including percent-encoded separators. */
    @JvmStatic fun clipboardPath(c: Context, uri: Uri): File? {
        if (uri.scheme != "content" || uri.authority != "${c.packageName}.fileprovider") return null
        val path = uri.encodedPath ?: return null
        val split = path.indexOf('/',1)
        if (split < 0 || Uri.decode(path.substring(1,split)) != "cache") return null
        val root = dir(c)
        val file = File(c.cacheDir,Uri.decode(path.substring(split+1))).canonicalFile
        return if (file == root || file.path.startsWith(root.path + File.separator)) file else null
    }
    internal fun validName(name: String) = namePattern.matches(name)
    private fun requireFile(c: Context, f: File) {
        require(f.canonicalFile.parentFile == dir(c) && validName(f.name))
    }
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS,null)
        if (existing is SecretKey) return existing
        check(create && existing == null)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
        return gen.generateKey()
    }
    private fun aad(name: String): ByteArray {
        require(validName(name))
        return ("brrBoard.clipboard.image.v1:" + name).toByteArray(Charsets.UTF_8)
    }
    internal fun seal(key: SecretKey,name: String,plain: ByteArray): ByteArray {
        require(plain.size in 1..MAX_IMAGE_BYTES)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key)
        require(cipher.iv.size==12)
        cipher.updateAAD(aad(name))
        return magic + cipher.iv + cipher.doFinal(plain)
    }
    internal fun open(key: SecretKey,name: String,encrypted: ByteArray): ByteArray {
        require(encrypted.size in (OVERHEAD+1)..(MAX_IMAGE_BYTES+OVERHEAD) &&
            encrypted.copyOfRange(0,4).contentEquals(magic))
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,encrypted.copyOfRange(4,16)))
        cipher.updateAAD(aad(name))
        // No streaming plaintext escapes before authentication succeeds.
        return cipher.doFinal(encrypted,16,encrypted.size-16)
    }
    private fun readBounded(input: InputStream,limit: Int): ByteArray {
        var buffer=ByteArray(minOf(64*1024,limit+1))
        var size=0
        try {
            while (true) {
                if(size==buffer.size) {
                    require(size<=limit)
                    val old=buffer
                    buffer=old.copyOf(minOf(limit+1,old.size*2))
                    old.fill(0)
                }
                val n=input.read(buffer,size,buffer.size-size)
                if(n<0) break
                require(n>0)
                size+=n
                require(size<=limit)
            }
            return buffer.copyOf(size)
        } finally { buffer.fill(0) }
    }
    private fun sealedFile(file: File) = AtomicFile(File(file.parentFile,file.name+".vault"))
    private fun exists(f: AtomicFile) = f.baseFile.exists() || File(f.baseFile.path+".bak").exists()
    fun hasStored(file: File): Boolean = file.exists() || exists(sealedFile(file))
    private fun write(c: Context,file: File,plain: ByteArray) {
        val encrypted=seal(key(true),file.name,plain)
        check(available(c))
        val atomic=sealedFile(file)
        val out=atomic.startWrite()
        try { out.write(encrypted); atomic.finishWrite(out) }
        catch(e: Exception) { atomic.failWrite(out); throw e }
        finally { encrypted.fill(0) }
    }
    /** A separate sealed file is authoritative. Corrupt ciphertext can never be treated as legacy.
     * A crash before deletion leaves a recoverable old copy; the next read retries cleanup. */
    @Synchronized fun ensureEncrypted(c: Context,file: File): Boolean = try {
        requireFile(c,file)
        check(available(c))
        val sealed=sealedFile(file)
        if (exists(sealed)) {
            val bytes=sealed.openRead().use { readBounded(it,MAX_IMAGE_BYTES+OVERHEAD) }
            try { open(key(false),file.name,bytes).fill(0) } finally { bytes.fill(0) }
        } else {
            val plain=file.inputStream().use { readBounded(it,MAX_IMAGE_BYTES) }
            try {
                require(plain.isNotEmpty())
                write(c,file,plain)
                val verified=sealed.openRead().use { readBounded(it,MAX_IMAGE_BYTES+OVERHEAD) }
                try {
                    val decoded=open(key(false),file.name,verified)
                    try { check(decoded.contentEquals(plain)) } finally { decoded.fill(0) }
                } finally { verified.fill(0) }
            } finally { plain.fill(0) }
        }
        check(!file.exists() || file.delete())
        true
    } catch (_: Exception) { false }

    @Synchronized fun cache(c: Context,source: Uri,file: File): Boolean = try {
        requireFile(c,file)
        check(available(c))
        if(hasStored(file)) ensureEncrypted(c,file) else {
        val plain=c.contentResolver.openInputStream(source)?.use { readBounded(it,MAX_IMAGE_BYTES) }
            ?: throw FileNotFoundException("Clipboard source unavailable")
        try { write(c,file,plain); true } finally { plain.fill(0) }
        }
    } catch (_: Exception) { false }

    /** Scan only our known filenames. Failed or oversized legacy entries are not served. */
    fun migrateExisting(c: Context): Int {
        if (!available(c)) return 0
        var failed=0
        for (f in runCatching { dir(c).listFiles() }.getOrNull().orEmpty()) {
            if (!available(c)) break
            if (f.isFile && validName(f.name) && !ensureEncrypted(c,f)) failed++
            else if(f.isFile && f.name.startsWith("clip_") && f.name.endsWith(".tmp") && !f.delete()) failed++
        }
        return failed
    }
    @Synchronized private fun verified(c: Context,file: File): ByteArray {
        check(available(c) && ensureEncrypted(c,file))
        val bytes=sealedFile(file).openRead().use { readBounded(it,MAX_IMAGE_BYTES+OVERHEAD) }
        return try { open(key(false),file.name,bytes) } finally { bytes.fill(0) }
    }
    /** API 26+ uses a bounded seekable proxy, old devices use an authenticated reliable pipe. */
    @JvmStatic fun openRead(c: Context,file: File): ParcelFileDescriptor {
        requireFile(c,file)
        if (!available(c)) throw FileNotFoundException("Clipboard locked")
        if(Build.VERSION.SDK_INT>=26) return proxyRead(c,file)
        val pipe=ParcelFileDescriptor.createReliablePipe()
        try {
            reader.execute {
                var plain: ByteArray?=null
                try {
                    val verified=verified(c,file)
                    plain=verified
                    check(available(c))
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(verified) }
                } catch (_: Exception) {
                    runCatching { pipe[1].closeWithError("Clipboard image unavailable") }
                } finally { plain?.fill(0); runCatching { pipe[1].close() } }
            }
            return pipe[0]
        } catch (_: Exception) {
            runCatching { pipe[0].close() }; runCatching { pipe[1].close() }
            throw FileNotFoundException("Clipboard image reader busy")
        }
    }
    @RequiresApi(26)
    private fun proxyRead(c: Context,file: File): ParcelFileDescriptor {
        if(!leases.tryAcquire()) throw FileNotFoundException("Clipboard image reader busy")
        var plain: ByteArray?=null
        val released=AtomicBoolean(false)
        val lock=Any()
        try {
            val bytes=verified(c,file)
            plain=bytes
            val handler=proxyHandler
            val expire=Runnable {
                synchronized(lock) {
                    if(released.compareAndSet(false,true)) { bytes.fill(0); leases.release() }
                }
            }
            val callback=object: ProxyFileDescriptorCallback() {
                override fun onGetSize(): Long = synchronized(lock) {
                    if(released.get() || !available(c)) throw ErrnoException("clipboard",OsConstants.EIO)
                    bytes.size.toLong()
                }
                override fun onRead(offset: Long,size: Int,data: ByteArray): Int = synchronized(lock) {
                    if(released.get() || !available(c) || offset<0 || size<0 || offset>bytes.size)
                        throw ErrnoException("clipboard",OsConstants.EIO)
                    val count=minOf(size,data.size,bytes.size-offset.toInt())
                    System.arraycopy(bytes,offset.toInt(),data,0,count)
                    count
                }
                override fun onRelease() { handler.removeCallbacks(expire); expire.run() }
            }
            val manager=c.getSystemService(StorageManager::class.java) ?: throw FileNotFoundException()
            val fd=manager.openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,callback,handler)
            handler.postDelayed(expire,60_000L)
            return fd
        } catch(e: Exception) {
            synchronized(lock) { if(released.compareAndSet(false,true)) { plain?.fill(0); leases.release() } }
            throw FileNotFoundException("Clipboard image unavailable")
        }
    }
    @Synchronized fun delete(c: Context,file: File): Boolean = try {
        requireFile(c,file)
        val sealed=sealedFile(file)
        sealed.delete()
        val oldRemoved=!file.exists() || file.delete()
        oldRemoved && !exists(sealed) && !File(sealed.baseFile.path+".new").exists()
    } catch (_: Exception) { false }
    @JvmStatic fun plainSize(c: Context,file: File): Long {
        requireFile(c,file)
        check(available(c))
        val sealed=sealedFile(file)
        return if(exists(sealed)) {
            check(sealed.baseFile.length() in (OVERHEAD+1L)..(MAX_IMAGE_BYTES+OVERHEAD.toLong()))
            sealed.baseFile.length()-OVERHEAD
        } else {
            check(file.length() in 1L..MAX_IMAGE_BYTES.toLong())
            file.length()
        }
    }
}
