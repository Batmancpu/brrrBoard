package helium314.keyboard.latin.suggestions
import org.junit.Assert.*
import org.junit.Test
class ParcelAccessoryPolicyTest {
    @Test fun explicitCarriersAndOfficialDestinations() {
        assertEquals("UPS", ParcelAccessoryPolicy.choose("1Z999AA10123456784")?.carrier)
        assertEquals("FedEx", ParcelAccessoryPolicy.choose("FedEx tracking: 123456789012")?.carrier)
        assertEquals("https://www.dhl.com/in-en/home/tracking.html", ParcelAccessoryPolicy.choose("DHL Express AWB: 1234567890")?.url)
        assertEquals("https://www.bluedart.com/web/guest/trackdartresultthirdparty?trackFor=0&trackNo=12345678901", ParcelAccessoryPolicy.choose("Blue Dart waybill 12345678901")?.url)
        assertEquals("https://www.delhivery.com/tracking", ParcelAccessoryPolicy.choose("Delhivery tracking number: 123456789012")?.url)
    }
    @Test fun ambiguousAndMalformedInputsDoNotRoute() {
        for (input in listOf("123456789012", "DHL 1234567890", "FedEx tracking: 1234", "FedEx tracking: 123456789012&x=1", "OTP 123456", "Unknown AWB: 123456789012"))
            assertNull(input, ParcelAccessoryPolicy.choose(input))
    }
}
