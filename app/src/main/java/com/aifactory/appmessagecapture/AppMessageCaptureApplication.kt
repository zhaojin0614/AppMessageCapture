package com.aifactory.appmessagecapture

import android.app.Application
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidgetWorker
import com.aifactory.appmessagecapture.data.AppDatabase

class AppMessageCaptureApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        BirthdayLog.install()
        BirthdayLog.i("Application onCreate. BirthdayKeeper module initializing...")
        BirthdayWidgetWorker.schedule(this)
    }
}
