package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isWifiConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                trySend(isWifi)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onAvailable(network: Network) {
                 val capabilities = connectivityManager.getNetworkCapabilities(network)
                 val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                 trySend(isWifi)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        trySend(isWifi)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
