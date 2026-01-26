package com.homesoil.app

import android.app.Application
import com.homesoil.app.data.repository.HomesoilRepository

class HomesoilApp : Application() {

    lateinit var repository: HomesoilRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = HomesoilRepository(this)
    }

    companion object {
        lateinit var instance: HomesoilApp
            private set
    }
}
