package com.example.laforte20

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FacebookSdk.setApplicationId("722191337175281") // <-- Explicitly set App ID
        FacebookSdk.sdkInitialize(this)
        AppEventsLogger.activateApp(this)
    }
}
