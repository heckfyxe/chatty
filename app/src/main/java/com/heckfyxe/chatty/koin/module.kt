package com.heckfyxe.chatty.koin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heckfyxe.chatty.ui.auth.EditUserDataViewModel
import com.heckfyxe.chatty.ui.main.MainViewModel
import com.heckfyxe.chatty.ui.main.NewDialogViewModel
import com.heckfyxe.chatty.ui.message.MessageViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

private val viewModelModule = module {
    viewModel { MainViewModel() }
    viewModel { EditUserDataViewModel() }
    viewModel { NewDialogViewModel() }
    viewModel { MessageViewModel() }
}

private val firebaseModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    factory("users") { get<FirebaseFirestore>().collection("users") }
    factory("uid") { get<FirebaseAuth>().currentUser!!.uid }
}

val koinModule: List<Module> = listOf(viewModelModule, firebaseModule)