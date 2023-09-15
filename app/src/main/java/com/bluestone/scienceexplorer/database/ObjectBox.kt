package com.bluestone.scienceexplorer.database

import android.content.Context
import io.objectbox.BoxStore
import io.objectbox.BoxStoreBuilder
import io.objectbox.android.Admin
import io.objectbox.android.BuildConfig
import io.objectbox.config.ValidateOnOpenModePages
import io.objectbox.exception.FileCorruptException
import timber.log.Timber

const val DBTAG = "ObjectBox"

object ObjectBox {
    private lateinit var _boxStore: BoxStore
    fun init(context: Context) {
        val storeBuilder: BoxStoreBuilder = MyObjectBox.builder()
            .validateOnOpen(ValidateOnOpenModePages.WithLeaves) // Additional DB page validation
            .validateOnOpenPageLimit(20)
            .androidContext(context.applicationContext)
        _boxStore = try {
            storeBuilder.build()
        } catch (e: FileCorruptException) { // Demonstrate handling issues caused by devices with a broken file system
            Timber.tag(DBTAG).w(e, "File corrupt, trying previous data snapshot...")
            // Retrying requires ObjectBox 2.7.1+
            storeBuilder.usePreviousCommit()
            storeBuilder.build()
        }
        if (BuildConfig.DEBUG) {
            Timber.tag("ObjectBox")
                .d("Using ObjectBox %s (%s)", BoxStore.getVersion(), BoxStore.getVersionNative())
            // Enable Data Browser on debug builds.
            // https://docs.objectbox.io/data-browser
            Admin(_boxStore).start(context.applicationContext)
        }
    }

    fun get(): BoxStore {
        return _boxStore
    }
}
