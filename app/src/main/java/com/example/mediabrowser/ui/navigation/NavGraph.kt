package com.example.mediabrowser.ui.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mediabrowser.ui.artist.ArtistScreen
import com.example.mediabrowser.ui.downloads.DownloadsScreen
import com.example.mediabrowser.ui.favorites.FavoritesScreen
import com.example.mediabrowser.ui.home.HomeScreen
import com.example.mediabrowser.ui.search.SearchScreen
import com.example.mediabrowser.ui.settings.SettingsScreen

@Composable
fun MediaBrowserNavGraph(
    navController: NavHostController = rememberNavController(),
    artistNavigationState: ArtistNavigationState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnArtistPage = currentDestination?.hierarchy?.any { it.route == Destination.ArtistPage.route } == true

    Scaffold(
        bottomBar = { if (!isOnArtistPage) BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(bottom = if (isOnArtistPage) 0.dp else paddingValues.calculateBottomPadding())
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onNavigateToSearch = {
                        navController.navigate(Destination.Search.route) {
                            popUpTo(Destination.Home.route)
                        }
                    }
                )
            }

            composable(Destination.Search.route) {
                SearchScreen()
            }

            composable(Destination.Favorites.route) {
                FavoritesScreen(
                    onOpenArtist = { profile ->
                        artistNavigationState.set(profile)
                        navController.navigate(Destination.ArtistPage.route)
                    }
                )
            }

            composable(Destination.Downloads.route) {
                DownloadsScreen()
            }

            composable(Destination.Settings.route) {
                SettingsScreen()
            }

            composable(Destination.ArtistPage.route) {
                val profile by artistNavigationState.current.collectAsState()
                profile?.let {
                    ArtistScreen(
                        profile = it,
                        onBackClick = { navController.popBackStack() }
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

    NavigationBar(
        containerColor = Color(0xFF050607),
        contentColor = Color.White
    ) {
        bottomNavDestinations.forEach { destination ->
            val navDestination = destination as BottomNavDestination
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) navDestination.selectedIcon else navDestination.unselectedIcon,
                        contentDescription = navDestination.label,
                        tint = Color.White
                    )
                },
                label = { Text(navDestination.label, color = Color.White) }
            )
        }
    }
}