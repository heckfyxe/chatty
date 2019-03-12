package com.heckfyxe.chatty

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.heckfyxe.chatty.koin.koinModule
import com.sendbird.android.SendBird
import org.koin.android.ext.android.startKoin

class App : MultiDexApplication() {
    companion object {
        private const val APP_ID = "CCC955EE-E041-483F-9866-590B1C4B1E30"
    }

    override fun onCreate() {
        super.onCreate()

        startKoin(this, koinModule)
        SendBird.init(APP_ID, this)
        FirebaseApp.initializeApp(this)
    }
}