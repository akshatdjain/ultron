package com.akshatdjain.ultron.car

import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver

class UltronSession : Session(), DefaultLifecycleObserver {
    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return MainCarScreen(carContext)
    }
}
