package com.xiaozhi.smarthome

import android.content.Context
import android.util.Log
import com.xiaozhi.AppState
import com.xiaozhi.hass.entity.EntityType
import com.xiaozhi.hass.manager.EntityManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class HomeAssistantManager private constructor(
    private val context: Context,
    private val entityManager: EntityManager? = null
) {
    companion object {
        @Volatile private var instance: HomeAssistantManager? = null
        fun getInstance(context: Context, entityManager: EntityManager? = null): HomeAssistantManager {
            return instance ?: synchronized(this) {
                instance ?: HomeAssistantManager(context.applicationContext, entityManager).also { instance = it }
            }
        }
        private const val TAG = "HomeAssistantManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var api: HomeAssistantApi? = null
    private var webSocketClient: HaWebSocketClient? = null
    private val entityCache = ConcurrentHashMap<String, HaEntity>()
    private var haUrl: String? = null
    private var haToken: String? = null

    suspend fun connect(url: String, token: String): Boolean {
        if (!url.startsWith("http")) return false
        AppState.setHaUrl(context, url)
        AppState.setHaToken(context, token)
        AppState.setHaEnabled(context, true)
        this.haUrl = url
        this.haToken = token

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(HaAuthInterceptor(token))
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl(if (url.endsWith("/")) url else "$url/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            api = retrofit.create(HomeAssistantApi::class.java)

            val configResponse = api?.getConfig()
            if (configResponse?.isSuccessful == true) {
                Log.i(TAG, "HA Config fetched successfully")
                startWebSocket(url, token)
                refreshCache()
                entityManager?.let { syncEntitiesToLocal(it) }
                true
            } else {
                Log.e(TAG, "Failed to fetch HA config. Response: ${configResponse?.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Home Assistant", e)
            false
        }
    }

    private fun startWebSocket(baseUrl: String, token: String) {
        val wsUrl = baseUrl.replace("http", "ws") + "api/websocket"
        webSocketClient?.close()
        webSocketClient = HaWebSocketClient(wsUrl, token) { entity ->
            // ✅ Sửa: đưa vào coroutine scope
            scope.launch {
                entityCache[entity.entityId] = entity
                entityManager?.let { em ->
                    em.createOrUpdateDiscoveredEntity(
                        deviceId = entity.entityId,
                        name = entity.name,
                        type = mapDomainToEntityType(entity.domain),
                        manufacturer = (entity.attributes["manufacturer"] as? String) ?: "Home Assistant",
                        model = (entity.attributes["model"] as? String) ?: entity.domain,
                        extraConfig = entity.attributes
                    )
                }
            }
        }
        webSocketClient?.connect()
    }

    suspend fun refreshCache() {
        val apiInstance = api ?: return
        val response = try {
            apiInstance.getAllStates()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all states", e)
            return
        }

        if (response.isSuccessful) {
            response.body()?.forEach { entity ->
                entityCache[entity.entityId] = entity
            }
            Log.i(TAG, "Refreshed cache with ${entityCache.size} entities")
        } else {
            Log.e(TAG, "Failed to refresh cache: ${response.code()}")
        }
    }

    private suspend fun syncEntitiesToLocal(em: EntityManager) {
        try {
            val haEntities = getAllEntities()
            for (haEntity in haEntities) {
                em.createOrUpdateDiscoveredEntity(
                    deviceId = haEntity.entityId,
                    name = haEntity.name,
                    type = mapDomainToEntityType(haEntity.domain),
                    manufacturer = (haEntity.attributes["manufacturer"] as? String) ?: "Home Assistant",
                    model = (haEntity.attributes["model"] as? String) ?: haEntity.domain,
                    extraConfig = haEntity.attributes
                )
            }
            Log.i(TAG, "Đã đồng bộ ${haEntities.size} thiết bị từ Home Assistant")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi đồng bộ HA", e)
        }
    }

    private fun mapDomainToEntityType(domain: String): EntityType {
        return when (domain) {
            "light" -> EntityType.LIGHT
            "switch" -> EntityType.SWITCH
            "fan" -> EntityType.FAN
            "sensor" -> EntityType.SENSOR
            "camera" -> EntityType.CAMERA
            "lock" -> EntityType.LOCK
            "climate" -> EntityType.THERMOSTAT
            "vacuum" -> EntityType.VACUUM
            "media_player" -> EntityType.MEDIA_PLAYER
            "scene" -> EntityType.SCENE
            "script" -> EntityType.SCRIPT
            "group" -> EntityType.GROUP
            else -> EntityType.UNKNOWN
        }
    }

    suspend fun callService(domain: String, service: String, entityId: String? = null, data: Map<String, Any> = emptyMap()): Boolean {
        val apiInstance = api ?: return false
        val body = mutableMapOf<String, Any>()
        if (entityId != null) body["entity_id"] = entityId
        body.putAll(data)

        return try {
            val response = apiInstance.callService(domain, service, body)
            if (!response.isSuccessful) {
                Log.e(TAG, "Service call failed: ${response.code()} - ${response.message()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling service $domain.$service", e)
            false
        }
    }

    fun getEntity(entityId: String): HaEntity? = entityCache[entityId]
    fun getAllEntities(): List<HaEntity> = entityCache.values.toList()

    fun disconnect() {
        webSocketClient?.close()
        webSocketClient = null
        api = null
        AppState.setHaEnabled(context, false)
        Log.i(TAG, "Disconnected from Home Assistant")
    }

    fun isConnected(): Boolean = webSocketClient?.isOpen ?: false
}