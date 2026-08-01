package com.xiaozhi.smarthome

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class HaUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val entities: List<HaEntity> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val favoriteEntities: List<HaEntity> = emptyList(),
    val selectedRoom: String? = null,
    val connectionStatus: String = "Disconnected"
)

class HomeAssistantViewModel(private val context: Context) : ViewModel() {
    private val manager = HomeAssistantManager.getInstance(context)
    
    private val _uiState = MutableStateFlow(HaUiState())
    val uiState: StateFlow<HaUiState> = _uiState.asStateFlow()
    
    fun connect(url: String, token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = manager.connect(url, token)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        isLoading = false,
                        connectionStatus = "Connected"
                    )
                    loadEntities()
                    loadRooms()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to connect to Home Assistant"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun loadEntities() {
        viewModelScope.launch {
            try {
                val entities = manager.getAllEntities()
                _uiState.value = _uiState.value.copy(entities = entities)
            } catch (e: Exception) {
                Log.e("HaViewModel", "Error loading entities", e)
            }
        }
    }
    
    fun loadRooms() {
        viewModelScope.launch {
            try {
                val rooms = manager.getAllRooms()
                _uiState.value = _uiState.value.copy(rooms = rooms)
            } catch (e: Exception) {
                Log.e("HaViewModel", "Error loading rooms", e)
            }
        }
    }
    
    fun selectRoom(roomName: String?) {
        _uiState.value = _uiState.value.copy(selectedRoom = roomName)
    }
    
    fun toggleEntity(entity: HaEntity) {
        viewModelScope.launch {
            try {
                manager.toggleEntity(entity)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle entity: ${e.message}"
                )
            }
        }
    }
    
    fun setEntityBrightness(entity: HaEntity, brightness: Int) {
        viewModelScope.launch {
            try {
                manager.setLightBrightness(entity.entityId, brightness)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to set brightness: ${e.message}"
                )
            }
        }
    }
    
    fun setEntityTemperature(entity: HaEntity, temperature: Float) {
        viewModelScope.launch {
            try {
                manager.setTemperature(entity.entityId, temperature)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to set temperature: ${e.message}"
                )
            }
        }
    }
    
    fun addToFavorites(entity: HaEntity) {
        val current = _uiState.value.favoriteEntities.toMutableList()
        if (!current.any { it.entityId == entity.entityId }) {
            current.add(entity)
            _uiState.value = _uiState.value.copy(favoriteEntities = current)
        }
    }
    
    fun removeFromFavorites(entity: HaEntity) {
        val current = _uiState.value.favoriteEntities.toMutableList()
        current.removeAll { it.entityId == entity.entityId }
        _uiState.value = _uiState.value.copy(favoriteEntities = current)
    }
}
