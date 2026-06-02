package com.aifactory.appmessagecapture

import android.app.Application
import com.aifactory.appmessagecapture.data.AppDatabase

class AppMessageCaptureApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
