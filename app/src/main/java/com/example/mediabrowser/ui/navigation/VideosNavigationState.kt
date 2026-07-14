package com.example.mediabrowser.ui.navigation

import com.example.mediabrowser.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-composable handoff for opening the Videos tab already scrolled to a
 * specific post — set from Home's Videos row before navigating, consumed
 * (and cleared) on VideosScreen entry. Same pattern as ArtistNavigationState.
 */
@Singleton
class VideosNavigationState @Inject constructor() {
    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post

    fun set(post: Post) { _post.value = post }
    fun consume(): Post? {
        val v = _post.value
        _post.value = null
        return v
    }
}
