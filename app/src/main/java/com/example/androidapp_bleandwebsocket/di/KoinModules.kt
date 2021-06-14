package com.example.androidapp_bleandwebsocket.di

import com.example.androidapp_bleandwebsocket.BleRepository
import com.example.androidapp_bleandwebsocket.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get()) }
}

val repositoryModule = module{
    single{
        BleRepository()
    }
}