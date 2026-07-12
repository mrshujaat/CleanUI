package com.example.mediabrowser.ui.navigation

import com.example.mediabrowser.domain.model.ArtistProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the artist profile being navigated to, since it carries richer
 * data than fits cleanly in a route string. Set right before navigating
 * to [Destination.ArtistPage], read by [com.example.mediabrowser.ui.artist.ArtistScreen].
 */
@Singleton
class ArtistNavigationState @Inject constructor() {
    private val _current = MutableStateFlow<ArtistProfile?>(null)
    val current: StateFlow<ArtistProfile?> = _current.asStateFlow()

    fun set(profile: ArtistProfile) {
        _current.value = profile
    }
}