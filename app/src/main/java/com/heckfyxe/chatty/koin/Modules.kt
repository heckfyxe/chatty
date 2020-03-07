package com.heckfyxe.chatty.koin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heckfyxe.chatty.remote.FirestoreApi
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.repository.*
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.ui.auth.EditUserDataViewModel
import com.heckfyxe.chatty.ui.contacts.ContactViewModel
import com.heckfyxe.chatty.ui.main.MainViewModel
import com.heckfyxe.chatty.ui.main.NewInterlocutorByUserDataViewModel
import com.heckfyxe.chatty.ui.message.MessageViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

private val koin: Koin by lazy { GlobalContext.get().koin }

val KOIN_USERS_FIRESTORE_COLLECTION = named("users")
val KOIN_USER_ID = named("uid")

private val KOIN_SCOPE_USER = named("user_logged")

val userScope: Scope
    get() = koin.getOrCreateScope(KOIN_SCOPE_USER.value, KOIN_SCOPE_USER)

val isUserScopeInitialized: Boolean
    get() = koin.getScopeOrNull(KOIN_SCOPE_USER.value) != null

fun deleteUserScope() {
    userScope.close()
    koin.deleteScope(KOIN_SCOPE_USER.value)
}

private val remoteApiModule = module {
    scope(KOIN_SCOPE_USER) {
        scoped { SendBirdApi(get(KOIN_USER_ID)) }
        scoped(KOIN_USER_ID) { get<FirebaseAuth>().currentUser!!.uid }
    }
    factory { FirestoreApi() }
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
    factory { EditUserDataRepository() }
}

private val roomModule = module {
    single { AppDatabase.getInstance(androidApplication()) }
    single { (get() as AppDatabase).getDialogDao() }
    single { (get() as AppDatabase).getMessageDao() }
    single { (get() as AppDatabase).getUserDao() }
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
}

private val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    factory(KOIN_USERS_FIRESTORE_COLLECTION) { get<FirebaseFirestore>().collection("users") }
}

val koinModule: List<Module> =
    listOf(repositoryModule, roomModule, viewModelModule, firebaseModule, remoteApiModule)