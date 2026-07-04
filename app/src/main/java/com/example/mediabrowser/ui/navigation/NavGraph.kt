package com.example.mediabrowser.ui.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.ui.artist.ArtistScreen
import com.example.mediabrowser.ui.downloads.DownloadsScreen
import com.example.mediabrowser.ui.favorites.FavoritesScreen
import com.example.mediabrowser.ui.home.CategoryDetailScreen
import com.example.mediabrowser.ui.home.HomeScreen
import com.example.mediabrowser.ui.search.SearchScreen
import com.example.mediabrowser.ui.settings.SettingsScreen

@Composable
fun MediaBrowserNavGraph(
    navController: NavHostController = rememberNavController(),
    artistNavigationState: ArtistNavigationState,
    categoryNavigationState: CategoryNavigationState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnArtistPage = currentDestination?.hierarchy?.any { it.route == Destination.ArtistPage.route } == true
    val isOnCategoryDetail = currentDestination?.hierarchy?.any { it.route == Destination.CategoryDetail.route } == true

    // Shared by every screen that can host PostFeedScreen, so tapping an
    // artist tag in any post's expanded details navigates to that artist's
    // dedicated page, regardless of which screen the feed was opened from.
    val onNavigateToArtist: (ArtistProfile) -> Unit = { profile ->
        artistNavigationState.set(profile)
        navController.navigate(Destination.ArtistPage.route)
    }

    Scaffold(
        bottomBar = { if (!isOnArtistPage && !isOnCategoryDetail) BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(bottom = if (isOnArtistPage || isOnCategoryDetail) 0.dp else paddingValues.calculateBottomPadding())
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onNavigateToSearch = {
                        navController.navigate(Destination.Search.route) {
                            popUpTo(Destination.Home.route)
                        }
                    },
                    onOpenCategory = { category ->
                        categoryNavigationState.set(category)
                        navController.navigate(Destination.CategoryDetail.route)
                    },
                    onNavigateToArtist = onNavigateToArtist
                )
            }

            composable(Destination.Search.route) {
                SearchScreen(
                    onNavigateToArtist = onNavigateToArtist
                )
            }

            composable(Destination.Favorites.route) {
                FavoritesScreen(
                    onOpenArtist = { profile ->
                        artistNavigationState.set(profile)
                        navController.navigate(Destination.ArtistPage.route)
                    },
                    onNavigateToArtist = onNavigateToArtist,
                    section = null,
                    onOpenSection = { sec ->
                        navController.navigate(Destination.FavoriteSection.createRoute(sec.name))
                    }
                )
            }

            composable(
                route = Destination.FavoriteSection.route,
                arguments = listOf(navArgument(Destination.FavoriteSection.ARG) {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val sectionName = backStackEntry.arguments
                    ?.getString(Destination.FavoriteSection.ARG)
                val section = runCatching {
                    com.example.mediabrowser.ui.favorites.FavoriteSection.valueOf(sectionName ?: "")
                }.getOrNull()
                if (section != null) {
                    FavoritesScreen(
                        onOpenArtist = { profile ->
                            artistNavigationState.set(profile)
                            navController.navigate(Destination.ArtistPage.route)
                        },
                        onNavigateToArtist = onNavigateToArtist,
                        section = section,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Destination.Downloads.route) {
                DownloadsScreen(
                    onNavigateToArtist = onNavigateToArtist
                )
            }

            composable(Destination.Settings.route) {
                SettingsScreen()
            }

            composable(Destination.ArtistPage.route) {
                val profile by artistNavigationState.current.collectAsState()
                profile?.let {
                    ArtistScreen(
                        profile = it,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToArtist = onNavigateToArtist
                    )
                }
            }

            composable(Destination.CategoryDetail.route) {
                val category by categoryNavigationState.current.collectAsState()
                category?.let {
                    CategoryDetailScreen(
                        category = it,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToArtist = onNavigateToArtist
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Column {
        // Thin divider line above the bar, as in the design.
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF2A2D2F))
        )
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            bottomNavDestinations.forEach { destination ->
                val navDestination = destination as BottomNavDestination
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

                // Active item gets a thin rounded outline; inactive is plain text.
                val itemModifier = if (selected) {
                    androidx.compose.ui.Modifier
                        .border(
                            1.dp,
                            Color(0xFFB0B3B7),
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                } else {
                    androidx.compose.ui.Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                }

                Text(
                    text = navDestination.label,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = androidx.compose.ui.Modifier
                        .clickable {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .then(itemModifier)
                )
            }
        }
    }
}