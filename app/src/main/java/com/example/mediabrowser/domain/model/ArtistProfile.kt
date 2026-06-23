package com.example.mediabrowser.domain.model

/**
 * Generic artist/creator profile. Bio, avatar, and follower count are
 * placeholders for providers that expose real creator-profile data (e.g.
 * Patreon-style platforms); Pexels has no such endpoint, so those fields
 * are left null/zero today and only [displayName] and [postQuery] are
 * populated from a [FavoriteArtist].
 */
data class ArtistProfile(
    val artistId: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val followerCount: Int? = null,
    /** The query used to fetch this artist's posts from the current provider. */
    val postQuery: String
)