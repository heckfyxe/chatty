package com.heckfyxe.chatty.koin

import com.heckfyxe.chatty.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

private val viewModelModule = module {
    viewModel { MainViewModel() }
}

val koinModule: List<Module> = listOf(viewModelModule)