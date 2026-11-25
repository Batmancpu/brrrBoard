// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Local, fail-closed URL policy. No network, redirects, clipboard writes or logging. */
public final class CleanLinkPolicy {
    public static final long WINDOW_MS = 10_000L;
    private static final int MAX_LENGTH = 4096;
    private static final Set<String> GLOBAL = new HashSet<>(Arrays.asList(
        "utm_source","utm_medium","utm_campaign","utm_content","utm_term","utm_id",
        "utm_source_platform","utm_creative_format","utm_marketing_tactic",
        "fbclid","gclid","dclid","gbraid","wbraid","msclkid","ttclid","twclid",
        "yclid","li_fat_id","mc_cid","mc_eid","mkt_tok","_hsenc","_hsmi",
        // Audited narrow subset of ClearURLs globalRules (see docs/URL_CLEANING_RULES.md).
        "srsltid","_ga","_gl","__twitter_impression","__hsfp","__hssc","__hstc",
        "vero_id","vero_conv","rb_clickid","wickedid","oly_anon_id","oly_enc_id",
        "mtm_campaign","mtm_source","mtm_medium","mtm_content","mtm_keyword"));
    private static final Set<String> SECRET = new HashSet<>(Arrays.asList(
        "token","access_token","accesstoken","refresh_token","refreshtoken","id_token",
        "api_key","apikey","password","passwd","secret","signature","sig","auth","code",
        "key","expires","expiry","credential","session","sessionid","session_id",
        "state","oauth_token","samlrequest","samlresponse","relaystate","policy",
        "key-pair-id","x-amz-signature","x-goog-signature","ticket","otp",
        "reset_token","invite_token","auth_token","access_key","accesskey","hmac"));
    private CleanLinkPolicy() {}

    /** Zero means ineligible. Never infer a copy time from read time. */
    public static long remaining(long copiedWall, long nowWall) {
        if (copiedWall <= 0 || nowWall < copiedWall) return 0;
        long age = nowWall - copiedWall;
        return age < WINDOW_MS ? WINDOW_MS - age : 0;
    }

    /** Decode only ASCII parameter names, not values. Reject ambiguous names. */
    private static String name(String segment) {
        String raw = segment.split("=", 2)[0];
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%') {
                if (i + 2 >= raw.length()) return null;
                int a = Character.digit(raw.charAt(++i), 16), b = Character.digit(raw.charAt(++i), 16);
                if (a < 0 || b < 0) return null;
                c = (char)((a << 4) | b);
            }
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' ||
                    c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.')) return null;
            out.append(c);
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
    private static boolean secret(String n) {
        return SECRET.contains(n) || n.startsWith("x-amz-") || n.startsWith("x-goog-")
                || n.endsWith("_token") || n.endsWith("-token") || n.endsWith("_signature");
    }
    private static boolean hostIs(String host, String root) {
        return host.equals(root) || host.endsWith("." + root);
    }
    private static boolean remove(String host, String n) {
        return GLOBAL.contains(n)
            || (hostIs(host,"youtube.com") || host.equals("youtu.be") || host.equals("open.spotify.com")) && n.equals("si")
            || hostIs(host,"instagram.com") && (n.equals("igsh") || n.equals("igshid"))
            || hostIs(host,"facebook.com") && n.equals("mibextid")
            || (hostIs(host,"youtube.com") || host.equals("youtu.be")) && n.equals("pp")
            || hostIs(host,"linkedin.com") && (n.equals("trackingid") || n.equals("trk"))
            || (hostIs(host,"twitter.com") || hostIs(host,"x.com")) && (n.equals("ref_src") || n.equals("ref_url"));
    }
    // Conservative exceptions: retain original URL instead of trying to rewrite
    // application-specific navigation, signed resources or sensitive endpoints.
    private static boolean preserveAll(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath().toLowerCase(Locale.ROOT);
        return hostIs(host,"github.com") || hostIs(host,"gitlab.com") ||
            hostIs(host,"cloudflare.com") || hostIs(host,"prismic.io") ||
            hostIs(host,"stripe.com") || hostIs(host,"paypal.com") ||
            hostIs(host,"accounts.google.com") || hostIs(host,"myaccount.google.com") ||
            hostIs(host,"docs.google.com") || hostIs(host,"drive.google.com") ||
            hostIs(host,"matrix.org") && path.startsWith("/_matrix/") ||
            path.matches(".*(?:^|/)(?:payment|payments|checkout|billing|purchase|pay|download|api)(?:/|$).*");
    }
    private static URI parse(String url) {
        if (url == null || url.isEmpty() || url.length() > MAX_LENGTH) return null;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= 32 || c == 127 || c == '\\' || Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT) return null;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme(), host = uri.getHost();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || host == null || uri.getRawUserInfo() != null || uri.getPort() > 65535) return null;
            host = host.toLowerCase(Locale.ROOT);
            // Intentionally accept only unambiguous public DNS-shaped names; no IP literals,
            // single-label names, local suffixes or numeric final labels. No DNS is performed.
            if (!host.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z][a-z0-9-]*")
                    || host.endsWith(".local") || host.endsWith(".localhost")
                    || host.endsWith(".internal") || host.endsWith(".lan") || host.endsWith(".home")
                    || host.endsWith(".onion") || host.endsWith(".test") || host.endsWith(".invalid")) return null;
            String path = uri.getPath().toLowerCase(Locale.ROOT);
            if (path.matches(".*(?:^|/)(?:oauth[0-9]*|authorize|login|signin|sign-in|reset|reset-password|password-reset|verify|activate|invitation|invite|magic-link|magiclink|sso|saml)(?:/|$).*")) return null;
            if (uri.getRawQuery() != null) {
                for (String p : uri.getRawQuery().split("&", -1)) {
                    String n = name(p);
                    if (n == null || secret(n)) return null;
                    // Do not interpret alternate query delimiters or ambiguous encoded names.
                    if (p.indexOf(';') >= 0) return null;
                }
            }
            String fragment = uri.getRawFragment();
            if (fragment != null && (fragment.contains("=") || fragment.contains("%"))) return null;
            return uri;
        } catch (Exception ignored) { return null; }
    }
    /** Eligibility checks only; called before the chip is displayed. */
    public static boolean eligible(String url) { return parse(url) != null; }

    /** Called exclusively after deliberate Chrome action. Null means refuse navigation. */
    public static String cleanForNavigation(String url) {
        URI original = parse(url);
        if (original == null) return null;
        String query = original.getRawQuery();
        if (query == null || query.isEmpty() || preserveAll(original)) return url;
        ArrayList<String> retained = new ArrayList<>();
        boolean changed = false;
        String host = original.getHost().toLowerCase(Locale.ROOT);
        for (String p : query.split("&", -1)) {
            if (remove(host, name(p))) changed = true;
            else retained.add(p);
        }
        if (!changed) return url;
        int q = url.indexOf('?'), hash = url.indexOf('#', q);
        String result = url.substring(0, q) +
                (retained.isEmpty() ? "" : "?" + String.join("&", retained)) +
                (hash < 0 ? "" : url.substring(hash));
        URI checked = parse(result);
        if (checked == null || !original.getRawAuthority().equals(checked.getRawAuthority())
                || !original.getScheme().equals(checked.getScheme())
                || !original.getRawPath().equals(checked.getRawPath())
                || !java.util.Objects.equals(original.getRawFragment(), checked.getRawFragment())) return null;
        return result;
    }
}
