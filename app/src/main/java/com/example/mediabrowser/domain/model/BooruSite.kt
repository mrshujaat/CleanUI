package com.example.mediabrowser.domain.model

/**
 * Catalog of supported booru sites. All are Gelbooru-DAPI-compatible, so the
 * whole app (search, tags, favorites, downloads, batch tag lookup, Poison
 * feed) works against any of them — only the API host changes, plus a couple
 * of per-site quirks handled in the data layer:
 *  - [supportsAutocomplete]: only Rule34 has the native ranked autocomplete
 *    endpoint (orange suggestions); other sites use DAPI prefix search only.
 *  - Gelbooru 0.2.5 wraps its post array in a `{"post": [...]}` object, which
 *    the response parser handles for every site transparently.
 */
enum class BooruSite(
    val displayName: String,
    val apiBaseUrl: String,
    val supportsAutocomplete: Boolean,
    // CDN base for constructing image URLs when the API returns blank file_url
    // (TBIB, Realbooru, some forks do this — they only send directory+image+hash).
    // Empty means "the API always returns usable full URLs" (Rule34, Xbooru).
    val cdnBase: String = ""
) {
    RULE34("Rule34", "https://api.rule34.xxx/", true),
    // Rule34.world is a Rule34 mirror using standard DAPI (bare JSON array),
    // no auth needed.
    RULE34_WORLD("Rule34.world", "https://rule34.world/", false),
    // Gelbooru-family sites that share Rule34's exact DAPI shape (bare JSON array,
    // no auth needed for read). These are known-working — Xbooru is your baseline,
    // and Realbooru/TBIB/HypnoHub use identical endpoints.
    XBOORU("Xbooru", "https://xbooru.com/", false, cdnBase = "https://img.xbooru.com"),
    REALBOORU("Realbooru", "https://realbooru.com/", false, cdnBase = "https://realbooru.com"),
    TBIB("TBIB", "https://tbib.org/", false, cdnBase = "https://tbib.org"),
    HYPNOHUB("HypnoHub", "https://hypnohub.net/", false, cdnBase = "https://hypnohub.net"),
    // Gelbooru.com is DAPI-compatible but requires an api_key+user_id since 2023;
    // it also wraps its response as {"post":[...]}. Kept in the list so users can
    // switch to it once they've entered credentials in the API section.
    GELBOORU("Gelbooru (needs API key)", "https://gelbooru.com/", false);

    val isDefault: Boolean get() = this == RULE34

    companion object {
        /** Every selectable site other than the default. */
        val OTHERS: List<BooruSite> = entries.filter { !it.isDefault }

        /** Resolve the active site from settings; unknown/blank → Rule34. */
        fun fromSettings(settings: AppSettings): BooruSite =
            entries.find { it.name == settings.apiProviderName } ?: RULE34

        fun fromName(name: String?): BooruSite =
            entries.find { it.name == name } ?: RULE34
    }
}
