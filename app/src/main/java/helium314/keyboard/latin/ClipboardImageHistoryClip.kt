// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.content.ClipDescription
import android.net.Uri

object ClipboardImageHistoryClip {
    private const val PREFIX = "\u0001image\t"

    data class ImageClip(val uri: Uri, val mimeType: String, val label: String)

    fun hasImageHistoryPrefix(text: String): Boolean = text.startsWith(PREFIX)

    fun decode(text: String): ImageClip? {
        if (!text.startsWith(PREFIX)) return null
        val parts = text.removePrefix(PREFIX).split('\t', limit = 3)
        if (parts.size != 3) return null
        return ImageClip(Uri.parse(parts[0]), parts[1], parts[2])
    }

    fun encode(uri: Uri, mimeType: String, label: String): String {
        val safeLabel = label.replace('\t', ' ').replace('\n', ' ')
        return "$PREFIX$uri\t$mimeType\t$safeLabel"
    }
}

/** A rejected stored image must never be inserted as serialized clipboard text. */
enum class ClipboardHistoryPasteOutcome { TEXT, COMMITTED, REJECTED }

internal object ClipboardHistoryPastePolicy {
    fun paste(text: String, commitImage: (ClipboardImageHistoryClip.ImageClip) -> Boolean): ClipboardHistoryPasteOutcome {
        if (!ClipboardImageHistoryClip.hasImageHistoryPrefix(text)) return ClipboardHistoryPasteOutcome.TEXT
        val image = ClipboardImageHistoryClip.decode(text) ?: return ClipboardHistoryPasteOutcome.REJECTED
        if (image.uri.scheme != "content" ||
            !ClipDescription.compareMimeTypes(image.mimeType, "image/*")) return ClipboardHistoryPasteOutcome.REJECTED
        return if (commitImage(image)) ClipboardHistoryPasteOutcome.COMMITTED else ClipboardHistoryPasteOutcome.REJECTED
    }
}
