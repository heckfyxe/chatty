package com.heckfyxe.chatty

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.heckfyxe.chatty.koin.koinModule
import com.sendbird.android.SendBird
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(koinModule)
        }
        SendBird.init("CCC955EE-E041-483F-9866-590B1C4B1E30", this)
        FirebaseApp.initializeApp(this)
    }
}