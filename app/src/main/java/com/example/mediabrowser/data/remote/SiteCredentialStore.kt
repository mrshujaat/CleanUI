package com.example.mediabrowser.data.remote

import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.domain.model.BooruSite
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Per-site API credentials. Each booru has its own [key]/[user] pair, stored
 * as a JSON map in [AppSettings.siteCredentials] keyed by [BooruSite.name].
 * Rule34 falls back to the legacy top-level [AppSettings.apiCredentialOne]/
 * [AppSettings.apiCredentialTwo] fields so existing installs don't have to
 * re-enter them.
 */
@Serializable
data class SiteApiCredential(
    val key: String = "",
    val user: String = ""
) {
    val isEmpty: Boolean get() = key.isBlank() && user.isBlank()
}

object SiteCredentialStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun all(settings: AppSettings): Map<String, SiteApiCredential> {
        if (settings.siteCredentials.isBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, SiteApiCredential>>(settings.siteCredentials)
        }.getOrDefault(emptyMap())
    }

    fun get(settings: AppSettings, site: BooruSite): SiteApiCredential {
        val stored = all(settings)[site.name]
        if (stored != null && !stored.isEmpty) return stored
        // Rule34 backwards compatibility.
        if (site == BooruSite.RULE34) {
            return SiteApiCredential(
                key = settings.apiCredentialOne,
                user = settings.apiCredentialTwo
            )
        }
        return SiteApiCredential()
    }

    fun serialize(map: Map<String, SiteApiCredential>): String =
        json.encodeToString(kotlinx.serialization.serializer(), map)
}
