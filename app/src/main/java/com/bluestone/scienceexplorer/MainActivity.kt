package com.bluestone.scienceexplorer

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.bluestone.embededyoutubeplayer.CrashReportingTree
import com.bluestone.scenceexplorer.BuildConfig
import com.bluestone.scenceexplorer.R
import com.bluestone.scenceexplorer.databinding.ActivityMainBinding
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.dataclasses.ApplicationControlBlock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        val controlBlock = Cache.getApplicationControlBlock()
        if (controlBlock.appVersion.isEmpty() && controlBlock.uniqueClientID.isEmpty() && controlBlock.defaultClientID.isEmpty()) {
            val initialControlBlock = ApplicationControlBlock(
                BuildConfig.youtube_client_key,
                BuildConfig.youtube_client_key,
                BuildConfig.VERSION_CODE.toString()
            )
            Cache.putApplicationControlBlock(initialControlBlock)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.visibility = View.GONE
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.let {
            setSupportActionBar(it)
        }
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(false)
        }
        monitorTitleBarControl()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            _homeNavigaion.trySend(true)
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun monitorTitleBarControl() {
        lifecycleScope.launch {
            _titleBarControl.receiveAsFlow().onEach { show ->
                if (show) binding.toolbar.visibility = View.VISIBLE
                else binding.toolbar.visibility = View.GONE
            }.collect()
        }
    }

    companion object {
        private val _shareEvent = Channel<String>(Channel.CONFLATED)
        val shareEvent: ReceiveChannel<String> = _shareEvent
        private val _homeNavigaion = Channel<Boolean>(Channel.CONFLATED)
        val homeNaviation = _homeNavigaion as ReceiveChannel<Boolean>
        private val _titleBarControl = Channel<Boolean>(Channel.CONFLATED)
        val titleBarControl = _titleBarControl as SendChannel<Boolean>
    }
}