// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import java.util.Locale

/** Local recognition only. Numeric carrier overlap requires an explicit name and tracking label. */
object ParcelAccessoryPolicy {
    data class Parcel(val carrier: String, val url: String)
    private val ups = Regex("(?i)1Z[A-Z0-9]{16}")
    private val labelled = Regex("(?i)^(UPS|FedEx|DHL(?: Express)?|Blue[ ]?Dart|Delhivery)\\s+(?:tracking(?:\\s+(?:number|id|code))?|AWB|waybill)\\s*[:#-]?\\s*([A-Z0-9]{8,22})$")
    fun choose(text: String): Parcel? {
        if (text.length > 96) return null
        if (ups.matches(text)) return Parcel("UPS", "https://www.ups.com/track?tracknum=" + text.uppercase(Locale.ROOT))
        val match = labelled.matchEntire(text) ?: return null
        val carrier = match.groupValues[1].lowercase(Locale.ROOT).replace(" ", "")
        val number = match.groupValues[2].uppercase(Locale.ROOT)
        return when (carrier) {
            "ups" -> if (ups.matches(number)) Parcel("UPS", "https://www.ups.com/track?tracknum=$number") else null
            "fedex" -> if (number.matches(Regex("[0-9]{12}|[0-9]{15}|[0-9]{20}|[0-9]{22}")))
                Parcel("FedEx", "https://www.fedex.com/apps/fedextrack/?action=track&tracknumbers=$number") else null
            "dhl", "dhlexpress" -> if (number.matches(Regex("[0-9]{10}")))
                Parcel("DHL", "https://www.dhl.com/in-en/home/tracking.html") else null
            "bluedart" -> if (number.matches(Regex("[0-9]{11}")))
                Parcel("Blue Dart", "https://www.bluedart.com/web/guest/trackdartresultthirdparty?trackFor=0&trackNo=$number") else null
            "delhivery" -> if (number.matches(Regex("[0-9]{10,15}")))
                Parcel("Delhivery", "https://www.delhivery.com/tracking") else null
            else -> null
        }
    }
}
