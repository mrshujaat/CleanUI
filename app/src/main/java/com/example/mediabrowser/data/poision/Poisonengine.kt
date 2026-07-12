package com.example.mediabrowser.data.poison

import com.example.mediabrowser.data.local.dao.AffinityDao
import com.example.mediabrowser.data.local.dao.InteractionDao
import com.example.mediabrowser.data.local.entity.ArtistAffinityEntity
import com.example.mediabrowser.data.local.entity.InteractionEntity
import com.example.mediabrowser.data.local.entity.TagAffinityEntity
import com.example.mediabrowser.domain.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * The recommendation engine. Records interactions and maintains derived,
 * time-decayed affinity scores for tags and artists.
 *
 * Design notes for performance:
 *  - Every public entry point is fire-and-forget on Dispatchers.IO. The UI never
 *    waits for scoring; recording an interaction returns immediately.
 *  - Scores are stored already-aggregated (one row per tag), so generating the
 *    feed is a single ORDER BY, never a recompute over history.
 *  - Time decay is applied lazily on write: when we touch a tag we first decay its
 *    old score by how long it's been since the last update, then add the new
 *    weight. No background recompute needed, and recent taste naturally wins.
 */
@Singleton
class PoisonEngine @Inject constructor(
    private val interactionDao: InteractionDao,
    private val affinityDao: AffinityDao
) {
    // Independent scope so interaction recording survives past the screen that
    // triggered it. SupervisorJob so one failure doesn't cancel future writes.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class InteractionType(val weight: Int) {
        VIEW(1),
        SEARCH(2),
        LIKE(3),
        DOWNLOAD(4),
        FAVORITE(5)
    }

    companion object {
        // Half-life of an affinity score, in days. After this long without
        // reinforcement a score halves. 30 days keeps the feed responsive to
        // changing taste while not being twitchy.
        private const val HALF_LIFE_DAYS = 30.0
        private const val MS_PER_DAY = 24.0 * 60.0 * 60.0 * 1000.0

        // Tags that are structural noise rather than taste signals. We don't want
        // the feed dominated by "video" or "highres" just because they co-occur
        // with everything.
        private val IGNORED_TAGS = setOf(
            "video", "animated", "sound", "webm", "mp4", "highres", "absurdres",
            "tagme", "lowres"
        )
    }

    /** Decay a stored score to "now" given when it was last updated. */
    private fun decayed(oldScore: Double, lastUpdated: Long, now: Long): Double {
        val ageDays = (now - lastUpdated).coerceAtLeast(0) / MS_PER_DAY
        val factor = 0.5.pow(ageDays / HALF_LIFE_DAYS)
        return oldScore * factor
    }

    /**
     * Record an interaction with a post. Extracts the post's tags, writes a raw
     * history row, and bumps the affinity scores for each tag. Fire-and-forget.
     */
    fun record(post: Post, type: InteractionType) {
        val tags = post.tags
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && it !in IGNORED_TAGS }
        recordRaw(post.id, tags, artist = null, type = type)
    }

    /**
     * Record a search as a set of tags the user typed. Searches are a strong
     * explicit signal of intent, so each searched tag gets the SEARCH weight.
     */
    fun recordSearch(tags: List<String>) {
        val clean = tags
            .map { it.trim().lowercase().replace(' ', '_') }
            .filter { it.isNotBlank() && it !in IGNORED_TAGS }
        if (clean.isEmpty()) return
        recordRaw(postId = -1, tags = clean, artist = null, type = InteractionType.SEARCH)
    }

    /** Explicitly reinforce an artist (e.g. when favouriting an artist). */
    fun recordArtist(artist: String, type: InteractionType) {
        val a = artist.trim().lowercase()
        if (a.isBlank()) return
        scope.launch {
            val now = System.currentTimeMillis()
            bumpArtist(a, type.weight.toDouble(), now)
            interactionDao.insert(
                InteractionEntity(
                    postId = -1, tagsCsv = "", artistId = a,
                    type = type.name, weight = type.weight, timestamp = now
                )
            )
        }
    }

    private fun recordRaw(postId: Long, tags: List<String>, artist: String?, type: InteractionType) {
        if (tags.isEmpty() && artist == null) return
        scope.launch {
            val now = System.currentTimeMillis()
            tags.forEach { bumpTag(it, type.weight.toDouble(), now) }
            artist?.let { bumpArtist(it, type.weight.toDouble(), now) }
            interactionDao.insert(
                InteractionEntity(
                    postId = postId,
                    tagsCsv = tags.joinToString(" "),
                    artistId = artist,
                    type = type.name,
                    weight = type.weight,
                    timestamp = now
                )
            )
        }
    }

    private suspend fun bumpTag(tag: String, add: Double, now: Long) {
        val existing = affinityDao.getTag(tag)
        val base = if (existing != null) decayed(existing.score, existing.lastUpdated, now) else 0.0
        affinityDao.upsertTag(TagAffinityEntity(tag = tag, score = base + add, lastUpdated = now))
    }

    private suspend fun bumpArtist(artist: String, add: Double, now: Long) {
        val existing = affinityDao.getArtist(artist)
        val base = if (existing != null) decayed(existing.score, existing.lastUpdated, now) else 0.0
        affinityDao.upsertArtist(ArtistAffinityEntity(artist = artist, score = base + add, lastUpdated = now))
    }

    /** Top tags by current (decayed) score, for feed candidate sourcing. */
    suspend fun topTags(limit: Int): List<String> {
        val now = System.currentTimeMillis()
        return affinityDao.topTags(limit * 2)
            .map { it.tag to decayed(it.score, it.lastUpdated, now) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /** Top artists by current (decayed) score. */
    suspend fun topArtists(limit: Int): List<String> {
        val now = System.currentTimeMillis()
        return affinityDao.topArtists(limit * 2)
            .map { it.artist to decayed(it.score, it.lastUpdated, now) }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Score a candidate post against the user's current affinities. Used to rank
     * the hybrid candidate pool client-side. Sums the decayed affinity of each of
     * the post's tags, so a post matching several liked tags ranks above one that
     * matches a single tag.
     */
    suspend fun scorePost(post: Post): Double {
        val now = System.currentTimeMillis()
        val tagScores = affinityDao.allTags().associate { it.tag to decayed(it.score, it.lastUpdated, now) }
        return post.tags.sumOf { tagScores[it.trim().lowercase()] ?: 0.0 }
    }

    /** Whether the user has enough history for the feed to be meaningful. */
    suspend fun hasEnoughSignal(): Boolean = affinityDao.allTags().size >= 3

    suspend fun clearAll() {
        affinityDao.clearTags()
        affinityDao.clearArtists()
        interactionDao.clearAll()
    }
}