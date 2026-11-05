package com.example

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.CookieManager
import com.example.network.NetworkMonitor
import com.example.ui.screens.MainViewContainer

class MainActivity : Activity() {

  private lateinit var networkMonitor: NetworkMonitor
  private lateinit var mainViewContainer: MainViewContainer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    networkMonitor = NetworkMonitor(this) { online ->
      runOnUiThread {
        if (::mainViewContainer.isInitialized) {
          mainViewContainer.onNetworkStatusChanged(online)
        }
      }
    }
    mainViewContainer = MainViewContainer(this, networkMonitor)

    setContentView(mainViewContainer.rootLayout)
  }

  @Deprecated("Deprecated in Java")
  @Suppress("DEPRECATION")
  override fun onBackPressed() {
    val wv = mainViewContainer.webView
    if (wv != null && wv.canGoBack()) {
      wv.goBack()
    } else {
      super.onBackPressed()
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    mainViewContainer.onConfigurationChanged(newConfig)
  }

  override fun onPause() {
    super.onPause()
    try {
      CookieManager.getInstance().flush()
    } catch (_: Exception) {}
  }
}
