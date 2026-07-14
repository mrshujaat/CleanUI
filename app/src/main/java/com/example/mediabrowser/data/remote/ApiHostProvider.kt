package com.example.mediabrowser.data.remote

import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.domain.model.BooruSite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently selected booru site for the network layer. Retrofit is
 * built once against Rule34's host; an OkHttp interceptor consults this
 * provider per-request and rewrites the host when another site is active.
 *
 * Initialization reads the persisted setting exactly once, lazily, on the
 * OkHttp thread of the first API call (never the main thread). Updates come
 * synchronously from Settings when the user switches sites, so the very next
 * request already targets the new host.
 */
@Singleton
class ApiHostProvider @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) {
    @Volatile
    private var site: BooruSite = BooruSite.RULE34  // safe default until loaded

    @Volatile
    private var credential: SiteApiCredential = SiteApiCredential()

    @Volatile
    private var loaded: Boolean = false

    private val loadScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    init {
        // Kick off the first read on a background scope — never block the
        // network thread on a DataStore fetch. Until it lands we serve the
        // Rule34 default (safe for the vast majority of installs); once it
        // lands, currentSite/currentCredential return the real values.
        loadScope.launch {
            runCatching {
                val settings = preferencesDataStore.settingsFlow.first()
                site = BooruSite.fromSettings(settings)
                credential = SiteCredentialStore.get(settings, site)
                loaded = true
            }
        }
    }

    fun currentSite(): BooruSite = site
    fun currentCredential(): SiteApiCredential = credential

    fun update(newSite: BooruSite) {
        site = newSite
        loadScope.launch {
            runCatching {
                val settings = preferencesDataStore.settingsFlow.first()
                credential = SiteCredentialStore.get(settings, newSite)
                loaded = true
            }
        }
    }

    fun refreshCredentials() {
        loadScope.launch {
            runCatching {
                val settings = preferencesDataStore.settingsFlow.first()
                credential = SiteCredentialStore.get(settings, site)
            }
        }
    }
}
