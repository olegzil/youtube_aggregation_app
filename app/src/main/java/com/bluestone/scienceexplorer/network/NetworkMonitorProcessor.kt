package com.bluestone.scienceexplorer.network

import androidx.appcompat.app.AppCompatActivity
import com.bluestone.scienceexplorer.uitilities.observeInLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.onEach

class NetworkMonitorProcessor(
    private val activity: AppCompatActivity,
    private val connectionLostCallback: () -> Unit,
    private val connectionUnavailableCallback: () -> Unit,
    private val connectionAvailableCallback: () -> Unit
) {
    val channelUiNeedsUpdated = MutableStateFlow(true)
    private val networkMonitor = NetworkMonitor(activity)
    private var prevTitle = ""

    init {
        monitorNetwork()
    }

    fun isNetworkAvailable() = networkMonitor.isNetworkAvailable()
    private fun handleConnectionLost(event: NetworkMonitor.NetworkHealthEvent.ConnectionLost) {
        channelUiNeedsUpdated.getAndUpdate { true }
        prevTitle = activity.supportActionBar?.title.toString()
        activity.supportActionBar?.title = event.message
        connectionLostCallback()
    }

    private fun handleInternetUnavailable(event: NetworkMonitor.NetworkHealthEvent.InternetUnavailable) {
        channelUiNeedsUpdated.getAndUpdate { true }
        prevTitle = activity.supportActionBar?.title.toString()
        activity.supportActionBar?.title = event.message
        connectionUnavailableCallback()
    }

    private fun handleConnectionAvailable(_event: NetworkMonitor.NetworkHealthEvent.InternetAvailable) {
        if (prevTitle != "")
            activity.supportActionBar?.title = prevTitle
        if (channelUiNeedsUpdated.value) {
            connectionAvailableCallback()
            channelUiNeedsUpdated.getAndUpdate { false }
        }
    }

    private fun monitorNetwork() {
        networkMonitor.initialize()
        networkMonitor.networkMonitorFlow.onEach { event ->
            when (event) {
                is NetworkMonitor.NetworkHealthEvent.ConnectionLost -> {
                    handleConnectionLost(event)
                }
                is NetworkMonitor.NetworkHealthEvent.InternetUnavailable -> {
                    handleInternetUnavailable(event)
                }
                is NetworkMonitor.NetworkHealthEvent.InternetAvailable -> {
                    handleConnectionAvailable(event)
                }

            }
        }.observeInLifecycle(activity)
    }
}