package com.winuicalc.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient

const val WINCALC_URL = "https://wincalc-lilac.vercel.app/"

@SuppressLint("SetJavaScriptEnabled")
fun createConfiguredWebView(
    context: Context,
    isOnline: Boolean,
    isDarkTheme: Boolean,
    onProgressChange: (Int) -> Unit,
    onErrorChange: (Boolean, String?) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 1. Enable Native Persistent Cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        try {
            CookieManager.setAcceptFileSchemeCookies(true)
        } catch (_: Exception) {}

        // 2. Comprehensive WebSettings for maximum Native LocalStorage, IndexedDB, & Caching
        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true

            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true

            cacheMode = if (isOnline) {
                WebSettings.LOAD_DEFAULT
            } else {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            }

            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            applySystemThemeSettings(this, isDarkTheme)
        }

        // 3. Service Worker Caching on Android N+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val swController = ServiceWorkerController.getInstance()
                swController.serviceWorkerWebSettings.cacheMode = if (isOnline) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
            }
        } catch (_: Exception) {}

        // 4. WebChromeClient for progress & permission handling
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChange(newProgress)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // 5. WebViewClient for navigation control, theme reporting, and cookie flushing
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host ?: ""

                if (host.contains("vercel.app") || host.contains("wincalc")) {
                    return false
                }

                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    false
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onErrorChange(false, null)
                onCanGoBackChange(view?.canGoBack() == true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onCanGoBackChange(view?.canGoBack() == true)

                reportSystemThemeToWeb(view, isDarkTheme)

                view?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            if (window.localStorage) {
                                var testKey = '__native_wincalc_storage_init__';
                                window.localStorage.setItem(testKey, '1');
                                window.localStorage.removeItem(testKey);
                            }
                        } catch (e) {
                            console.warn('LocalStorage test:', e);
                        }
                    })();
                    """.trimIndent(),
                    null
                )

                try {
                    cookieManager.flush()
                } catch (_: Exception) {}
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.description?.toString() ?: "Connection error"
                    } else {
                        "Connection error"
                    }
                    onErrorChange(true, description)
                }
            }
        }

        loadUrl(WINCALC_URL)
    }
}

fun applySystemThemeSettings(settings: WebSettings, isDarkTheme: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        settings.isAlgorithmicDarkeningAllowed = isDarkTheme
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        settings.forceDark = if (isDarkTheme) {
            WebSettings.FORCE_DARK_ON
        } else {
            WebSettings.FORCE_DARK_OFF
        }
    }
}

fun reportSystemThemeToWeb(webView: WebView?, isDark: Boolean) {
    val theme = if (isDark) "dark" else "light"
    val script = """
        (function() {
            try {
                var isDark = $isDark;
                var theme = '$theme';

                document.documentElement.style.colorScheme = theme;

                var meta = document.querySelector('meta[name="color-scheme"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'color-scheme';
                    document.head.appendChild(meta);
                }
                meta.content = theme;

                if (isDark) {
                    document.documentElement.classList.add('dark');
                    document.documentElement.classList.remove('light');
                    if (document.body) {
                        document.body.classList.add('dark');
                        document.body.classList.remove('light');
                    }
                } else {
                    document.documentElement.classList.add('light');
                    document.documentElement.classList.remove('dark');
                    if (document.body) {
                        document.body.classList.add('light');
                        document.body.classList.remove('dark');
                    }
                }

                document.documentElement.setAttribute('data-theme', theme);
                document.documentElement.setAttribute('data-color-mode', theme);
                document.documentElement.setAttribute('data-bs-theme', theme);

                window.dispatchEvent(new CustomEvent('themechange', { detail: { theme: theme, isDark: isDark } }));
                window.dispatchEvent(new CustomEvent('systemthemechange', { detail: { theme: theme, isDark: isDark } }));
                window.dispatchEvent(new Event('colorschemechange'));

                if (window.localStorage) {
                    window.localStorage.setItem('system-theme', theme);
                    var currentTheme = window.localStorage.getItem('theme');
                    if (!currentTheme || currentTheme === 'system') {
                        window.localStorage.setItem('theme', theme);
                    }
                }
            } catch (e) {
                console.warn('System theme report error:', e);
            }
        })();
    """.trimIndent()
    webView?.evaluateJavascript(script, null)
}

object WebViewStorageHelper {
    fun forceReload(webView: WebView?) {
        webView?.let { wv ->
            val prevCacheMode = wv.settings.cacheMode
            wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wv.reload()
            wv.postDelayed({
                wv.settings.cacheMode = prevCacheMode
            }, 3000)
        }
    }

    fun reloadFromCache(webView: WebView?) {
        webView?.let { wv ->
            wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            wv.reload()
        }
    }

    fun clearAllStorage(context: Context, webView: WebView?) {
        webView?.let { wv ->
            wv.clearCache(true)
            wv.clearHistory()
            wv.clearFormData()
        }
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {}
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        } catch (_: Exception) {}
        webView?.loadUrl(WINCALC_URL)
    }
}
