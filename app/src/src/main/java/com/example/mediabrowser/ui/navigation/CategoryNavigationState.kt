package com.example.mediabrowser.ui.navigation

import com.example.mediabrowser.domain.model.HomeCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the [HomeCategory] being navigated to, since it carries richer data
 * (tag query, display title) than fits cleanly in a route string. Set right
 * before navigating to [Destination.CategoryDetail], read by
 * [com.example.mediabrowser.ui.home.CategoryDetailScreen].
 */
@Singleton
class CategoryNavigationState @Inject constructor() {
    private val _current = MutableStateFlow<HomeCategory?>(null)
    val current: StateFlow<HomeCategory?> = _current.asStateFlow()

    fun set(category: HomeCategory) {
        _current.value = category
    }
}