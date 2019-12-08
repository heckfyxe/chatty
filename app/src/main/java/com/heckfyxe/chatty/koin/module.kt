package com.heckfyxe.chatty.koin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.repository.*
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.ui.auth.ContactViewModel
import com.heckfyxe.chatty.ui.auth.EditUserDataViewModel
import com.heckfyxe.chatty.ui.friends.FriendsViewModel
import com.heckfyxe.chatty.ui.main.MainViewModel
import com.heckfyxe.chatty.ui.main.NewInterlocutorByUserDataViewModel
import com.heckfyxe.chatty.ui.message.MessageViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val remoteApiModule = module {
    single { SendBirdApi(get(KOIN_USER_ID)) }
}

private val repositoryModule = module {
    factory { DialogRepository() }
    factory { (channelId: String) ->
        MessageRepository(
            channelId
        )
    }
    factory { ContactRepository() }
    factory { UserRepository() }
    factory { EditUserDataRepository(get(), get(KOIN_USERS_FIRESTORE_COLLECTION)) }
}

private val roomModule = module {
    single { AppDatabase.getInstance(androidApplication()) }
    factory { (get() as AppDatabase).getDialogDao() }
    factory { (get() as AppDatabase).getMessageDao() }
    factory { (get() as AppDatabase).getUserDao() }
}

private val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { EditUserDataViewModel(androidApplication(), get()) }
    viewModel { (userDataName: String) -> NewInterlocutorByUserDataViewModel(userDataName) }
    viewModel { (channelId: String, interlocutorId: String, lastMessageTime: Long) ->
        MessageViewModel(
            channelId,
            interlocutorId,
            lastMessageTime
        )
    }
    viewModel { ContactViewModel() }
    viewModel { FriendsViewModel() }
}

val KOIN_USERS_FIRESTORE_COLLECTION = named("users")
val KOIN_USER_ID = named("uid")

private val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .setCacheSizeBytes(-1)
                .build()
        }
    }
    factory(KOIN_USERS_FIRESTORE_COLLECTION) { get<FirebaseFirestore>().collection("users") }
    factory(KOIN_USER_ID) { get<FirebaseAuth>().currentUser!!.uid }
}

val koinModule: List<Module> =
    listOf(repositoryModule, roomModule, remoteApiModule, viewModelModule, firebaseModule)