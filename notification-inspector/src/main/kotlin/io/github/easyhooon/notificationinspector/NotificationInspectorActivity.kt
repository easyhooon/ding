package io.github.easyhooon.notificationinspector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class NotificationInspectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = NotificationInspectorStore(this)
        setContent {
            NotificationInspectorApp(
                store = store,
                onCopy = ::copyPayload,
                onShare = ::sharePayload,
                onCleared = {
                    PersistentNotificationController.refreshAsync(this, store)
                    Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    private fun copyPayload(payload: String) {
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Notification payload", payload))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun sharePayload(subject: String, payload: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        startActivity(Intent.createChooser(shareIntent, "Share notification payload"))
    }
}
