package icu.dclef.webview.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Network Util Class
 */

class NetworkUtils {

        @RequiresApi(Build.VERSION_CODES.M)
        //判断是否有网
        fun haveNetworkConnection(context: Context): Boolean {
            try {
                val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connMgr.activeNetwork ?: return false
                val capabilities: NetworkCapabilities = connMgr.getNetworkCapabilities(network) ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
}