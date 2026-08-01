package com.xiaozhi.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhi.smarthome.HaEntity
import com.xiaozhi.smarthome.HaUiState
import com.xiaozhi.smarthome.Room

@Composable
fun HomeAssistantScreen(
    uiState: HaUiState,
    onToggleEntity: (HaEntity) -> Unit = {},
    onSetBrightness: (HaEntity, Int) -> Unit = { _, _ -> },
    onSetTemperature: (HaEntity, Float) -> Unit = { _, _ -> },
    onSelectRoom: (String?) -> Unit = {},
    onAddFavorite: (HaEntity) -> Unit = {},
    onRemoveFavorite: (HaEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showConnectionDialog by remember { mutableStateOf(!uiState.isConnected) }
    var selectedEntityForControl by remember { mutableStateOf<HaEntity?>(null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isConnected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0x0B0E14))
            ) {
                // Header
                HomeAssistantHeader(
                    connectionStatus = uiState.connectionStatus,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick Actions / Favorites
                if (uiState.favoriteEntities.isNotEmpty()) {
                    FavoriteEntitiesSection(
                        entities = uiState.favoriteEntities,
                        onToggle = onToggleEntity,
                        onSelect = { selectedEntityForControl = it },
                        onRemoveFavorite = onRemoveFavorite,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Room Selector
                RoomSelectorSection(
                    rooms = uiState.rooms,
                    selectedRoom = uiState.selectedRoom,
                    onSelectRoom = onSelectRoom,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Entities List (filtered by room if selected)
                EntitiesListSection(
                    entities = uiState.entities.filter { entity ->
                        uiState.selectedRoom == null || 
                        uiState.rooms.any { room -> room.name == uiState.selectedRoom && room.entities.contains(entity.entityId) }
                    },
                    onToggle = onToggleEntity,
                    onSelect = { selectedEntityForControl = it },
                    onAddFavorite = onAddFavorite,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Not Connected State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x0B0E14)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ConnectWithoutContact,
                        contentDescription = "Not Connected",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Home Assistant Not Connected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configure your Home Assistant connection in settings",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        // Error Message
        if (!uiState.error.isNullOrEmpty()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                backgroundColor = Color(0xFFB71C1C)
            ) {
                Text(uiState.error, color = Color.White)
            }
        }
        
        // Entity Control Dialog
        selectedEntityForControl?.let { entity ->
            EntityControlDialog(
                entity = entity,
                onDismiss = { selectedEntityForControl = null },
                onToggle = { onToggleEntity(entity) },
                onSetBrightness = { brightness -> onSetBrightness(entity, brightness) },
                onSetTemperature = { temp -> onSetTemperature(entity, temp) }
            )
        }
    }
}

@Composable
fun HomeAssistantHeader(
    connectionStatus: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0x1A237E), Color(0x0B0E14))
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "🏠 Home Assistant",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    connectionStatus,
                    fontSize = 12.sp,
                    color = if (connectionStatus == "Connected") Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun FavoriteEntitiesSection(
    entities: List<HaEntity>,
    onToggle: (HaEntity) -> Unit = {},
    onSelect: (HaEntity) -> Unit = {},
    onRemoveFavorite: (HaEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            "⭐ Favorites",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(entities) { entity ->
                FavoriteEntityCard(
                    entity = entity,
                    onToggle = { onToggle(entity) },
                    onSelect = { onSelect(entity) },
                    onRemove = { onRemoveFavorite(entity) }
                )
            }
        }
    }
}

@Composable
fun FavoriteEntityCard(
    entity: HaEntity,
    onToggle: () -> Unit = {},
    onSelect: () -> Unit = {},
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1F1F2E))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
        border = BorderStroke(1.dp, Color(0x2196F3))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    getEntityIcon(entity.domain),
                    fontSize = 20.sp
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFF9800)
                    )
                }
            }
            
            Text(
                entity.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                entity.state,
                fontSize = 10.sp,
                color = getStateColor(entity.state),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RoomSelectorSection(
    rooms: List<Room>,
    selectedRoom: String?,
    onSelectRoom: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            "🏠 Rooms",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                RoomButton(
                    name = "All",
                    isSelected = selectedRoom == null,
                    onClick = { onSelectRoom(null) }
                )
            }
            items(rooms) { room ->
                RoomButton(
                    name = room.name,
                    isSelected = selectedRoom == room.name,
                    onClick = { onSelectRoom(room.name) },
                    deviceCount = room.entities.size
                )
            }
        }
    }
}

@Composable
fun RoomButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit = {},
    deviceCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0x2F2F3E),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (deviceCount > 0) {
                Text(
                    "$deviceCount",
                    fontSize = 10.sp,
                    color = Color(0xFFBDBDBD)
                )
            }
        }
    }
}

@Composable
fun EntitiesListSection(
    entities: List<HaEntity>,
    onToggle: (HaEntity) -> Unit = {},
    onSelect: (HaEntity) -> Unit = {},
    onAddFavorite: (HaEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            "📱 Devices & Entities",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(entities) { entity ->
                EntityListItem(
                    entity = entity,
                    onToggle = { onToggle(entity) },
                    onSelect = { onSelect(entity) },
                    onAddFavorite = { onAddFavorite(entity) }
                )
            }
        }
    }
}

@Composable
fun EntityListItem(
    entity: HaEntity,
    onToggle: () -> Unit = {},
    onSelect: () -> Unit = {},
    onAddFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
        border = BorderStroke(1.dp, Color(0x353541))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    getEntityIcon(entity.domain),
                    fontSize = 24.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entity.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${entity.domain} • ${entity.state}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAddFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Add to favorites",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
                
                if (entity.domain in listOf("light", "switch", "lock", "cover")) {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (entity.state == "on") Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                            contentDescription = "Toggle",
                            modifier = Modifier.size(20.dp),
                            tint = if (entity.state == "on") Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntityControlDialog(
    entity: HaEntity,
    onDismiss: () -> Unit = {},
    onToggle: () -> Unit = {},
    onSetBrightness: (Int) -> Unit = {},
    onSetTemperature: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var brightnessValue by remember { mutableStateOf(entity.attributes["brightness"]?.toString()?.toIntOrNull()?.div(2.55)?.toInt() ?: 50) }
    var temperatureValue by remember { mutableStateOf(entity.attributes["temperature"]?.toString()?.toFloatOrNull() ?: 20f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        containerColor = Color(0x1F1F2E),
        titleContentColor = Color.White,
        textContentColor = Color.Gray,
        title = {
            Text(
                "${getEntityIcon(entity.domain)} ${entity.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Domain: ${entity.domain}\nState: ${entity.state}",
                    fontSize = 12.sp
                )
                
                when (entity.domain) {
                    "light" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Brightness: $brightnessValue%", fontSize = 12.sp, color = Color.White)
                            Slider(
                                value = brightnessValue.toFloat(),
                                onValueChange = { brightnessValue = it.toInt() },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    "climate" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Temperature: $temperatureValue°C", fontSize = 12.sp, color = Color.White)
                            Slider(
                                value = temperatureValue,
                                onValueChange = { temperatureValue = it },
                                valueRange = 15f..30f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (entity.domain) {
                        "light" -> onSetBrightness(brightnessValue)
                        "climate" -> onSetTemperature(temperatureValue)
                        else -> onToggle()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Apply", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x353541))
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

fun getEntityIcon(domain: String): String = when (domain) {
    "light" -> "💡"
    "switch" -> "🔌"
    "lock" -> "🔒"
    "cover" -> "🪟"
    "climate" -> "🌡️"
    "fan" -> "🌀"
    "media_player" -> "📺"
    "camera" -> "📷"
    "sensor" -> "📊"
    "binary_sensor" -> "🔲"
    else -> "⚙️"
}

fun getStateColor(state: String): Color = when (state.lowercase()) {
    "on" -> Color(0xFF4CAF50)
    "off" -> Color(0xFFFF9800)
    "open" -> Color(0xFF2196F3)
    "closed" -> Color(0xFF4CAF50)
    "locked" -> Color(0xFF4CAF50)
    "unlocked" -> Color(0xFFFF9800)
    else -> Color.Gray
}
