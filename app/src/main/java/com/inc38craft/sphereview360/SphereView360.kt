package com.inc38craft.sphereview360

import android.app.Application
import timber.log.Timber

class SphereView360 : Application() {
    override fun onCreate() {
        super.onCreate()
        configureTimber()
    }

    private fun configureTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}