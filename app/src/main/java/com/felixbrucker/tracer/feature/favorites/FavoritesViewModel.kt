package com.felixbrucker.tracer.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felixbrucker.tracer.core.data.FavoriteRepository
import com.felixbrucker.tracer.core.model.FavoriteTarget
import com.felixbrucker.tracer.core.model.TraceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteTarget>> = favoriteRepository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addFavorite(name: String, host: String, mode: TraceMode) {
        if (host.isBlank()) return
        viewModelScope.launch {
            favoriteRepository.addFavorite(
                FavoriteTarget(
                    name = name.ifBlank { host },
                    host = host.trim(),
                    defaultMode = mode
                )
            )
        }
    }

    fun removeFavorite(target: FavoriteTarget) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(target)
        }
    }
}
