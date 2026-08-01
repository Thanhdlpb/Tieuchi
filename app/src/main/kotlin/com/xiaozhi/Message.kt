package com.xiaozhi

data class Message(
    val type: String,
    val text: String,
    val isImage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_USER = "user"
        const val TYPE_ASSISTANT = "assistant"
    }
}
