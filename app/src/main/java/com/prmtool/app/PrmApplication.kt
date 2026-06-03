package com.prmtool.app

import android.app.Application
import android.content.Context
import com.prmtool.app.data.ContactRepository
import com.prmtool.app.data.SettingsStore
import com.prmtool.app.data.db.AppDatabase

/** Minimal manual DI container, exposed via the Application. */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    val settings = SettingsStore(context.applicationContext)
    val repository = ContactRepository(
        contactDao = database.contactDao(),
        eventDao = database.eventDao(),
        sourceDao = database.sourceDao(),
        appContext = context.applicationContext
    )
}

class PrmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
