package com.momostack.app.feature.webarchive

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object WebPdfExtractor {

    private const val TIMEOUT_MS = 35_000L

    suspend fun convertUrlToPdf(
        context: Context,
        url: String,
        destFile: File
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post {
            var webView: WebView? = null
            var hasCompleted = false
            var isRenderTriggered = false
            var settleRunnable: Runnable? = null

            fun cleanup() {
                settleRunnable?.let { mainHandler.removeCallbacks(it) }
                settleRunnable = null
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {}
                webView = null
            }

            fun finishWithSuccess(file: File) {
                if (hasCompleted) return
                hasCompleted = true
                cleanup()
                continuation.resume(Result.success(file))
            }

            fun finishWithError(error: Throwable) {
                if (hasCompleted) return
                hasCompleted = true
                cleanup()
                if (destFile.exists()) {
                    destFile.delete()
                }
                continuation.resume(Result.failure(error))
            }

            val timeoutRunnable = Runnable {
                finishWithError(Exception("Web-to-PDF conversion timed out after ${TIMEOUT_MS / 1000}s"))
            }
            mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

            try {
                destFile.parentFile?.mkdirs()
                if (destFile.exists()) destFile.delete()

                val view = WebView(context.applicationContext)
                webView = view

                view.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 MomoStackReader/1.0"
                }

                view.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView?, loadedUrl: String?) {
                        super.onPageFinished(v, loadedUrl)

                        // Clear any pending settle runnables from previous onPageFinished invocations
                        settleRunnable?.let { mainHandler.removeCallbacks(it) }

                        if (hasCompleted || isRenderTriggered || webView == null) {
                            return
                        }

                        // Settle window for dynamic fonts, CSS layout, and inline images (2000ms debounce)
                        val runnable = Runnable {
                            if (hasCompleted || isRenderTriggered || webView == null) return@Runnable
                            isRenderTriggered = true

                            try {
                                renderWebViewToPdf(
                                    webView = view,
                                    destFile = destFile,
                                    onSuccess = {
                                        mainHandler.removeCallbacks(timeoutRunnable)
                                        finishWithSuccess(destFile)
                                    },
                                    onError = { err ->
                                        mainHandler.removeCallbacks(timeoutRunnable)
                                        finishWithError(Exception(err))
                                    }
                                )
                            } catch (e: Exception) {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                finishWithError(e)
                            }
                        }
                        settleRunnable = runnable
                        mainHandler.postDelayed(runnable, 2000L)
                    }

                    override fun onReceivedError(
                        v: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(v, request, error)
                        if (request?.isForMainFrame == true) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            finishWithError(Exception("Failed to load page: ${error?.description ?: "Network error"}"))
                        }
                    }
                }

                view.loadUrl(url)

            } catch (e: Exception) {
                mainHandler.removeCallbacks(timeoutRunnable)
                finishWithError(e)
            }

            continuation.invokeOnCancellation {
                mainHandler.removeCallbacks(timeoutRunnable)
                mainHandler.post {
                    cleanup()
                }
            }
        }
    }

    private fun renderWebViewToPdf(
        webView: WebView,
        destFile: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf_render", "pdf_render", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        // Inject print-fidelity CSS to ensure full background colors, cards, and exact webpage appearance
        webView.evaluateJavascript(
            """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.innerHTML = '@media print { *, *::before, *::after { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; color-adjust: exact !important; } }';
                if (document.head) {
                    document.head.appendChild(style);
                } else if (document.body) {
                    document.body.appendChild(style);
                }
            })();
            """.trimIndent(),
            null
        )

        val adapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("momo_web_archive")

        android.print.WebPrintToPdfAdapter.printPdf(
            adapter,
            destFile,
            printAttributes,
            object : android.print.WebPrintToPdfAdapter.Callback {
                override fun onSuccess() {
                    onSuccess()
                }

                override fun onError(error: String) {
                    onError(error)
                }
            }
        )
    }
}
