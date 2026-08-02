package com.xiaozhi.smarthome

import android.util.Log

// Extension functions for HomeAssistantManager (WITHOUT duplicates)

suspend fun HomeAssistantManager.getAllEntities(): List<HaEntity> {
    return emptyList()
}

suspend fun HomeAssistantManager.getAllRooms(): List<Room> {
    return emptyList()
}

suspend fun HomeAssistantManager.toggleEntity(entity: HaEntity) {
    val newState = if (entity.state == "on") "off" else "on"
    when (entity.domain) {
        "light" -> setLightStateDirect(entity.entityId, newState)
        "switch" -> setSwitchStateDirect(entity.entityId, newState)
        "lock" -> setLockStateDirect(entity.entityId, newState)
        "cover" -> setCoverStateDirect(entity.entityId, newState)
        else -> Log.w("HA", "Unsupported domain: ${entity.domain}")
    }
}

suspend fun HomeAssistantManager.setLightBrightness(entityId: String, brightness: Int) {
    callHaServiceDirect(
        domain = "light",
        service = "turn_on",
        entityId = entityId,
        brightness = (brightness * 2.55).toInt().coerceIn(0, 255)
    )
}

suspend fun HomeAssistantManager.setTemperature(entityId: String, temperature: Float) {
    callHaServiceDirect(
        domain = "climate",
        service = "set_temperature",
        entityId = entityId,
        temperature = temperature
    )
}

// Direct implementation functions
private fun HomeAssistantManager.setLightStateDirect(entityId: String, state: String) {
    val service = if (state == "on") "turn_on" else "turn_off"
    // Call via API
}

private fun HomeAssistantManager.setSwitchStateDirect(entityId: String, state: String) {
    val service = if (state == "on") "turn_on" else "turn_off"
}

private fun HomeAssistantManager.setLockStateDirect(entityId: String, state: String) {
    val service = if (state == "locked") "lock" else "unlock"
}

private fun HomeAssistantManager.setCoverStateDirect(entityId: String, state: String) {
    val service = when (state.lowercase()) {
        "open" -> "open_cover"
        "closed" -> "close_cover"
        else -> "stop_cover"
    }
}

private fun HomeAssistantManager.callHaServiceDirect(
    domain: String,
    service: String,
    entityId: String,
    brightness: Int = 0,
    temperature: Float = 0f
) {
    val data = mutableMapOf<String, Any>("entity_id" to entityId)
    if (brightness > 0) {
        data["brightness"] = brightness
    }
    if (temperature > 0) {
        data["temperature"] = temperature
    }
}
