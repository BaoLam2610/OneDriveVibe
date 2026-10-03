package com.lambao.odv.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.lambao.odv.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [NetworkMonitor] cho Android (DS-05), dựa trên `ConnectivityManager`. Sống suốt vòng đời app nên đăng ký một lần và
 * không hủy đăng ký. "Online" nghĩa là mạng mặc định có Internet và đã được hệ thống xác nhận (VALIDATED), nên Wi-Fi
 * đăng nhập captive portal hay Wi-Fi không ra ngoài được tính là offline.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val online = MutableStateFlow(currentlyOnline())

    override val isOnline: StateFlow<Boolean> = online.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                online.value = capabilities.isUsable()
            }

            override fun onLost(network: Network) {
                online.value = false
            }
        })
    }

    private fun currentlyOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)?.isUsable() == true
    }

    private fun NetworkCapabilities.isUsable(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
