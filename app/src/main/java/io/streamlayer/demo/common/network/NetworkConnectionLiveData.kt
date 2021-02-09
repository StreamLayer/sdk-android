package io.streamlayer.demo.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData

internal class NetworkConnectionLiveData @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) constructor(
    private val context: Context
) : LiveData<Boolean>() {

    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            postValue(true)
        }

        override fun onLost(network: Network) {
            postValue(false)
            super.onLost(network)
        }
    }

    override fun onActive() {
        super.onActive()
        postValue(connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                networkCallback
            )
        }
    }

    override fun onInactive() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onInactive()
    }

}