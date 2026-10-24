package pl.statusaut

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hosting InfinityFree zasłania dane testem JavaScript (cookie __test). Zwykłe zapytanie HTTP
 * dostaje wtedy stronę HTML zamiast JSON. Ukryty WebView przechodzi ten test tak jak Chrome,
 * a ciasteczko trafia do wspólnego CookieManager, z którego korzysta OkHttp.
 */
object ChallengeSolver {
    suspend fun solve(context: Context, url: String): Boolean = withContext(Dispatchers.Main) {
        val appContext = context.applicationContext
        withTimeoutOrNull(20_000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val webView = WebView(appContext)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String) {
                        // Po rozwiązaniu wyzwania strona przekierowuje na adres z parametrem i=
                        if (finishedUrl.contains("i=") && cont.isActive) {
                            CookieManager.getInstance().flush()
                            cont.resume(true)
                            Handler(Looper.getMainLooper()).post { view.destroy() }
                        }
                    }
                }
                cont.invokeOnCancellation {
                    Handler(Looper.getMainLooper()).post {
                        webView.stopLoading()
                        webView.destroy()
                    }
                }
                webView.loadUrl(url)
            }
        } ?: false
    }
}
