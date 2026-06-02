package com.aifactory.appmessagecapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aifactory.appmessagecapture.ui.MainScreen
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme

import android.content.Intent
import com.aifactory.appmessagecapture.service.MessageCaptureService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wake up the notification listener service to trigger onCreate() -> requestRebind()
        val serviceIntent = Intent(this, MessageCaptureService::class.java)
        startService(serviceIntent)
        enableEdgeToEdge()
        setContent {
            AppMessageCaptureTheme {
                MainScreen()
            }
        }
    }
}
