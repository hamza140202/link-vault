package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.File;

/**
 * Bridge helper placed in package android.print to access package-private
 * constructors of PrintDocumentAdapter.LayoutResultCallback and WriteResultCallback
 * without reflection or external dependencies.
 */
public final class WebPrintToPdfAdapter {

    public interface Callback {
        void onSuccess();
        void onError(String error);
    }

    private WebPrintToPdfAdapter() {}

    public static void printPdf(
            final PrintDocumentAdapter adapter,
            final File destFile,
            final PrintAttributes attributes,
            final Callback callback
    ) {
        adapter.onLayout(
                null,
                attributes,
                new CancellationSignal(),
                new PrintDocumentAdapter.LayoutResultCallback() {
                    @Override
                    public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                        try {
                            if (!destFile.exists()) {
                                File parent = destFile.getParentFile();
                                if (parent != null && !parent.exists()) {
                                    parent.mkdirs();
                                }
                                destFile.createNewFile();
                            }

                            final ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                                    destFile,
                                    ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_READ_WRITE
                            );

                            adapter.onWrite(
                                    new PageRange[]{PageRange.ALL_PAGES},
                                    fileDescriptor,
                                    new CancellationSignal(),
                                    new PrintDocumentAdapter.WriteResultCallback() {
                                        @Override
                                        public void onWriteFinished(PageRange[] pages) {
                                            try {
                                                fileDescriptor.close();
                                            } catch (Exception ignored) {
                                            }

                                            if (destFile.exists() && destFile.length() > 0) {
                                                callback.onSuccess();
                                            } else {
                                                callback.onError("Generated PDF file is empty or missing");
                                            }
                                        }

                                        @Override
                                        public void onWriteFailed(CharSequence error) {
                                            try {
                                                fileDescriptor.close();
                                            } catch (Exception ignored) {
                                            }
                                            callback.onError(error != null ? error.toString() : "Print adapter write failed");
                                        }

                                        @Override
                                        public void onWriteCancelled() {
                                            try {
                                                fileDescriptor.close();
                                            } catch (Exception ignored) {
                                            }
                                            callback.onError("PDF generation cancelled");
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            callback.onError("Failed to open file descriptor: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onLayoutFailed(CharSequence error) {
                        callback.onError(error != null ? error.toString() : "Print adapter layout failed");
                    }

                    @Override
                    public void onLayoutCancelled() {
                        callback.onError("Print adapter layout cancelled");
                    }
                },
                null
        );
    }
}
