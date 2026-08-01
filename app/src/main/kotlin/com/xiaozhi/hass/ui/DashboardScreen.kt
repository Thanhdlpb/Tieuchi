package com.xiaozhi.hass.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.xiaozhi.*
import com.xiaozhi.hass.entity.Entity
import com.xiaozhi.hass.manager.EntityManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    entityManager: EntityManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entities by entityManager.getAllEntities().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tieuchi Home") },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isScanning) {
                                isScanning = true
                                val app = context.applicationContext as? MyApplication
                                app?.scanDevices { count ->
                                    isScanning = false
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Icon(
                            if (isScanning) Icons.Filled.Refresh else Icons.Filled.Search,
                            contentDescription = if (isScanning) "Đang quét..." else "Quét thiết bị",
                            tint = if (isScanning) Color(0xFFF59E0B) else Color(0xFF00E5FF)
                        )
                    }
                    IconButton(onClick = { /* Mở cài đặt HASS */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                    }
                }
            )
        }
    ) { padding ->
        if (entities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có thiết bị nào", style = MaterialTheme.typography.titleMedium)
                    Text("Nhấn nút 🔍 để quét thiết bị", style = MaterialTheme.typography.bodySmall)
                    if (isScanning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "📡 ${entities.size} thiết bị",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                items(entities) { entity ->
                    EntityCardDynamic(entity = entity, entityManager = entityManager)
                }
            }
        }
    }
}

private suspend fun sendHttpCommand(
    entity: Entity,
    endpoint: String,
    payload: Map<String, Any> = emptyMap()
): Boolean {
    return try {
        val attrs = JsonParser.parseString(entity.attributes).asJsonObject
        val connectionInfo = attrs.get("connectionInfo")?.asJsonObject ?: return false
        val endpoints = connectionInfo.get("endpoints")?.asJsonObject ?: return false
        val url = endpoints.get(endpoint)?.asString
        if (url.isNullOrEmpty()) return false

        val client = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}

private val gson = com.google.gson.Gson()

@Composable
fun EntityCardDynamic(
    entity: Entity,
    entityManager: EntityManager
) {
    val attrs = try {
        JsonParser.parseString(entity.attributes).asJsonObject
    } catch (_: Exception) { null }

    val caps = try {
        val capsArray = attrs?.get("capabilities")?.asJsonArray
        if (capsArray == null) emptyList()
        else capsArray.mapNotNull {
            try { Capability.valueOf(it.asString) } catch (_: Exception) { null }
        }
    } catch (_: Exception) { emptyList() }

    val manufacturer = attrs?.get("manufacturer")?.asString ?: "Generic"
    val model = attrs?.get("model")?.asString ?: "Unknown"
    val connectionInfo = attrs?.get("connectionInfo")?.asJsonObject
    val baseUrl = connectionInfo?.get("baseUrl")?.asString

    val deviceType = when {
        manufacturer.contains("Samsung", ignoreCase = true) -> DeviceType.SAMSUNG_TV
        manufacturer.contains("LG", ignoreCase = true) -> DeviceType.LG_TV
        manufacturer.contains("Sony", ignoreCase = true) -> DeviceType.SONY_TV
        manufacturer.contains("Xiaomi", ignoreCase = true) -> DeviceType.XIAOMI_TV
        caps.contains(Capability.CAMERA) -> DeviceType.CAMERA
        caps.contains(Capability.TEMPERATURE) || caps.contains(Capability.HUMIDITY) -> DeviceType.SENSOR
        caps.contains(Capability.BRIGHTNESS) -> DeviceType.LIGHT
        caps.contains(Capability.SWITCH) || caps.contains(Capability.ON_OFF) -> DeviceType.SWITCH
        caps.contains(Capability.MEDIA_PLAY) -> DeviceType.MEDIA_PLAYER
        caps.contains(Capability.TV) -> DeviceType.GENERIC_TV
        else -> DeviceType.GENERIC
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entity.state == "on") MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        when (deviceType) {
            DeviceType.SAMSUNG_TV, DeviceType.LG_TV, DeviceType.SONY_TV, DeviceType.XIAOMI_TV, DeviceType.GENERIC_TV ->
                TvCard(entity, entityManager, manufacturer, model, baseUrl)
            DeviceType.CAMERA -> CameraCard(entity, entityManager, baseUrl)
            DeviceType.SENSOR -> SensorCard(entity)
            DeviceType.LIGHT -> LightCard(entity, entityManager, caps)
            DeviceType.SWITCH -> SwitchCard(entity, entityManager)
            DeviceType.MEDIA_PLAYER -> MediaPlayerCard(entity, entityManager)
            else -> GenericCard(entity, entityManager)
        }
    }
}

// ==================== Card TV ====================
@Composable
fun TvCard(
    entity: Entity,
    entityManager: EntityManager,
    manufacturer: String,
    model: String,
    baseUrl: String?
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "📺 ${entity.name}", style = MaterialTheme.typography.titleMedium)
                Text(text = "$manufacturer $model", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                Text(
                    text = "Trạng thái: ${entity.state}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch {
                            val newState = if (entity.state == "on") "off" else "on"
                            val success = sendHttpCommand(entity, "power", mapOf("state" to newState))
                            if (success) entityManager.updateState(entity.id, newState)
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Icon(
                    if (entity.state == "on") Icons.Filled.PowerSettingsNew else Icons.Filled.PowerOff,
                    contentDescription = "Nguồn",
                    tint = if (entity.state == "on") Color(0xFFFF5722) else Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "volume", mapOf("action" to "down")) } }) {
                Icon(Icons.Filled.VolumeDown, contentDescription = "Giảm âm lượng")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "volume", mapOf("action" to "up")) } }) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Tăng âm lượng")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "channel", mapOf("action" to "down")) } }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Kênh trước")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "channel", mapOf("action" to "up")) } }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Kênh sau")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "input", mapOf("source" to "HDMI1")) } }) {
                Icon(Icons.Filled.Input, contentDescription = "Chọn nguồn vào")
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (baseUrl != null) {
            Text(text = "🔗 ${baseUrl.take(50)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
    }
}

// ==================== Các card khác ====================
@Composable
fun CameraCard(entity: Entity, entityManager: EntityManager, baseUrl: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "📷 ${entity.name}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
            }
            Icon(Icons.Filled.Videocam, contentDescription = "Camera")
        }
        if (baseUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* TODO: Mở luồng video */ }, modifier = Modifier.fillMaxWidth()) { Text("Xem luồng") }
        }
    }
}

@Composable
fun SensorCard(entity: Entity) {
    val attrs = try { JsonParser.parseString(entity.attributes).asJsonObject } catch (_: Exception) { null }
    val temperature = attrs?.get("temperature")?.asDouble
    val humidity = attrs?.get("humidity")?.asDouble

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "🌡️ ${entity.name}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
            }
            Icon(Icons.Filled.Thermostat, contentDescription = "Cảm biến")
        }
        if (temperature != null || humidity != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (temperature != null) Text("🌡️ ${temperature}°C", style = MaterialTheme.typography.bodyMedium)
                if (humidity != null) Text("💧 ${humidity}%", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LightCard(entity: Entity, entityManager: EntityManager, caps: List<Capability>) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "💡 ${entity.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (caps.contains(Capability.BRIGHTNESS)) Icon(Icons.Filled.Lightbulb, contentDescription = "Đèn")
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch {
                            val newState = if (entity.state == "on") "off" else "on"
                            if (sendHttpCommand(entity, "state", mapOf("state" to newState))) {
                                entityManager.updateState(entity.id, newState)
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) { Text(if (entity.state == "on") "Tắt" else "Bật") }
        }
    }
}

@Composable
fun SwitchCard(entity: Entity, entityManager: EntityManager) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "🔌 ${entity.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
        }
        Button(
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    scope.launch {
                        val newState = if (entity.state == "on") "off" else "on"
                        if (sendHttpCommand(entity, "state", mapOf("state" to newState))) {
                            entityManager.updateState(entity.id, newState)
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) { Text(if (entity.state == "on") "Tắt" else "Bật") }
    }
}

@Composable
fun MediaPlayerCard(entity: Entity, entityManager: EntityManager) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "🎵 ${entity.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
        }
        Row {
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "play", emptyMap()) } }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "pause", emptyMap()) } }) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause")
            }
            IconButton(onClick = { if (!isLoading) scope.launch { sendHttpCommand(entity, "next", emptyMap()) } }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun GenericCard(entity: Entity, entityManager: EntityManager) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "🛠️ ${entity.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Trạng thái: ${entity.state}", style = MaterialTheme.typography.bodySmall, color = if (entity.state == "on") Color(0xFF4ADE80) else Color(0xFF94A3B8))
        }
        if (entity.state in listOf("on", "off")) {
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch {
                            val newState = if (entity.state == "on") "off" else "on"
                            if (sendHttpCommand(entity, "state", mapOf("state" to newState))) {
                                entityManager.updateState(entity.id, newState)
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) { Text(if (entity.state == "on") "Tắt" else "Bật") }
        }
    }
}

// ==================== Enum DeviceType ====================
enum class DeviceType {
    SAMSUNG_TV, LG_TV, SONY_TV, XIAOMI_TV, GENERIC_TV,
    CAMERA, SENSOR, LIGHT, SWITCH, MEDIA_PLAYER, GENERIC
}