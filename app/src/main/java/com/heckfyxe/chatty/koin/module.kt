package com.heckfyxe.chatty.koin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heckfyxe.chatty.repository.ContactRepository
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.repository.UserRepository
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.ui.auth.ContactViewModel
import com.heckfyxe.chatty.ui.auth.EditUserDataViewModel
import com.heckfyxe.chatty.ui.main.MainViewModel
import com.heckfyxe.chatty.ui.main.NewDialogViewModel
import com.heckfyxe.chatty.ui.message.MessageViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

private val repositoryModule = module {
    factory { DialogRepository() }
    factory { (channelId: String) -> MessageRepository(channelId) }
    factory { ContactRepository() }
    factory { UserRepository() }
}

private val roomModule = module {
    single { AppDatabase.getInstance(androidApplication()) }
    single { (get() as AppDatabase).getDialogDao() }
    single { (get() as AppDatabase).getMessageDao() }
    single { (get() as AppDatabase).getUserDao() }
}

private val viewModelModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { EditUserDataViewModel() }
    viewModel { NewDialogViewModel() }
    viewModel { (channelId: String) -> MessageViewModel(channelId) }
    viewModel { ContactViewModel() }
}

const val KOIN_USERS_FIRESTORE_COLLECTION = "users"
const val KOIN_USER_ID = "uid"

private val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    factory(KOIN_USERS_FIRESTORE_COLLECTION) { get<FirebaseFirestore>().collection("users") }
    factory(KOIN_USER_ID) { get<FirebaseAuth>().currentUser!!.uid }
}

val koinModule: List<Module> = listOf(repositoryModule, roomModule, viewModelModule, firebaseModule)