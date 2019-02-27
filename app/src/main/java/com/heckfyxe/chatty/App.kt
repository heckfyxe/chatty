package com.heckfyxe.chatty

import android.app.Application
import com.google.firebase.FirebaseApp
import com.heckfyxe.chatty.koin.koinModule
import org.koin.android.ext.android.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin(this, koinModule)
        FirebaseApp.initializeApp(this)
    }
}