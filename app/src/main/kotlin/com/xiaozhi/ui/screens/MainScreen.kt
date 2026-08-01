package com.xiaozhi.ui.screens

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.xiaozhi.AIState
import com.xiaozhi.MainUiState
import com.xiaozhi.Message
import com.xiaozhi.VideoPlayerManager

@Composable
fun MainScreen(
    uiState: MainUiState,
    onInputTextChange: (String) -> Unit = {},
    onSendTextMessage: () -> Unit = {},
    onToggleMic: () -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit = {},
    onSelectImage: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleMcpMusic: () -> Unit = {},
    onToggleMcpVideo: () -> Unit = {},
    onLoginLogout: () -> Unit = {},
    onCopyActivationCode: () -> Unit = {},
    videoPlayerManager: VideoPlayerManager,
    playerView: PlayerView,
    previewViewForEye: PreviewView,
    isMicActive: Boolean = false,
    isMusicPlaying: Boolean = false,
    isVideoPlaying: Boolean = false,
    currentEmotionGif: String = "neutral",
    currentSongTitle: String = "",
    onPrevious: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onRequestRecordAudioPermission: () -> Unit = {},
    waveformAmplitudes: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 0)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x0B0E14))
    ) {
        // Main Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ChatScreen(
                    messages = uiState.messages,
                    inputText = uiState.inputText,
                    onInputTextChange = onInputTextChange,
                    onSendMessage = onSendTextMessage,
                    aiState = uiState.aiState,
                    isMicActive = isMicActive,
                    isAlbumArtVisible = uiState.isAlbumArtVisible,
                    albumArtUrl = uiState.albumArtUrl,
                    currentSongTitle = uiState.currentSongTitle,
                    isVideoVisible = uiState.isVideoVisible,
                    videoPlayerManager = videoPlayerManager,
                    playerView = playerView,
                    previewViewForEye = previewViewForEye,
                    currentEmotionGif = currentEmotionGif,
                    waveformAmplitudes = waveformAmplitudes,
                    onToggleMic = onToggleMic,
                    onRequestRecordAudioPermission = onRequestRecordAudioPermission,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    batteryPercent = uiState.batteryPercent
                )
                1 -> HomeAssistantScreen(
                    uiState = com.xiaozhi.smarthome.HaUiState(),
                    modifier = Modifier.fillMaxSize()
                )
                2 -> SettingsMenuScreen(
                    isMenuOpen = uiState.isMenuOpen,
                    onMenuOpenChange = onMenuOpenChange,
                    onSettingsClick = onSettingsClick,
                    onToggleMcpMusic = onToggleMcpMusic,
                    onToggleMcpVideo = onToggleMcpVideo,
                    onLoginLogout = onLoginLogout,
                    onCopyActivationCode = onCopyActivationCode,
                    isActivationCardVisible = uiState.isActivationCardVisible,
                    activationPinCode = uiState.activationPinCode,
                    batteryPercent = uiState.batteryPercent
                )
            }
        }
        
        // Bottom Navigation
        BottomNavigationBar(
            currentPage = pagerState.currentPage,
            onPageSelected = { page ->
                // Handle page navigation via LaunchedEffect
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ChatScreen(
    messages: List<Message>,
    inputText: String,
    onInputTextChange: (String) -> Unit = {},
    onSendMessage: () -> Unit = {},
    aiState: AIState = AIState.IDLE,
    isMicActive: Boolean = false,
    isAlbumArtVisible: Boolean = false,
    albumArtUrl: String? = null,
    currentSongTitle: String = "",
    isVideoVisible: Boolean = false,
    videoPlayerManager: VideoPlayerManager? = null,
    playerView: PlayerView? = null,
    previewViewForEye: PreviewView? = null,
    currentEmotionGif: String = "neutral",
    waveformAmplitudes: List<Float> = emptyList(),
    onToggleMic: () -> Unit = {},
    onRequestRecordAudioPermission: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    batteryPercent: Int = 100,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x0B0E14))
        ) {
            // Header
            ChatHeader(batteryPercent = batteryPercent)
            
            // Video/Album Art
            if (isVideoVisible && playerView != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { playerView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (isAlbumArtVisible && !albumArtUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0x1F1F2E))
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(albumArtUrl),
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // Emotion GIF or Waveform
                EmotionDisplay(
                    emotion = currentEmotionGif,
                    waveformAmplitudes = waveformAmplitudes,
                    aiState = aiState,
                    isMicActive = isMicActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
            
            // Messages
            LazyMessageList(
                messages = messages,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Media Controls (if music playing)
            if (currentSongTitle.isNotEmpty()) {
                MediaControlsBar(
                    songTitle = currentSongTitle,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    waveformAmplitudes = waveformAmplitudes,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Input
            MessageInputArea(
                text = inputText,
                onTextChange = onInputTextChange,
                onSend = onSendMessage,
                onMicToggle = onToggleMic,
                isMicActive = isMicActive,
                aiState = aiState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChatHeader(
    batteryPercent: Int = 100,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0x1A237E), Color(0x0B0E14))
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "🤖 XiaoZhi AI Assistant",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "🟢 Ready to assist",
                fontSize = 12.sp,
                color = Color(0xFF4CAF50)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Battery3Bar,
                contentDescription = "Battery",
                tint = if (batteryPercent > 30) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Text(
                "$batteryPercent%",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun EmotionDisplay(
    emotion: String,
    waveformAmplitudes: List<Float>,
    aiState: AIState,
    isMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0x1F1F2E))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Emotion Emoji
            Text(
                when (emotion.lowercase()) {
                    "happy" -> "😊"
                    "sad" -> "😢"
                    "angry" -> "😠"
                    "surprised" -> "😲"
                    else -> "😐"
                },
                fontSize = 64.sp,
                modifier = Modifier.scale(
                    if (isMicActive) 1.2f else 1.0f
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Waveform
            if (isMicActive && waveformAmplitudes.isNotEmpty()) {
                WaveformVisualizer(
                    amplitudes = waveformAmplitudes,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(40.dp)
                )
            } else {
                Text(
                    when (aiState) {
                        AIState.IDLE -> "Ready to help"
                        AIState.LISTENING -> "Listening..."
                        AIState.SPEAKING -> "Speaking..."
                        AIState.PROCESSING -> "Processing..."
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LazyMessageList(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(Color(0x0B0E14))
            .padding(horizontal = 12.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages.reversed().size) { index ->
            val message = messages.reversed()[index]
            MessageBubble(message = message)
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.type == Message.TYPE_USER) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = if (message.type == Message.TYPE_USER) Color(0xFF2196F3) else Color(0x2F2F3E)
            )
        ) {
            if (message.isImage) {
                Image(
                    painter = rememberAsyncImagePainter(message.text),
                    contentDescription = "Message image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    message.text,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MessageInputArea(
    text: String,
    onTextChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onMicToggle: () -> Unit = {},
    isMicActive: Boolean = false,
    aiState: AIState = AIState.IDLE,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0x0B0E14))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x2F2F3E))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent),
                placeholder = { Text("Ask me anything...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )
            
            IconButton(
                onClick = onMicToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Toggle Mic",
                    tint = if (isMicActive) Color(0xFF2196F3) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = onSend,
                enabled = text.isNotEmpty(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotEmpty()) Color(0xFF2196F3) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MediaControlsBar(
    songTitle: String,
    onPrevious: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    waveformAmplitudes: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0x1F1F2E))
            .padding(12.dp)
    ) {
        Text(
            "🎵 $songTitle",
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        
        if (waveformAmplitudes.isNotEmpty()) {
            WaveformVisualizer(
                amplitudes = waveformAmplitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(bottom = 8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x1F1F2E), RoundedCornerShape(4.dp))
    ) {
        if (amplitudes.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / amplitudes.size
        
        amplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val y = centerY - (amplitude * centerY).coerceIn(0f, centerY)
            val barHeight = (amplitude * height).coerceIn(2f, height)
            
            drawLine(
                color = Color(0xFF2196F3),
                start = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2),
                end = androidx.compose.ui.geometry.Offset(x, centerY + barHeight / 2),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x1A1A2E)),
        containerColor = Color(0x1A1A2E),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = currentPage == 0,
            onClick = { onPageSelected(0) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
            label = { Text("Chat", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            selected = currentPage == 1,
            onClick = { onPageSelected(1) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            selected = currentPage == 2,
            onClick = { onPageSelected(2) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

@Composable
fun SettingsMenuScreen(
    isMenuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleMcpMusic: () -> Unit = {},
    onToggleMcpVideo: () -> Unit = {},
    onLoginLogout: () -> Unit = {},
    onCopyActivationCode: () -> Unit = {},
    isActivationCardVisible: Boolean = false,
    activationPinCode: String = "------",
    batteryPercent: Int = 100,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x0B0E14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "⚙️ Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        // Activation Card
        if (isActivationCardVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
                border = BorderStroke(1.dp, Color(0xFF2196F3))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Device Activation Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x353541))
                            .clickable { onCopyActivationCode() }
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x353541))
                    ) {
                        Text(
                            activationPinCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                    
                    Text(
                        "Tap to copy activation code",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Settings Items
        SettingItemCard(
            title = "🎵 MCP Music",
            subtitle = "Enable/Disable music playback via MCP",
            onClick = onToggleMcpMusic
        )
        
        SettingItemCard(
            title = "🎬 MCP Video",
            subtitle = "Enable/Disable video streaming via MCP",
            onClick = onToggleMcpVideo
        )
        
        SettingItemCard(
            title = "⚙️ Full Settings",
            subtitle = "Open full settings screen",
            onClick = onSettingsClick
        )
        
        SettingItemCard(
            title = "👤 Account",
            subtitle = "Login/Logout to your account",
            onClick = onLoginLogout
        )
    }
}

@Composable
fun SettingItemCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
        border = BorderStroke(1.dp, Color(0x353541))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color(0xFF2196F3)
            )
        }
    }
}
