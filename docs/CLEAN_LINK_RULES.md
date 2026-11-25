# Clean Link v1 — 2026-11-25

Research references:
- https://firefox-source-docs.mozilla.org/toolkit/components/antitracking/anti-tracking/query-stripping/index.html
- https://github.com/brave/brave-browser/wiki/Copy-clean-link
- https://github.com/brave/brave-browser/wiki/Query-String-Filter
- https://github.com/ClearURLs/Rules

Conservative independent subset; do not import full regex/redirect catalogs without review. Exact campaign/ad-click names in CleanLinkPolicy.GLOBAL; domain-specific si (YouTube/Spotify), igsh/igshid (Instagram), mibextid (Facebook). Retain unknown parameters, query order, repeats and encoding, path and fragment; refuse ambiguous query/fragment, credential, auth and signed indicators and local/IP targets. No automatic redirect unwrap or network lookup. No mutation during normal copy/paste. Sanitizer invoked only by deliberate Chrome click after receipt validation. Rule updates must include sample before/after, false-positive cases and tests. Android Chrome can still track visits; cleaning is not malware or anonymity protection. Ten seconds from reliable OS copy timestamp, never fallback read time. Existing full clipboard strip integration only in phase 1; compact/typing coexistence and ranking remain outstanding.
