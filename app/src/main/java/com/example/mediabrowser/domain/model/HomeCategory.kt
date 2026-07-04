package com.example.mediabrowser.domain.model

/**
 * Represents one horizontally-scrollable row on the Home screen, and the
 * full-grid "see all" destination it expands into.
 *
 * - [MostPopular] sorts all posts by score, descending (rule34 DAPI's
 *   `sort:score` meta-tag, passed as part of the `tags` query string).
 * - [Trending] is simply the default (unsorted/newest-first) feed — no
 *   special tag needed, matches HomeViewModel's existing trendingFeed.
 * - [TopSeries] represents a single popular copyright/franchise tag; each
 *   instance carries its own [tagName] so multiple series can be shown.
 */
sealed class HomeCategory(val title: String, val tagQuery: String) {
    data object MostPopular : HomeCategory(title = "Most Popular", tagQuery = "sort:score")
    data object Trending : HomeCategory(title = "Trending", tagQuery = "")
    data class TopSeries(val tagName: String, val displayName: String) :
        HomeCategory(title = displayName, tagQuery = tagName)
}

/** Fixed set of franchise tags shown in the "Top Series" home row, matching
 *  the same series names recognized by MediaRepositoryImpl.categorizeTag. */
val DEFAULT_TOP_SERIES: List<HomeCategory.TopSeries> = listOf(
    HomeCategory.TopSeries("horimiya", "Horimiya"),
    HomeCategory.TopSeries("fate", "Fate"),
    HomeCategory.TopSeries("naruto", "Naruto"),
    HomeCategory.TopSeries("one_piece", "One Piece"),
    HomeCategory.TopSeries("dragon_ball", "Dragon Ball"),
    HomeCategory.TopSeries("pokemon", "Pokemon"),
    HomeCategory.TopSeries("arknights", "Arknights"),
    HomeCategory.TopSeries("genshin_impact", "Genshin Impact"),
    HomeCategory.TopSeries("hololive", "Hololive")
)