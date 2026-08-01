package com.xiaozhi.hass.device

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,          // esp32, shelly, sonoff, v.v.
    val model: String? = null,
    val manufacturer: String? = null,
    val firmwareVersion: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    var isOnline: Boolean = false,
    val config: String = "{}"  // JSON chứa cấu hình riêng của thiết bị
)