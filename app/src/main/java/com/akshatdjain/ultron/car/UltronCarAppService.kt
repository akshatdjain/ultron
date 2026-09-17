package com.akshatdjain.ultron.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class UltronCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.Builder(applicationContext).build()
    }

    override fun onCreateSession(): Session {
        return UltronSession()
    }
}
