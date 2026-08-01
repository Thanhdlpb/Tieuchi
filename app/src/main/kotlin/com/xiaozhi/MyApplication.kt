package com.xiaozhi

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import com.xiaozhi.hass.entity.EntityType
import com.xiaozhi.hass.manager.EntityManager
import com.xiaozhi.hass.manager.*
import com.xiaozhi.smarthome.HomeAssistantManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.*
import java.util.concurrent.TimeUnit

// ==================== Discovery Engine Data Models ====================
enum class Protocol { BLE, MDNS, SSDP, MATTER, MQTT, ESP_NOW, TCP, HTTP }

data class DeviceInfo(
    val id: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val protocol: Protocol,
    val ip: String?,
    val mac: String?,
    val firmware: String?,
    val capabilities: List<Capability>,
    val online: Boolean,
    val signal: Int,
    val extra: Map<String, Any> = emptyMap()
)

enum class Capability {
    SWITCH, ON_OFF, BRIGHTNESS, COLOR_TEMP, COLOR,
    TEMPERATURE, HUMIDITY, MOTION, CAMERA, MICROPHONE, SPEAKER,
    OTA, SCENE, LOCK, FAN_SPEED, MEDIA_PLAY, MEDIA_PAUSE,
    MEDIA_NEXT, MEDIA_PREVIOUS, VOLUME_SET,
    TV, REMOTE, CHANNEL, INPUT_SOURCE, POWER
}

sealed interface DeviceEvent {
    data class Online(val id: String) : DeviceEvent
    data class StateChanged(val id: String, val state: String) : DeviceEvent
    data class CapabilitiesUpdated(val id: String, val caps: List<Capability>) : DeviceEvent
}

object EventBus {
    private val _events = MutableSharedFlow<DeviceEvent>()
    val events = _events.asSharedFlow()
    suspend fun emit(event: DeviceEvent) { _events.emit(event) }
}

interface DiscoveryPlugin {
    suspend fun scan(): List<DeviceInfo>
}

object DeviceResolver {
    fun resolve(devices: List<DeviceInfo>): List<DeviceInfo> {
        val grouped = devices.groupBy { it.mac ?: it.id }
        return grouped.mapNotNull { (_, list) ->
            if (list.isEmpty()) return@mapNotNull null
            list.reduce { acc, info ->
                acc.copy(
                    name = if (info.name.isNotBlank() && info.name != "Unknown") info.name else acc.name,
                    manufacturer = if (info.manufacturer.isNotBlank() && info.manufacturer != "Generic") info.manufacturer else acc.manufacturer,
                    model = if (info.model.isNotBlank() && info.model != "Unknown") info.model else acc.model,
                    firmware = info.firmware ?: acc.firmware,
                    capabilities = (acc.capabilities + info.capabilities).distinct(),
                    online = acc.online || info.online,
                    signal = maxOf(acc.signal, info.signal),
                    extra = acc.extra + info.extra
                )
            }
        }
    }
}

// ==================== MyApplication ====================
class MyApplication : Application() {
    lateinit var audioManager: XiaoZhiAudioManager
    lateinit var entityManager: EntityManager
    lateinit var deviceManager: DeviceManager
    lateinit var automationManager: AutomationManager
    lateinit var mqttManager: MqttManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var discoveryManager: DiscoveryManager

    override fun onCreate() {
        super.onCreate()

        ShizukuManager.init(this)

        audioManager = XiaoZhiAudioManager(this)
        audioManager.start()

        entityManager = EntityManager(this)
        deviceManager = DeviceManager(this)
        automationManager = AutomationManager(this)
        mqttManager = MqttManager(this)

        val haManager = HomeAssistantManager.getInstance(this, entityManager)

        // ===== Khởi tạo Discovery Engine =====
        val plugins = listOf(
            BleDiscoveryPlugin(this),
            MdnsDiscoveryPlugin(this),
            SsdpDiscoveryPlugin(this),
            HttpDiscoveryPlugin(this)
        )
        discoveryManager = DiscoveryManager(plugins)

        // ===== Lắng nghe sự kiện thiết bị =====
        appScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is DeviceEvent.Online -> {
                        Log.d("MyApp", "Device online: ${event.id}")
                    }
                    is DeviceEvent.StateChanged -> {
                        Log.d("MyApp", "State changed: ${event.id} -> ${event.state}")
                        entityManager.updateState(event.id, event.state)
                    }
                    is DeviceEvent.CapabilitiesUpdated -> {
                        Log.d("MyApp", "Capabilities updated: ${event.id}")
                    }
                }
            }
        }

        // ===== Tự động phát hiện thiết bị =====
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            delay(5000)
            performDiscovery()
        }

        // ===== Kết nối WebSocket =====
        if (AppState.isActivated(this)) {
            val deviceId = AppState.getDeviceId(this)
            val clientId = AppState.getClientId(this)
            val wsUrl = AppState.getWssUrl(this)
            val wsToken = AppState.getWsToken(this)
            if (!wsUrl.isNullOrEmpty() && !wsToken.isNullOrEmpty()) {
                val wsManager = WebSocketManager(wsUrl, wsToken, deviceId, clientId)
                WebSocketManager.wsManager = wsManager
                wsManager.connect()
                Log.i("MyApp", "WebSocket initialized")
            }
        }

        // ===== Kết nối Home Assistant =====
        val haUrl = AppState.getHaUrl(this)
        val haToken = AppState.getHaToken(this)
        if (!haUrl.isNullOrEmpty() && !haToken.isNullOrEmpty()) {
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                val connected = haManager.connect(haUrl, haToken)
                if (connected) Log.i("MyApp", "✅ Kết nối HA thành công")
            }
        }

        // ===== Tự động kết nối MQTT =====
        appScope.launch {
            delay(8000)
            autoConnectMqtt()
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
        }
    }

    // ==================== Discovery Manager ====================
    inner class DiscoveryManager(private val plugins: List<DiscoveryPlugin>) {
        suspend fun discover(): List<DeviceInfo> = coroutineScope {
            plugins.map { async { it.scan() } }
                .awaitAll()
                .flatten()
        }
    }

    // ==================== Discovery Methods ====================
    private suspend fun performDiscovery() {
        val discovered = discoveryManager.discover()
        val resolved = DeviceResolver.resolve(discovered)

        for (device in resolved) {
            val connectionInfo = device.extra["connectionInfo"] as? Map<*, *>
            val finalConnectionInfo = if (connectionInfo == null || connectionInfo.isEmpty()) {
                val baseUrl = if (device.ip != null) "http://${device.ip}:80" else null
                val endpoints = mapOf(
                    "state" to "$baseUrl/state",
                    "power" to "$baseUrl/power",
                    "volume" to "$baseUrl/volume",
                    "channel" to "$baseUrl/channel",
                    "play" to "$baseUrl/play",
                    "pause" to "$baseUrl/pause",
                    "next" to "$baseUrl/next"
                )
                mapOf(
                    "protocol" to "http",
                    "baseUrl" to baseUrl,
                    "manufacturer" to device.manufacturer,
                    "endpoints" to endpoints
                )
            } else connectionInfo

            val extra = device.extra.toMutableMap()
            extra["connectionInfo"] = finalConnectionInfo

            val updatedDevice = device.copy(extra = extra)

            entityManager.createOrUpdateDiscoveredEntity(
                deviceId = updatedDevice.id,
                name = updatedDevice.name,
                type = mapCapabilitiesToEntityType(updatedDevice.capabilities),
                manufacturer = updatedDevice.manufacturer,
                model = updatedDevice.model,
                ip = updatedDevice.ip,
                extraConfig = mapOf(
                    "capabilities" to updatedDevice.capabilities.map { it.name },
                    "protocol" to updatedDevice.protocol.name,
                    "mac" to (updatedDevice.mac ?: "unknown"),
                    "firmware" to (updatedDevice.firmware ?: "unknown"),
                    "signal" to updatedDevice.signal,
                    "connectionInfo" to (updatedDevice.extra["connectionInfo"] ?: emptyMap<String, Any>())
                )
            )
        }
        Log.i("MyApp", "✅ Discovery completed, found ${resolved.size} devices")
    }

    private fun mapCapabilitiesToEntityType(caps: List<Capability>): EntityType {
        return when {
            caps.contains(Capability.CAMERA) -> EntityType.CAMERA
            caps.contains(Capability.TEMPERATURE) || caps.contains(Capability.HUMIDITY) -> EntityType.SENSOR
            caps.contains(Capability.SWITCH) || caps.contains(Capability.ON_OFF) -> EntityType.SWITCH
            caps.contains(Capability.BRIGHTNESS) -> EntityType.LIGHT
            caps.contains(Capability.MEDIA_PLAY) -> EntityType.MEDIA_PLAYER
            caps.contains(Capability.TV) -> EntityType.UNKNOWN
            else -> EntityType.UNKNOWN
        }
    }

    // ==================== PLUGINS ====================

    // ----- BLE Discovery -----
    inner class BleDiscoveryPlugin(private val context: Context) : DiscoveryPlugin {
        override suspend fun scan(): List<DeviceInfo> = withContext(Dispatchers.IO) {
            val devices = mutableListOf<DeviceInfo>()
            try {
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) return@withContext emptyList()

                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) return@withContext emptyList()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val device = result.device
                        val name = device.name ?: "Unknown BLE"
                        val address = device.address
                        val caps = when {
                            name.contains("Light", ignoreCase = true) -> listOf(Capability.ON_OFF, Capability.BRIGHTNESS)
                            name.contains("Sensor", ignoreCase = true) -> listOf(Capability.TEMPERATURE, Capability.HUMIDITY)
                            name.contains("Switch", ignoreCase = true) -> listOf(Capability.SWITCH)
                            name.contains("Mijia", ignoreCase = true) -> listOf(Capability.TEMPERATURE, Capability.HUMIDITY)
                            name.contains("Philips", ignoreCase = true) -> listOf(Capability.ON_OFF, Capability.BRIGHTNESS)
                            else -> listOf(Capability.ON_OFF)
                        }
                        val info = DeviceInfo(
                            id = address,
                            name = name,
                            manufacturer = if (name.contains("Mijia")) "Xiaomi" else "Generic",
                            model = if (name.contains("Philips")) "Hue" else "BLE",
                            protocol = Protocol.BLE,
                            ip = null,
                            mac = address,
                            firmware = null,
                            capabilities = caps,
                            online = true,
                            signal = result.rssi,
                            extra = mapOf("connectionInfo" to mapOf("protocol" to "ble", "mac" to address))
                        )
                        devices.add(info)
                    }

                    override fun onScanFailed(errorCode: Int) {}
                }

                val scanner = adapter.bluetoothLeScanner
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                scanner.startScan(null, settings, scanCallback)
                delay(5000)
                scanner.stopScan(scanCallback)

                return@withContext devices.distinctBy { it.id }
            } catch (e: Exception) {
                Log.e("BleDiscovery", "Error", e)
                return@withContext emptyList()
            }
        }
    }

    // ----- mDNS Discovery -----
    inner class MdnsDiscoveryPlugin(private val context: Context) : DiscoveryPlugin {
        override suspend fun scan(): List<DeviceInfo> = withContext(Dispatchers.IO) {
            val devices = mutableListOf<DeviceInfo>()
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 2000

                val services = listOf(
                    "_hue._tcp.local." to "Philips Hue",
                    "_miio._udp.local." to "Xiaomi",
                    "_sonoff._tcp.local." to "Sonoff",
                    "_homekit._tcp.local." to "Apple",
                    "_googlecast._tcp.local." to "Google"
                )

                for ((service, manufacturer) in services) {
                    val query = buildMdnsQuery(service)
                    val packet = DatagramPacket(query, query.size, InetAddress.getByName("224.0.0.251"), 5353)
                    socket.send(packet)
                }

                while (true) {
                    val buffer = ByteArray(1024)
                    val recvPacket = DatagramPacket(buffer, buffer.size)
                    try { socket.receive(recvPacket) } catch (e: SocketTimeoutException) { break }
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    val ip = recvPacket.address.hostAddress ?: continue
                    val name = extractMdnsName(response) ?: "mDNS Device"
                    val caps = when {
                        name.contains("Hue") -> listOf(Capability.ON_OFF, Capability.BRIGHTNESS)
                        name.contains("Mi") -> listOf(Capability.TEMPERATURE, Capability.HUMIDITY)
                        name.contains("Sonoff") -> listOf(Capability.SWITCH)
                        else -> listOf(Capability.ON_OFF)
                    }
                    val manufacturer = services.find { response.contains(it.first) }?.second ?: "Generic"
                    devices.add(
                        DeviceInfo(
                            id = "$manufacturer-$ip",
                            name = name,
                            manufacturer = manufacturer,
                            model = "mDNS",
                            protocol = Protocol.MDNS,
                            ip = ip,
                            mac = null,
                            firmware = null,
                            capabilities = caps,
                            online = true,
                            signal = 0,
                            extra = mapOf("connectionInfo" to mapOf("protocol" to "mdns", "ip" to ip))
                        )
                    )
                }
                socket.close()
            } catch (e: Exception) {
                Log.e("MdnsDiscovery", "Error", e)
            }
            return@withContext devices.distinctBy { it.id }
        }

        private fun buildMdnsQuery(service: String): ByteArray {
            val parts = service.split(".")
            val nameBytes = parts.flatMap {
                val bytes = it.toByteArray()
                listOf(bytes.size.toByte()) + bytes.toList()
            }.toByteArray()

            val baos = ByteArrayOutputStream()
            baos.write(0x00); baos.write(0x00)
            baos.write(0x01); baos.write(0x00)
            baos.write(0x00); baos.write(0x01)
            baos.write(0x00); baos.write(0x00)
            baos.write(0x00); baos.write(0x00)
            baos.write(0x00); baos.write(0x00)
            baos.write(nameBytes)
            baos.write(0x00)
            baos.write(0x00); baos.write(0x0C)
            baos.write(0x00); baos.write(0x01)
            return baos.toByteArray()
        }

        private fun extractMdnsName(response: String): String? {
            return Regex("name=([^\\s,]+)").find(response)?.groupValues?.get(1)
        }
    }

    // ----- SSDP Discovery -----
    inner class SsdpDiscoveryPlugin(private val context: Context) : DiscoveryPlugin {
        override suspend fun scan(): List<DeviceInfo> = withContext(Dispatchers.IO) {
            val devices = mutableListOf<DeviceInfo>()
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 2000
                val request = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: ssdp:all\r\n\r\n"
                val packet = DatagramPacket(request.toByteArray(), request.length, InetAddress.getByName("239.255.255.250"), 1900)
                socket.send(packet)

                while (true) {
                    val buffer = ByteArray(1024)
                    val recvPacket = DatagramPacket(buffer, buffer.size)
                    try { socket.receive(recvPacket) } catch (e: SocketTimeoutException) { break }
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    val headers = response.lines().mapNotNull {
                        val parts = it.split(":", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                    }.associate { it }
                    val server = headers["SERVER"] ?: continue
                    val ip = recvPacket.address.hostAddress ?: continue
                    val manufacturer = when {
                        server.contains("Sonoff") -> "Sonoff"
                        server.contains("TP-Link") -> "TP-Link"
                        server.contains("Xiaomi") -> "Xiaomi"
                        server.contains("ESP") -> "ESP8266"
                        else -> "Generic"
                    }
                    devices.add(
                        DeviceInfo(
                            id = "$manufacturer-$ip",
                            name = "UPnP Device",
                            manufacturer = manufacturer,
                            model = "SSDP",
                            protocol = Protocol.SSDP,
                            ip = ip,
                            mac = null,
                            firmware = null,
                            capabilities = listOf(Capability.ON_OFF),
                            online = true,
                            signal = 0,
                            extra = mapOf("connectionInfo" to mapOf("protocol" to "ssdp", "ip" to ip))
                        )
                    )
                }
                socket.close()
            } catch (e: Exception) {
                Log.e("SsdpDiscovery", "Error", e)
            }
            return@withContext devices.distinctBy { it.id }
        }
    }

    // ----- HTTP Discovery -----
    inner class HttpDiscoveryPlugin(private val context: Context) : DiscoveryPlugin {
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        override suspend fun scan(): List<DeviceInfo> = withContext(Dispatchers.IO) {
            val devices = mutableListOf<DeviceInfo>()
            try {
                val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val ip = wifiManager.connectionInfo.ipAddress
                val ipString = String.format("%d.%d.%d.%d", ip and 0xff, (ip shr 8) and 0xff, (ip shr 16) and 0xff, (ip shr 24) and 0xff)
                val subnet = ipString.substringBeforeLast(".")

                for (i in 1..254) {
                    val ipAddress = "$subnet.$i"
                    run loop@{
                        for (port in listOf(80, 8080, 5000, 8123, 8089, 8001, 8002, 3000)) {
                            try {
                                Socket().use { socket ->
                                    socket.connect(InetSocketAddress(ipAddress, port), 300)
                                    val info = fetchDeviceInfo(ipAddress, port)
                                    if (info != null) {
                                        devices.add(info)
                                        return@loop
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HttpDiscovery", "Error", e)
            }
            return@withContext devices
        }

        private suspend fun fetchDeviceInfo(ip: String, port: Int): DeviceInfo? {
            return withContext(Dispatchers.IO) {
                try {
                    val commonPaths = listOf(
                        "/device", "/info", "/api", "/status", "/api/device",
                        "/description.xml", "/upnp/description.xml", "/xml/device_description.xml"
                    )
                    for (path in commonPaths) {
                        try {
                            val url = "http://$ip:$port$path"
                            val request = Request.Builder().url(url).build()
                            val response = httpClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                val json = response.body?.string()
                                if (json != null) {
                                    val obj = JsonParser.parseString(json).asJsonObject
                                    val name = obj.get("name")?.asString ?: "Device"
                                    var manufacturer = obj.get("manufacturer")?.asString ?: "Generic"
                                    val model = obj.get("model")?.asString ?: "Unknown"
                                    val caps = obj.get("capabilities")?.asJsonArray?.mapNotNull {
                                        try { Capability.valueOf(it.asString) } catch (_: Exception) { null }
                                    } ?: listOf(Capability.ON_OFF)

                                    if (manufacturer == "Generic") {
                                        manufacturer = when {
                                            name.contains("Samsung", ignoreCase = true) ||
                                                    model.contains("Samsung", ignoreCase = true) -> "Samsung"
                                            name.contains("LG", ignoreCase = true) ||
                                                    model.contains("LG", ignoreCase = true) -> "LG"
                                            name.contains("Sony", ignoreCase = true) ||
                                                    model.contains("Sony", ignoreCase = true) -> "Sony"
                                            name.contains("Xiaomi", ignoreCase = true) ||
                                                    model.contains("Xiaomi", ignoreCase = true) -> "Xiaomi"
                                            else -> "Generic"
                                        }
                                    }

                                    val finalCaps = if (manufacturer in listOf("Samsung", "LG", "Sony", "Xiaomi")) {
                                        (caps + Capability.TV + Capability.POWER + Capability.VOLUME_SET + Capability.CHANNEL).distinct()
                                    } else caps

                                    val baseUrl = "http://$ip:$port"
                                    val endpointMap = mutableMapOf<String, String>()
                                    endpointMap["state"] = "$baseUrl/state"
                                    endpointMap["power"] = "$baseUrl/power"
                                    endpointMap["volume"] = "$baseUrl/volume"
                                    endpointMap["channel"] = "$baseUrl/channel"
                                    endpointMap["play"] = "$baseUrl/play"
                                    endpointMap["pause"] = "$baseUrl/pause"
                                    endpointMap["next"] = "$baseUrl/next"

                                    when (manufacturer) {
                                        "Samsung" -> {
                                            endpointMap["power"] = "$baseUrl/api/v2/power"
                                            endpointMap["volume"] = "$baseUrl/api/v2/volume"
                                            endpointMap["channel"] = "$baseUrl/api/v2/channel"
                                            endpointMap["input"] = "$baseUrl/api/v2/input"
                                        }
                                        "LG" -> {
                                            endpointMap["power"] = "$baseUrl/api/control/power"
                                            endpointMap["volume"] = "$baseUrl/api/control/volume"
                                            endpointMap["channel"] = "$baseUrl/api/control/channel"
                                        }
                                        "Sony" -> {
                                            endpointMap["power"] = "$baseUrl/sony/system"
                                            endpointMap["volume"] = "$baseUrl/sony/avContent"
                                        }
                                        "Xiaomi" -> {
                                            endpointMap["power"] = "$baseUrl/mitv/control"
                                            endpointMap["volume"] = "$baseUrl/mitv/status"
                                        }
                                    }

                                    val deviceId = "$manufacturer-$model-$ip"
                                    return@withContext DeviceInfo(
                                        id = deviceId,
                                        name = name,
                                        manufacturer = manufacturer,
                                        model = model,
                                        protocol = Protocol.HTTP,
                                        ip = ip,
                                        mac = null,
                                        firmware = obj.get("firmware")?.asString,
                                        capabilities = finalCaps,
                                        online = true,
                                        signal = 0,
                                        extra = mapOf(
                                            "connectionInfo" to mapOf(
                                                "protocol" to "http",
                                                "baseUrl" to baseUrl,
                                                "port" to port,
                                                "manufacturer" to manufacturer,
                                                "endpoints" to endpointMap
                                            )
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    // ==================== Auto Connect MQTT ====================
    private suspend fun autoConnectMqtt() {
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val ipString = String.format("%d.%d.%d.%d", ip and 0xff, (ip shr 8) and 0xff, (ip shr 16) and 0xff, (ip shr 24) and 0xff)
            val subnet = ipString.substringBeforeLast(".")

            for (i in 1..254) {
                val ipAddress = "$subnet.$i"
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ipAddress, 1883), 300)
                        Log.i("MyApp", "🔌 Found MQTT broker at $ipAddress")
                        mqttManager.connect(
                            serverUri = "tcp://$ipAddress:1883",
                            clientId = "android_${System.currentTimeMillis()}",
                            onConnected = {
                                Log.i("MyApp", "✅ Connected to MQTT broker")
                                mqttManager.subscribe("homeassistant/+/+/+/config")
                            }
                        )
                        return
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("MyApp", "MQTT auto-connect error", e)
        }
    }

    // ==================== Crash Handler ====================
    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        val fullLog = "Thread: ${thread.name}\n${stackTrace}"

        try {
            val crashDir = File(getExternalFilesDir(null), "crash_logs")
            crashDir.mkdirs()
            val crashFile = File(crashDir, "crash_${System.currentTimeMillis()}.txt")
            crashFile.writeText(fullLog)
        } catch (_: Exception) {}

        if (thread == Looper.getMainLooper().thread) {
            val intent = Intent(this, CrashReportActivity::class.java).apply {
                putExtra(CrashReportActivity.EXTRA_STACK_TRACE, fullLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }

    // ==================== Public API ====================
    fun scanDevices(onComplete: (Int) -> Unit = {}) {
        appScope.launch {
            performDiscovery()
            val count = entityManager.getAllEntities().firstOrNull()?.size ?: 0
            withContext(Dispatchers.Main) { onComplete(count) }
        }
    }
}