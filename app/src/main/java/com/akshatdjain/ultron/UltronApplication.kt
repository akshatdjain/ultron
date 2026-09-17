package com.akshatdjain.ultron

import android.app.Application
import com.akshatdjain.ultron.util.UltronLogger

class UltronApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UltronLogger.init(this)
    }
}
