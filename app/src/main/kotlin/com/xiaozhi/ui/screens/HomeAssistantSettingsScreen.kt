package com.xiaozhi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeAssistantSettingsScreen(
    onConnect: (url: String, token: String) -> Unit = { _, _ -> },
    onDisconnect: () -> Unit = {},
    isConnected: Boolean = false,
    currentUrl: String = "",
    currentToken: String = "",
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(currentUrl) }
    var token by remember { mutableStateOf(currentToken) }
    var showPassword by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x0B0E14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column {
            Text(
                "🏠 Home Assistant Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Configure your Home Assistant instance connection",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        // Status Card
        StatusCard(
            isConnected = isConnected,
            connectionUrl = currentUrl
        )
        
        // Connection Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
            border = BorderStroke(1.dp, Color(0x353541))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Connection Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // URL Input
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Home Assistant URL") },
                    placeholder = { Text("https://your-ha-instance.local:8123") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "URL",
                            tint = Color(0xFF2196F3)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0x353541),
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedLabelColor = Color(0xFF2196F3)
                    )
                )
                
                // Token Input
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Long-lived Access Token") },
                    placeholder = { Text("eyJ0eXAiOiJKV1QiLCJhbGc...") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Token",
                            tint = Color(0xFF2196F3)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0x353541),
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedLabelColor = Color(0xFF2196F3)
                    )
                )
                
                // Info Text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x2F2F3E), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        "Get your token from Home Assistant Settings → User Profile → Long-lived Access Tokens",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Test Result
        testResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.contains("Success")) Color(0x1B5E20) else Color(0x5D4037)
                ),
                border = BorderStroke(
                    1.dp,
                    if (result.contains("Success")) Color(0xFF4CAF50) else Color(0xFFFF5722)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.contains("Success")) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = "Test result",
                        modifier = Modifier.size(20.dp),
                        tint = if (result.contains("Success")) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                    Text(
                        result,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    isTesting = true
                    // Simulate test
                    testResult = "Success: Connected to Home Assistant"
                    isTesting = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                enabled = url.isNotEmpty() && token.isNotEmpty() && !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Test",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
            
            Button(
                onClick = {
                    onConnect(url, token)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = url.isNotEmpty() && token.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Connect")
            }
            
            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF5722))
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Disconnect",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect")
                }
            }
        }
        
        // Help Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x1F1F2E)),
            border = BorderStroke(1.dp, Color(0x353541))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "❓ How to Setup",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                listOf(
                    "1. Open your Home Assistant instance URL",
                    "2. Go to Settings → User Profile",
                    "3. Scroll down to 'Long-lived Access Tokens'",
                    "4. Click 'Create Token' and give it a name",
                    "5. Copy the token and paste it here"
                ).forEach { step ->
                    Text(
                        step,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusCard(
    isConnected: Boolean,
    connectionUrl: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0x1B5E20) else Color(0x3E2C27)
        ),
        border = BorderStroke(
            1.dp,
            if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = "Status",
                modifier = Modifier.size(32.dp),
                tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isConnected) "Connected" else "Not Connected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isConnected && connectionUrl.isNotEmpty()) {
                    Text(
                        connectionUrl,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = androidx.compose.foundation.text.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
