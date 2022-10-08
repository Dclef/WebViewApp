package icu.dclef.webview.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import icu.dclef.webapp.BuildConfig

import icu.dclef.webapp.R
import icu.dclef.webview.util.NetworkChangeReceiver
import icu.dclef.webview.util.NetworkUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.general_custom_dialog_network_error.*


open class WebViewActivity : AppCompatActivity() {

    private val networkUtils = NetworkUtils()
    private val networkChangeReceiver = NetworkChangeReceiver()

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mNotificationReceiverInternet,
            IntentFilter(getString(R.string.keySendInternetStatus))
        )

        if (Build.VERSION.SDK_INT >= 23) {
            // 动态注册广播
            val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            this.registerReceiver(networkChangeReceiver, intentFilter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
            loadWeb(BuildConfig.URL)
        } else {
            imgv_network_error.visibility = View.GONE
            webView.visibility = View.VISIBLE
            overlayView.visibility = View.VISIBLE
            connectionLostAlert("退出", BuildConfig.URL)
        }


    }




    /**
     * webview setting设置
     */

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "ClickableViewAccessibility")
    private fun loadWeb(url: String) {

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true//支持js
        webSettings.builtInZoomControls = false //设置支持缩放
        webSettings.javaScriptCanOpenWindowsAutomatically=true//支持通过JS打开新窗口
        webSettings.domStorageEnabled=true//缓存数据
        webSettings.useWideViewPort=true//自适应
        webSettings.loadWithOverviewMode=true//缩放至屏幕的大小
        webSettings.databaseEnabled=true
        webSettings.allowFileAccess=true

//        val userAgent: String = webSettings.userAgentString //获取ua
//        Log.i("TAG", "User Agent:$userAgent");  //日志打印
//        webSettings.userAgentString="app" //自定义UA
        webView.webViewClient = myWebClient()
        webView.webChromeClient = MyWebChromeClient()
        webView.addJavascriptInterface(JavaScriptHandler(), "Your_Handler_NAME")//自定义js 可删除
        try {
            webView.loadData("", "text/html", null)
            webView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView.setOnTouchListener { _, _ ->
            if (!networkUtils.haveNetworkConnection(this)) {
                webView.url?.let { connectionLostAlert("Quit", it) }
            }
            false
        }

    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    inner class myWebClient : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
                imgv_network_error.visibility = View.GONE
                webView.visibility = View.VISIBLE
                overlayView.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            } else {
                webView.visibility = View.GONE
                imgv_network_error.setVisibility(View.VISIBLE)
                overlayView.visibility = View.VISIBLE
                connectionLostAlert("Quit", url)
            }
        }

        /**
         * 重定向设置 将自定义链接进行拦截
         */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val hit: WebView.HitTestResult = view.hitTestResult
            if (TextUtils.isEmpty(hit.getExtra()) || hit.getType() == 0) {

            }
            try {

                if (request.url.toString().startsWith("http:") || request.url.toString().startsWith("https:")
                //除本链接外其他链接全部跳转到游览器中
//                if(request.url.toString().contains("https://github.com/dclef")
                ) {
                    view.loadUrl(request.url.toString())
                    return false
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }


        @RequiresApi(Build.VERSION_CODES.M)
        override fun onPageFinished(view: WebView, url: String) {
            if (networkUtils.haveNetworkConnection(this@WebViewActivity)) {
                webView.visibility = View.VISIBLE
                overlayView.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            try {
                webView.visibility = View.GONE
                imgv_network_error.visibility = View.VISIBLE
                overlayView.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


    }


    internal inner class MyWebChromeClient : WebChromeClient() {

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(
            view: WebView,
            url: String,
            message: String,
            defaultValue: String,
            result: JsPromptResult
        ): Boolean {
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            result.confirm()
            if (message.equals("退出", ignoreCase = true)) {
                finish()
            } else {
                showToast(message)
            }
            return true
        }
    }

    //自定义js 可删除
    class JavaScriptHandler internal constructor() {

        @JavascriptInterface
        fun setResult(value: String?, msg: String, status: String) {
            // 检查状态
        }
    }


    /**
     * 返回回调
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            generalDailog(getString(R.string.app_name), "确定要退出吗?")
        }
    }


    /**
     * 弹出返回警告窗口
     */
    private fun generalDailog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this@WebViewActivity)

            builder.setTitle(title)
            builder.setMessage(message)
            builder.setCancelable(false)
            builder.setPositiveButton("是") { _, _ ->
                try {
                    webView.clearCache(true)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            builder.setNegativeButton("否") { dialog, _ ->
                dialog.cancel()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 监听是否有网
     */
    private val mNotificationReceiverInternet = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent?) {

            if (intent != null && intent.extras != null && !intent.extras!!.isEmpty) {
                if (!intent.getBooleanExtra("isConnected", false)) {
                    val url = if (webView.url == null) {
                        BuildConfig.URL
                    } else {
                        webView.url
                    }
                    url?.let { url1 ->
                        connectionLostAlert("退出", url1)
                    }
                }
            }
        }
    }


    /***
     * @param noButtonText Button text
     * @param url Url
     */
    @RequiresApi(Build.VERSION_CODES.M)
    protected fun connectionLostAlert(noButtonText: String, url: String) {
        try {
            // custom dialog
            webView.visibility = View.GONE
            val customDialog = AppCompatDialog(this)
            customDialog.setContentView(R.layout.general_custom_dialog_network_error)
            customDialog.setCanceledOnTouchOutside(false)
            customDialog.setCancelable(false)
            customDialog.tvDialogTitle.text = getString(R.string.noInternetConnection)

            customDialog.tvDialogRetry.setOnClickListener { _ ->
                customDialog.cancel()
                if (networkUtils.haveNetworkConnection(this)) {
                    if (!isTextEmpty(url))
                        loadWeb(url)
                    customDialog.cancel()
                } else {
                    connectionLostAlert(noButtonText, url)
                }
            }
            customDialog.tvDialogCancel.text = noButtonText
            customDialog.tvDialogCancel.setOnClickListener { _ ->
                customDialog.cancel()
                finish()
            }

            if (!customDialog.isShowing) {
                customDialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }


    private fun isTextEmpty(text: String?): Boolean {
        var result = ""
        return try {
            if (text != null) {
                result = text.trim { it <= ' ' }
                result.isEmpty() || result.equals("null", ignoreCase = true)
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }

    }

    /**
     *关闭事件
     */
    override fun onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mNotificationReceiverInternet)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

}
