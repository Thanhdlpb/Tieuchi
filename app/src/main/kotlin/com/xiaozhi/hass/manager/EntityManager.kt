package com.xiaozhi.hass.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.xiaozhi.hass.database.AppDatabase
import com.xiaozhi.hass.entity.Entity
import com.xiaozhi.hass.entity.EntityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class EntityManager(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).entityDao()
    private val gson = Gson()

    fun getAllEntities(): Flow<List<Entity>> = dao.getAll()

    suspend fun getEntity(id: String): Entity? = dao.getById(id)

    suspend fun addEntity(entity: Entity) = dao.insert(entity)

    suspend fun updateEntity(entity: Entity) = dao.update(entity)

    suspend fun deleteEntity(entity: Entity) = dao.delete(entity)

    suspend fun updateState(id: String, state: String) =
        dao.updateState(id, state)

    fun getByArea(areaId: String): Flow<List<Entity>> = dao.getByAreaId(areaId)

    /**
     * Tạo hoặc cập nhật entity với thông tin kết nối đầy đủ
     */
    suspend fun createOrUpdateDiscoveredEntity(
        deviceId: String,
        name: String,
        type: EntityType,
        manufacturer: String? = null,
        model: String? = null,
        ip: String? = null,
        port: Int? = null,
        extraConfig: Map<String, Any>? = null
    ): Entity {
        val existing = dao.getById(deviceId)
        val attributesJson = buildAttributes(manufacturer, model, ip, port, extraConfig)

        if (existing != null) {
            val updated = existing.copy(
                name = name,
                attributes = attributesJson
            )
            dao.update(updated)
            return updated
        }

        val entity = Entity(
            id = deviceId,
            name = name,
            type = type,
            deviceId = null,
            areaId = null,
            attributes = attributesJson,
            state = "unknown"
        )
        dao.insert(entity)
        return entity
    }

    /**
     * Xây dựng chuỗi JSON attributes từ thông tin thiết bị
     */
    private fun buildAttributes(
        manufacturer: String?,
        model: String?,
        ip: String?,
        port: Int?,
        extraConfig: Map<String, Any>?
    ): String {
        val json = JsonObject()
        manufacturer?.let { json.addProperty("manufacturer", it) }
        model?.let { json.addProperty("model", it) }
        ip?.let { json.addProperty("ip", it) }
        port?.let { json.addProperty("port", it) }

        extraConfig?.forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> json.add(key, gson.toJsonTree(value))
                is List<*> -> json.add(key, gson.toJsonTree(value))
                is String -> json.addProperty(key, value)
                is Number -> json.addProperty(key, value)
                is Boolean -> json.addProperty(key, value)
                else -> json.addProperty(key, value.toString())
            }
        }
        return gson.toJson(json)
    }

    /**
     * Xóa tất cả entity (dùng khi reset)
     */
    suspend fun deleteAllEntities() {
        val entities = dao.getAll().firstOrNull() ?: return
        entities.forEach { dao.delete(it) }
    }

    // Helper methods
    suspend fun createLight(id: String, name: String, areaId: String? = null, deviceId: String? = null): Entity {
        val entity = Entity(
            id = id,
            name = name,
            type = EntityType.LIGHT,
            areaId = areaId,
            deviceId = deviceId,
            state = "off"
        )
        addEntity(entity)
        return entity
    }

    suspend fun createSwitch(id: String, name: String, areaId: String? = null, deviceId: String? = null): Entity {
        val entity = Entity(
            id = id,
            name = name,
            type = EntityType.SWITCH,
            areaId = areaId,
            deviceId = deviceId,
            state = "off"
        )
        addEntity(entity)
        return entity
    }

    suspend fun createSensor(id: String, name: String, areaId: String? = null, deviceId: String? = null): Entity {
        val entity = Entity(
            id = id,
            name = name,
            type = EntityType.SENSOR,
            areaId = areaId,
            deviceId = deviceId,
            state = "unknown"
        )
        addEntity(entity)
        return entity
    }
}