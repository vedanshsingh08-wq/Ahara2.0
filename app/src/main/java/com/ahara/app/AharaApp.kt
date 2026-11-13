package com.ahara.app

import android.app.Application
import com.ahara.app.notify.Reminders

class AharaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
    }
}
