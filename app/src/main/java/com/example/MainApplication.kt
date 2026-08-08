package com.example

import android.app.Application
import com.example.util.FirebaseManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


