package com.lilly.ble.di

import com.lilly.ble.MyRepository
import com.lilly.ble.viewmodel.BleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { BleViewModel(get()) }
}

val repositoryModule = module{
    single{
        MyRepository()
    }
}