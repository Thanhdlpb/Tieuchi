# 📋 HƯỚNG DẪN TÍCH HỢP HOME ASSISTANT

## 🚀 Cách sử dụng Home Assistant Tab

### 1. **Cấu hình kết nối**
```kotlin
// Trong MainActivity hoặc bất kỳ nơi nào bạn muốn khởi tạo
val haViewModel = HomeAssistantViewModel(context)
haViewModel.connect(
    url = "https://your-ha-instance.local:8123",
    token = "eyJ0eXAiOiJKV1QiLCJhbGc..."
)
```

### 2. **Sử dụng trong UI**
```kotlin
// HomeAssistantScreen tự động hiển thị khi người dùng chuyển đến tab Home
HomeAssistantScreen(
    uiState = uiState.value,
    onToggleEntity = { haViewModel.toggleEntity(it) },
    onSetBrightness = { entity, brightness -> haViewModel.setEntityBrightness(entity, brightness) },
    onSetTemperature = { entity, temp -> haViewModel.setEntityTemperature(entity, temp) }
)
```

### 3. **Lấy Long-lived Access Token**
- Mở Home Assistant instance
- Đi tới **Settings → User Profile**
- Cuộn xuống **"Long-lived Access Tokens"**
- Nhấp **"Create Token"** và đặt tên cho nó
- Sao chép token vừa tạo

## 🏗️ Cấu trúc dự án

```
app/src/main/kotlin/com/xiaozhi/
├── ui/screens/
│   ├── MainScreen.kt                    ← NEW: Tab Navigation & Chat UI
│   ├── HomeAssistantScreen.kt           ← NEW: Home Assistant UI
│   └── HomeAssistantSettingsScreen.kt   ← NEW: HA Settings
├── smarthome/
│   ├── HaModels.kt
│   ├── HomeAssistantApi.kt
│   ├── HaWebSocketClient.kt
│   ├── HaAuthInterceptor.kt
│   ├── HomeAssistantManager.kt
│   ├── HomeAssistantViewModel.kt        ← NEW: State Management
│   ├── HomeAssistantManagerExtended.kt  ← NEW: Extensions
│   └── HomeAssistantManagerComplete.kt  ← NEW: Complete Implementation
└── Message.kt                           ← NEW: Message Data Class
```

## ✨ Tính năng

### 🏠 Tab Home Assistant
- ✅ Danh sách thiết bị theo thời gian thực
- ✅ Lọc thiết bị theo phòng
- ✅ Yêu thích thiết bị
- ✅ Điều khiển thiết bị (bật/tắt, độ sáng, nhiệt độ)
- ✅ Dialog chi tiết cho từng thiết bị
- ✅ Trạng thái kết nối
- ✅ Xử lý lỗi

### 💬 Tab Chat
- ✅ Trò chuyện với AI
- ✅ Nhận dạng cảm xúc
- ✅ Hiển thị sóng âm
- ✅ Điều khiển phát nhạc
- ✅ Phát video

### ⚙️ Tab Settings
- ✅ Bật/tắt MCP Music
- ✅ Bật/tắt MCP Video
- ✅ Truy cập cài đặt đầy đủ
- ✅ Quản lý tài khoản
- ✅ Mã kích hoạt thiết bị

## 🔧 Tích hợp vào MainActivity

### Bước 1: Import các class cần thiết
```kotlin
import com.xiaozhi.ui.screens.MainScreen
import com.xiaozhi.smarthome.HomeAssistantViewModel
```

### Bước 2: Thêm ViewModel vào MainActivity
```kotlin
private lateinit var haViewModel: HomeAssistantViewModel

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    haViewModel = HomeAssistantViewModel(this)
    // ...
}
```

### Bước 3: Truyền ViewModel sang MainScreen
```kotlin
setContent {
    XiaoZhiTheme {
        val waveform by waveformAmplitudes.collectAsStateWithLifecycle()
        MainScreen(
            // ... các tham số khác
            haViewModel = haViewModel  // Thêm dòng này
        )
    }
}
```

## 📱 Điều hướng giữa các Tab

Giao diện sử dụng **HorizontalPager** từ Jetpack Compose:
- **Tab 0**: Chat (💬)
- **Tab 1**: Home Assistant (🏠)
- **Tab 2**: Settings (⚙️)

Cuộn ngang hoặc nhấp vào biểu tượng ở Bottom Navigation Bar để chuyển tab.

## 🎨 Thiết kế Giao diện

- **Dark Theme**: Màu nền #0B0E14
- **Primary Color**: #2196F3 (Xanh dương)
- **Accent Color**: #4CAF50 (Xanh lá)
- **Cards**: Màu #1F1F2E với viền #353541

## 📚 Tài liệu thêm

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Home Assistant API Docs](https://developers.home-assistant.io/docs/api/rest/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 🐛 Xử lý sự cố

### Không kết nối được Home Assistant
- Kiểm tra URL và token có chính xác không
- Đảm bảo thiết bị có kết nối internet
- Kiểm tra firewall/SSL certificate

### Thiết bị không hiển thị
- Đảm bảo thiết bị được cấu hình trong Home Assistant
- Kiểm tra quyền truy cập của token
- Xem log trong Home Assistant

### UI bị lỗi
- Clear cache ứng dụng
- Rebuild project
- Kiểm tra phiên bản Jetpack Compose

## 🤝 Hỗ trợ

Nếu gặp bất kỳ vấn đề nào, hãy kiểm tra:
1. Home Assistant đang chạy
2. Token còn hiệu lực
3. Kết nối mạng ổn định
4. Đọc logs từ Android Studio
