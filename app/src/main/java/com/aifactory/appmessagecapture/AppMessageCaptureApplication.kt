package com.aifactory.appmessagecapture

import android.app.Application
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidgetWorker
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.worker.NotificationCleanupWorker
import com.aifactory.appmessagecapture.worker.RecurringBillWorker

class AppMessageCaptureApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        BirthdayLog.install()
        BirthdayLog.i("Application onCreate. BirthdayKeeper module initializing...")
        BirthdayWidgetWorker.schedule(this)
        RecurringBillWorker.schedule(this)
        NotificationCleanupWorker.schedule(this)
    }
}
