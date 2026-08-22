package com.prsnl.app

import android.app.Application
import com.prsnl.core.log.CrashLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PrsnlApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
    }
}
