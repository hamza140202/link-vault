package com.momostack.app.feature.webarchive

import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
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

    private const val TIMEOUT_MS = 30_000L

    suspend fun convertUrlToPdf(
        context: Context,
        url: String,
        destFile: File
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post {
            var webView: WebView? = null
            var hasCompleted = false

            fun finishWithSuccess(file: File) {
                if (hasCompleted) return
                hasCompleted = true
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {}
                webView = null
                continuation.resume(Result.success(file))
            }

            fun finishWithError(error: Throwable) {
                if (hasCompleted) return
                hasCompleted = true
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {}
                webView = null
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

                        // Settle window for dynamic fonts and images
                        mainHandler.postDelayed({
                            if (hasCompleted || webView == null) return@postDelayed

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
                        }, 1200)
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
                    try {
                        webView?.stopLoading()
                        webView?.destroy()
                    } catch (_: Exception) {}
                    webView = null
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

        val adapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("momo_web_archive")

        adapter.onLayout(
            null,
            printAttributes,
            CancellationSignal(),
            object : PrintDocumentAdapter.LayoutResultCallback() {
                override fun onLayoutFinished(info: android.print.PrintDocumentInfo?, changed: Boolean) {
                    try {
                        val fileDescriptor = ParcelFileDescriptor.open(
                            destFile,
                            ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE
                        )

                        adapter.onWrite(
                            arrayOf(PageRange.ALL_PAGES),
                            fileDescriptor,
                            CancellationSignal(),
                            object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<out PageRange>?) {
                                    try {
                                        fileDescriptor.close()
                                    } catch (_: Exception) {}

                                    if (destFile.exists() && destFile.length() > 0) {
                                        onSuccess()
                                    } else {
                                        onError("Generated PDF file is empty or missing")
                                    }
                                }

                                override fun onWriteFailed(error: CharSequence?) {
                                    try {
                                        fileDescriptor.close()
                                    } catch (_: Exception) {}
                                    onError(error?.toString() ?: "Print adapter write failed")
                                }

                                override fun onWriteCancelled() {
                                    try {
                                        fileDescriptor.close()
                                    } catch (_: Exception) {}
                                    onError("Print adapter write cancelled")
                                }
                            }
                        )
                    } catch (e: Exception) {
                        onError("Failed to open file descriptor: ${e.message}")
                    }
                }

                override fun onLayoutFailed(error: CharSequence?) {
                    onError(error?.toString() ?: "Print adapter layout failed")
                }

                override fun onLayoutCancelled() {
                    onError("Print adapter layout cancelled")
                }
            },
            null
        )
    }
}
