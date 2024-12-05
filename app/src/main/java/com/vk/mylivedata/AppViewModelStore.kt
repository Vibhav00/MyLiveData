package com.vk.mylivedata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner


object  AppViewModelStore : ViewModelStoreOwner {
    private val appViewModelStore: ViewModelStore = ViewModelStore()

    // private val factory = ViewModelProvider.NewInstanceFactory()
    private val factory = ViewModelProvider.NewInstanceFactory()

    // Method to get or create the ViewModel
    fun getAppScopedViewModel() :AppViewModel{
        return ViewModelProvider(this, factory)[AppViewModel::class.java]
    }
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}

