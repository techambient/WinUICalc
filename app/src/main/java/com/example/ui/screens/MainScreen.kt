package com.example.ui.screens

import android.R
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.network.NetworkMonitor

class MainViewContainer(
    private val activity: Activity,
    private val networkMonitor: NetworkMonitor
) {
    val rootLayout: FrameLayout = FrameLayout(activity).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.parseColor("#13111C"))
    }

    var webView: WebView? = null
        private set

    private val progressBar: ProgressBar
    private val offlineBanner: LinearLayout
    private val errorOverlay: LinearLayout

    private var isOnline = true
    private var isDarkTheme = false

    init {
        isDarkTheme = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 1. Create and add configured WebView
        val wv = createConfiguredWebView(
            context = activity,
            isOnline = isOnline,
            isDarkTheme = isDarkTheme,
            onProgressChange = { progress ->
                updateProgress(progress)
            },
            onErrorChange = { hasError, desc ->
                updateErrorState(hasError, desc)
            },
            onCanGoBackChange = { _ -> }
        )
        webView = wv
        rootLayout.addView(wv)

        // 2. Slim Progress Bar at the top edge
        progressBar = ProgressBar(activity, null, R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(activity, 3)
            ).apply {
                gravity = Gravity.TOP
            }
            max = 100
            visibility = View.GONE
            indeterminateDrawable?.setTint(Color.parseColor("#8C7AE6"))
            progressDrawable?.setTint(Color.parseColor("#8C7AE6"))
        }
        rootLayout.addView(progressBar)

        // 3. Offline Banner
        offlineBanner = LinearLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#2C2416"))
            val pad = dpToPx(activity, 8)
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE

            val tv = TextView(activity).apply {
                text = "Offline Mode — Running with cached files & saved preferences"
                setTextColor(Color.parseColor("#FFD59E"))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
            }
            addView(tv)
        }
        rootLayout.addView(offlineBanner)

        // 4. Offline Error Overlay
        errorOverlay = LinearLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#13111C"))
            val pad = dpToPx(activity, 24)
            setPadding(pad, pad, pad, pad)
            visibility = View.GONE

            val titleTv = TextView(activity).apply {
                text = "First-Time Offline Setup"
                setTextColor(Color.WHITE)
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            addView(titleTv)

            val descTv = TextView(activity).apply {
                text = "WinCalc is configured with persistent caching, localStorage, and cookies. Connect once online to download the calculator bundle for full offline use."
                setTextColor(Color.parseColor("#A4B0BE"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(activity, 12), 0, dpToPx(activity, 24))
            }
            addView(descTv)

            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER

                val retryBtn = Button(activity).apply {
                    text = "Retry"
                    setBackgroundColor(Color.parseColor("#8C7AE6"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        errorOverlay.visibility = View.GONE
                        webView?.reload()
                    }
                }
                addView(retryBtn)

                val space = View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(activity, 16), 1)
                }
                addView(space)

                val cacheBtn = Button(activity).apply {
                    text = "Load Cache"
                    setBackgroundColor(Color.parseColor("#382F6A"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        errorOverlay.visibility = View.GONE
                        WebViewStorageHelper.reloadFromCache(webView)
                    }
                }
                addView(cacheBtn)
            }
            addView(btnRow)
        }
        rootLayout.addView(errorOverlay)

        // 5. Setup Window Insets (Edge to edge)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootLayout.setOnApplyWindowInsetsListener { view, insets ->
                val statusBar = insets.getInsets(WindowInsets.Type.statusBars())
                val navBar = insets.getInsets(WindowInsets.Type.navigationBars())
                view.setPadding(statusBar.left, statusBar.top, statusBar.right, navBar.bottom)
                insets
            }
        }

        // 6. Set initial online state
        onNetworkStatusChanged(networkMonitor.isOnline)
    }

    fun onNetworkStatusChanged(online: Boolean) {
        isOnline = online
        offlineBanner.visibility = if (online) View.GONE else View.VISIBLE
        webView?.settings?.cacheMode = if (online) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    private fun updateProgress(newProgress: Int) {
        if (newProgress in 1..99) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = newProgress
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun updateErrorState(hasError: Boolean, desc: String?) {
        if (hasError && !isOnline) {
            errorOverlay.visibility = View.VISIBLE
        } else {
            errorOverlay.visibility = View.GONE
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        val newDarkTheme = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (newDarkTheme != isDarkTheme) {
            isDarkTheme = newDarkTheme
            webView?.let { wv ->
                applySystemThemeSettings(wv.settings, isDarkTheme)
                reportSystemThemeToWeb(wv, isDarkTheme)
            }
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
