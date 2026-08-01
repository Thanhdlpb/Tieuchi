package com.xiaozhi.hass.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xiaozhi.MyApplication
import com.xiaozhi.ui.theme.XiaoZhiTheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entityManager = (application as MyApplication).entityManager
        setContent {
            XiaoZhiTheme {
                DashboardScreen(entityManager = entityManager)
            }
        }
    }
}