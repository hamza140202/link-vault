package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge helper placed in package android.print to access package-private
 * constructors of PrintDocumentAdapter.LayoutResultCallback and WriteResultCallback
 * without reflection or external dependencies.
 *
 * Implements strict single-flight synchronization and proper Android PrintDocumentAdapter
 * lifecycle (onStart -> onLayout -> onWrite -> onFinish) to prevent Chromium's
 * AwPdfExporter from throwing "printing is already pending".
 */
public final class WebPrintToPdfAdapter {

    public interface Callback {
        void onSuccess();
        void onError(String error);
    }

    private static final Object PRINT_LOCK = new Object();

    private WebPrintToPdfAdapter() {}

    public static void printPdf(
            final PrintDocumentAdapter adapter,
            final File destFile,
            final PrintAttributes attributes,
            final Callback callback
    ) {
        synchronized (PRINT_LOCK) {
            final AtomicBoolean isFinished = new AtomicBoolean(false);

            try {
                // 1. Android PrintDocumentAdapter contract requires onStart()
                adapter.onStart();

                adapter.onLayout(
                        null,
                        attributes,
                        new CancellationSignal(),
                        new PrintDocumentAdapter.LayoutResultCallback() {
                            @Override
                            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                                ParcelFileDescriptor pfd = null;
                                try {
                                    if (!destFile.exists()) {
                                        File parent = destFile.getParentFile();
                                        if (parent != null && !parent.exists()) {
                                            parent.mkdirs();
                                        }
                                        destFile.createNewFile();
                                    }

                                    pfd = ParcelFileDescriptor.open(
                                            destFile,
                                            ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_READ_WRITE
                                    );

                                    final ParcelFileDescriptor fileDescriptor = pfd;

                                    adapter.onWrite(
                                            new PageRange[]{PageRange.ALL_PAGES},
                                            fileDescriptor,
                                            new CancellationSignal(),
                                            new PrintDocumentAdapter.WriteResultCallback() {
                                                @Override
                                                public void onWriteFinished(PageRange[] pages) {
                                                    cleanup(adapter, fileDescriptor);
                                                    if (!isFinished.compareAndSet(false, true)) return;

                                                    if (destFile.exists() && destFile.length() > 0) {
                                                        callback.onSuccess();
                                                    } else {
                                                        callback.onError("Generated PDF file is empty or missing");
                                                    }
                                                }

                                                @Override
                                                public void onWriteFailed(CharSequence error) {
                                                    cleanup(adapter, fileDescriptor);
                                                    if (!isFinished.compareAndSet(false, true)) return;
                                                    callback.onError(error != null ? error.toString() : "Print adapter write failed");
                                                }

                                                @Override
                                                public void onWriteCancelled() {
                                                    cleanup(adapter, fileDescriptor);
                                                    if (!isFinished.compareAndSet(false, true)) return;
                                                    callback.onError("PDF generation cancelled");
                                                }
                                            }
                                    );
                                } catch (Exception e) {
                                    cleanup(adapter, pfd);
                                    if (isFinished.compareAndSet(false, true)) {
                                        callback.onError("Failed during PDF writing: " + e.getMessage());
                                    }
                                }
                            }

                            @Override
                            public void onLayoutFailed(CharSequence error) {
                                cleanup(adapter, null);
                                if (isFinished.compareAndSet(false, true)) {
                                    callback.onError(error != null ? error.toString() : "Print adapter layout failed");
                                }
                            }

                            @Override
                            public void onLayoutCancelled() {
                                cleanup(adapter, null);
                                if (isFinished.compareAndSet(false, true)) {
                                    callback.onError("PDF layout cancelled");
                                }
                            }
                        },
                        new Bundle()
                );
            } catch (Exception e) {
                cleanup(adapter, null);
                if (isFinished.compareAndSet(false, true)) {
                    callback.onError("Failed to start print adapter: " + e.getMessage());
                }
            }
        }
    }

    private static void cleanup(PrintDocumentAdapter adapter, ParcelFileDescriptor pfd) {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (Exception ignored) {}
        }
        try {
            adapter.onFinish();
        } catch (Exception ignored) {}
    }
}
