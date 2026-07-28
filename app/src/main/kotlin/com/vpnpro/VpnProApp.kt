package com.vpnpro

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VpnProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
