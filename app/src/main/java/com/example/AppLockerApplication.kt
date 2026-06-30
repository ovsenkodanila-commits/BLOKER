package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppLockRepository

class AppLockerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppLockRepository(database.appLockDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AppLockerApplication
            private set
    }
}
