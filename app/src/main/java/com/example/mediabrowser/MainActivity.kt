package com.example.mediabrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mediabrowser.data.local.PreferencesDataStore
import com.example.mediabrowser.domain.model.AppSettings
import com.example.mediabrowser.ui.navigation.ArtistNavigationState
import com.example.mediabrowser.ui.navigation.CategoryNavigationState
import com.example.mediabrowser.ui.navigation.MediaBrowserNavGraph
import com.example.mediabrowser.ui.theme.MediaBrowserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var artistNavigationState: ArtistNavigationState

    @Inject
    lateinit var categoryNavigationState: CategoryNavigationState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsFlow = remember { preferencesDataStore.settingsFlow }
            val settings by settingsFlow.collectAsState(initial = AppSettings())

            MediaBrowserTheme(settings = settings) {
                MediaBrowserNavGraph(
                    artistNavigationState = artistNavigationState,
                    categoryNavigationState = categoryNavigationState
                )
            }
        }
    }
}