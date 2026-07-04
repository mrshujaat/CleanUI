package com.example.mediabrowser.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations. Post detail is now an in-place modal dialog
 * (see PostDetailModal.kt), not a navigation route, so screens never lose
 * their scroll position when a post is opened/closed.
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home"), BottomNavDestination {
        override val label = "Home"
        override val selectedIcon: ImageVector = Icons.Filled.Home
        override val unselectedIcon: ImageVector = Icons.Outlined.Home
    }

    data object Search : Destination("search"), BottomNavDestination {
        override val label = "Search"
        override val selectedIcon: ImageVector = Icons.Filled.Search
        override val unselectedIcon: ImageVector = Icons.Outlined.Search
    }

    data object Favorites : Destination("favorites"), BottomNavDestination {
        override val label = "Favorites"
        override val selectedIcon: ImageVector = Icons.Filled.Favorite
        override val unselectedIcon: ImageVector = Icons.Outlined.FavoriteBorder
    }

    data object Downloads : Destination("downloads"), BottomNavDestination {
        override val label = "Downloads"
        override val selectedIcon: ImageVector = Icons.Filled.Download
        override val unselectedIcon: ImageVector = Icons.Outlined.Download
    }

    data object Settings : Destination("settings"), BottomNavDestination {
        override val label = "Settings"
        override val selectedIcon: ImageVector = Icons.Filled.Settings
        override val unselectedIcon: ImageVector = Icons.Outlined.Settings
    }

    data object ArtistPage : Destination("artist_page")
    data object CategoryDetail : Destination("category_detail")

    /**
     * A single favourites sub-section (Characters / Series / Tags / Artists /
     * My Poison), opened from the Favourites landing list. The section is passed
     * as a route argument.
     */
    data object FavoriteSection : Destination("favorite_section/{section}") {
        fun createRoute(section: String) = "favorite_section/$section"
        const val ARG = "section"
    }
}

interface BottomNavDestination {
    val label: String
    val selectedIcon: ImageVector
    val unselectedIcon: ImageVector
}

val bottomNavDestinations: List<Destination> = listOf(
    Destination.Home,
    Destination.Search,
    Destination.Favorites,
    Destination.Downloads,
    Destination.Settings
)