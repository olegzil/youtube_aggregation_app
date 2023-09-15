package com.bluestone.scienceexplorer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import com.bluestone.scenceexplorer.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class NetworkMonitor(
    private val context: Context
) {
    sealed class NetworkHealthEvent {
        data class ConnectionLost(val message: String = "connection lost") : NetworkHealthEvent()
        data class InternetUnavailable(val message: String = "no internet") : NetworkHealthEvent()
        data class InternetAvailable(val message: String = "internet available") :
            NetworkHealthEvent()
    }

    private var initialized = false
    private val networkMonitorChannel = Channel<NetworkHealthEvent>(Channel.UNLIMITED)
    val networkMonitorFlow = networkMonitorChannel.receiveAsFlow()
    private var connectivityManager = ContextCompat.getSystemService(
        context,
        ConnectivityManager::class.java
    ) as ConnectivityManager

    fun isNetworkAvailable(): Boolean {
        initialize()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_USB) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            networkMonitorChannel.trySend(
                NetworkHealthEvent.InternetAvailable(
                    context.resources.getString(R.string.success_connection_available)
                )
            )
        }

        override fun onUnavailable() {
            super.onUnavailable()
            networkMonitorChannel.trySend(
                NetworkHealthEvent.InternetUnavailable(
                    context.resources.getString(R.string.error_device_offline)
                )
            )
        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            networkMonitorChannel.trySend(
                NetworkHealthEvent.ConnectionLost(
                    context.resources.getString(
                        R.string.error_connection_lost
                    )
                )
            )
        }
    }

    private var networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    fun initialize() {
        if (!initialized) {
            connectivityManager.requestNetwork(networkRequest, networkCallback)
        }
        initialized = true
    }
}
