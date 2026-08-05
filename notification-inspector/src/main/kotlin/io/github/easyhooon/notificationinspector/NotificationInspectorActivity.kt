package io.github.easyhooon.notificationinspector

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotificationInspectorActivity : Activity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: NotificationInspectorStore
    private lateinit var countText: TextView
    private lateinit var payloadText: TextView
    private lateinit var filterButtons: Map<NotificationFilterTag, Button>
    private var selectedFilter = NotificationFilterTag.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = NotificationInspectorStore(this)
        setContentView(createContentView())
        renderAsync()
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun createContentView(): LinearLayout {
        val rootPadding = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(rootPadding, rootPadding, rootPadding, rootPadding)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(
            TextView(this).apply {
                text = "Notification Inspector"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(18, 25, 33))
            },
        )

        root.addView(
            TextView(this).apply {
                text = "RemoteMessage and local notification snapshots captured by this app. " +
                    "APNs fields and the original HTTP v1 JSON are not available on Android."
                textSize = 13f
                setTextColor(Color.rgb(84, 93, 105))
                setPadding(0, dp(6), 0, dp(12))
            },
        )

        countText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(84, 93, 105))
        }
        root.addView(countText)

        root.addView(createFilters())
        root.addView(createActions())

        payloadText = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(18, 25, 33))
            setTextIsSelectable(true)
            setPadding(0, dp(12), 0, dp(24))
        }

        root.addView(
            ScrollView(this).apply {
                addView(payloadText)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        return root
    }

    private fun createFilters(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)

            filterButtons = NotificationFilterTag.entries.associateWith { tag ->
                actionButton(tag.label) {
                    selectedFilter = tag
                    renderAsync()
                }.also(::addView)
            }
        }
    }

    private fun createActions(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)

            addView(actionButton("Reload") { renderAsync() })
            addView(actionButton("Copy") { copyPayloads() })
            addView(actionButton("Clear") { clearPayloads() })
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun renderAsync() {
        activityScope.launch {
            render(store.readAll())
        }
    }

    private fun render(snapshots: List<JSONObject>) {
        val filteredSnapshots = snapshots.filterBy(selectedFilter)
        updateFilterButtons()
        countText.text = "${filteredSnapshots.size} / ${snapshots.size} captured message(s)"
        payloadText.text = if (filteredSnapshots.isEmpty()) {
            "No notification payload captured yet."
        } else {
            filteredSnapshots
                .asReversed()
                .joinToString(separator = "\n\n---\n\n") { it.toString(2) }
        }
    }

    private fun copyPayloads() {
        val text = payloadText.text.toString()
        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Notification payloads", text))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun clearPayloads() {
        activityScope.launch {
            store.clear()
            render(emptyList())
            Toast.makeText(this@NotificationInspectorActivity, "Cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun List<JSONObject>.filterBy(tag: NotificationFilterTag): List<JSONObject> {
        if (tag == NotificationFilterTag.ALL) {
            return this
        }

        return filter { snapshot ->
            snapshot.optString("tag") == tag.jsonValue
        }
    }

    private fun updateFilterButtons() {
        filterButtons.forEach { (tag, button) ->
            val selected = tag == selectedFilter
            button.isSelected = selected
            button.alpha = if (selected) 1f else 0.58f
            button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
