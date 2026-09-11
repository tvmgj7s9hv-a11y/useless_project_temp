package org.tinkerhub.scrollmeter

import android.app.Application
import org.tinkerhub.scrollmeter.data.ScrollMeterDatabase
import org.tinkerhub.scrollmeter.data.UserPreferencesRepository
import org.tinkerhub.scrollmeter.notifications.NotificationHelper

class ScrollMeterApplication : Application() {

    val database by lazy { ScrollMeterDatabase.getDatabase(this) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val notificationHelper by lazy { NotificationHelper(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationHelper.createNotificationChannels()
    }

    companion object {
        lateinit var instance: ScrollMeterApplication
            private set
    }
}
