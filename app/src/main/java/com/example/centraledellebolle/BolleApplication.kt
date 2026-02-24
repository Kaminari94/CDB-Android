package com.example.centraledellebolle

import android.app.Application
import com.example.centraledellebolle.data.AppContainer
import com.example.centraledellebolle.data.DefaultAppContainer

class BolleApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}