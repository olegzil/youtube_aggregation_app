package com.bluestone.scienceexplorer

import android.app.Application
import com.bluestone.scienceexplorer.database.ObjectBox

class ScienceExplorerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ObjectBox.init(this)
    }
}