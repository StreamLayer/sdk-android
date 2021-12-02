package io.streamlayer.demo.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import io.streamlayer.demo.common.dispatcher.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class NetworkConnectionUseCase @Inject constructor(
    dispatcherProvider: CoroutineDispatcherProvider,
    context: Context
) {

    val state =
        NetworkConnectionTracker(context).networkStatus.distinctUntilChanged().flowOn(dispatcherProvider.default)
}

@ExperimentalCoroutinesApi
internal class NetworkConnectionTracker @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) constructor(
    context: Context
) {
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    val networkStatus = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}