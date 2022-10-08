package icu.dclef.webview.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import icu.dclef.webapp.R


class NetworkChangeReceiver : BroadcastReceiver() {


    @RequiresApi(Build.VERSION_CODES.M)
    fun isOnline(context: Context): Boolean {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false//判断network是否为空
        val capabilities: NetworkCapabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        if (isOnline(context)) {
            sendInternetUpdate(context, true)
        } else {
            sendInternetUpdate(context, false)
        }
    }

    private fun sendInternetUpdate(context: Context, isConnected: Boolean) {
        val intent = Intent(context.getString(R.string.keySendInternetStatus))
        intent.putExtra("isConnected", isConnected)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
