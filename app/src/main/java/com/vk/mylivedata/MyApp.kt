package com.vk.mylivedata

import android.app.Application

class MyApp:Application() {
    override fun onTerminate() {
        super.onTerminate()
        AppViewModelStore.viewModelStore.clear()
    }
}