package com.momostack.app.feature.screenshotvault.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

/**
 * Headless BroadcastReceiver that copies extracted screenshot text directly to clipboard
 * when the user taps "[Copy Text]" in the notification bar, without launching any UI.
 */
class CopyTextReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Screenshot Text", text))

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    companion object {
        const val ACTION_COPY_TEXT = "com.momostack.app.ACTION_COPY_SCREENSHOT_TEXT"
        const val EXTRA_TEXT = "extra_extracted_text"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
