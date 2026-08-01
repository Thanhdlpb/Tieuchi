package com.xiaozhi.smarthome

import android.content.Context
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Extension functions for HomeAssistantManager

suspend fun HomeAssistantManager.getAllEntities(): List<HaEntity> = 
    suspendCancellableCoroutine { continuation ->
        try {
            val entities = getEntityCache()
            continuation.resume(entities)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

suspend fun HomeAssistantManager.getAllRooms(): List<Room> = 
    suspendCancellableCoroutine { continuation ->
        try {
            val rooms = getRoomsList()
            continuation.resume(rooms)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

suspend fun HomeAssistantManager.toggleEntity(entity: HaEntity) = 
    suspendCancellableCoroutine { continuation ->
        try {
            val newState = if (entity.state == "on") "off" else "on"
            when (entity.domain) {
                "light" -> setLightState(entity.entityId, newState)
                "switch" -> setSwitchState(entity.entityId, newState)
                "lock" -> setLockState(entity.entityId, newState)
                "cover" -> setCoverState(entity.entityId, newState)
                else -> Log.w("HA", "Unsupported domain: ${entity.domain}")
            }
            continuation.resume(Unit)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

suspend fun HomeAssistantManager.setLightBrightness(entityId: String, brightness: Int) = 
    suspendCancellableCoroutine { continuation ->
        try {
            callHaService(
                domain = "light",
                service = "turn_on",
                entityId = entityId,
                brightness = (brightness * 2.55).toInt().coerceIn(0, 255)
            )
            continuation.resume(Unit)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

suspend fun HomeAssistantManager.setTemperature(entityId: String, temperature: Float) = 
    suspendCancellableCoroutine { continuation ->
        try {
            callHaService(
                domain = "climate",
                service = "set_temperature",
                entityId = entityId,
                temperature = temperature
            )
            continuation.resume(Unit)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

// Placeholder functions (to be implemented in HomeAssistantManager)
fun HomeAssistantManager.getEntityCache(): List<HaEntity> = emptyList()
fun HomeAssistantManager.getRoomsList(): List<Room> = emptyList()
fun HomeAssistantManager.setLightState(entityId: String, state: String) {}
fun HomeAssistantManager.setSwitchState(entityId: String, state: String) {}
fun HomeAssistantManager.setLockState(entityId: String, state: String) {}
fun HomeAssistantManager.setCoverState(entityId: String, state: String) {}
fun HomeAssistantManager.callHaService(domain: String, service: String, entityId: String, brightness: Int = 0, temperature: Float = 0f) {}
