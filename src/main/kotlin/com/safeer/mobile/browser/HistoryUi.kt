package com.safeer.mobile.browser

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * 🕒 Zgodovina: iskanje, razvrstitev po dnevih, odpiranje v tem ali novem zavihku, brisanje posameznih vnosov,
 * in predlogi iz zgodovine ter zaznamkov med tipkanjem v naslovno vrstico.
 */
object HistoryUi {
    private const val BG = "#0F172A"
    private const val CARD = "#1E293B"
    private const val TEXT = "#F1F5F9"
    private const val MUTED = "#94A3B8"
    private const val ACCENT = "#C7D2FE"

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun hostOf(url: String): String = try { Uri.parse(url).host ?: url } catch (_: Exception) { url }

    /** "Danes", "Včeraj" or the date, for grouping. */
    fun dayLabel(context: Context, timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val day = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val sameDay = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return I18n.t(context, "history_today")
        today.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        if (yesterday) return I18n.t(context, "history_yesterday")
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }

    fun show(context: Context, repository: BrowserRepository, open: (String) -> Unit, openInNewTab: (String) -> Unit) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(BG))
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 8))
        }
        val title = TextView(context).apply {
            text = "🕒 " + I18n.t(context, "history_title")
            setTextColor(Color.parseColor(TEXT)); textSize = 18f; setTypeface(null, Typeface.BOLD)
        }
        root.addView(title)
        val search = EditText(context).apply {
            hint = I18n.t(context, "history_search_hint")
            setHintTextColor(Color.parseColor(MUTED)); setTextColor(Color.parseColor(TEXT))
            setSingleLine(); setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
            setBackgroundColor(Color.parseColor(CARD))
        }
        root.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(context, 8) })
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(context).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(context, 8) })

        val dialog = AlertDialog.Builder(context)
            .setView(root)
            .setPositiveButton(I18n.t(context, "history_clear"), null)
            .setNegativeButton(I18n.t(context, "close"), null)
            .create()

        fun render(query: String) {
            list.removeAllViews()
            val items = repository.searchHistory(query, 300)
            if (items.isEmpty()) {
                list.addView(TextView(context).apply {
                    text = I18n.t(context, "history_empty"); setTextColor(Color.parseColor(MUTED)); setPadding(0, dp(context, 24), 0, dp(context, 24)); gravity = Gravity.CENTER
                })
                return
            }
            var lastDay = ""
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
            for (item in items) {
                val day = dayLabel(context, item.timestamp)
                if (day != lastDay) {
                    lastDay = day
                    list.addView(TextView(context).apply {
                        text = day; setTextColor(Color.parseColor(ACCENT)); textSize = 12f; setTypeface(null, Typeface.BOLD)
                        setPadding(0, dp(context, 12), 0, dp(context, 4))
                    })
                }
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
                    setBackgroundColor(Color.parseColor(CARD))
                    isClickable = true; isFocusable = true
                }
                row.addView(TextView(context).apply {
                    text = item.title.ifEmpty { item.url }; setTextColor(Color.parseColor(TEXT)); textSize = 15f; maxLines = 2
                })
                row.addView(TextView(context).apply {
                    text = timeFormat.format(Date(item.timestamp)) + " · " + hostOf(item.url); setTextColor(Color.parseColor(MUTED)); textSize = 12f; maxLines = 1
                })
                row.setOnClickListener { dialog.dismiss(); open(item.url) }
                row.setOnLongClickListener {
                    val options = arrayOf(I18n.t(context, "history_open_new_tab"), I18n.t(context, "history_copy"), I18n.t(context, "history_delete"))
                    AlertDialog.Builder(context).setTitle(item.title.ifEmpty { item.url }).setItems(options) { _, which ->
                        when (which) {
                            0 -> { dialog.dismiss(); openInNewTab(item.url) }
                            1 -> {
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("url", item.url))
                                Toast.makeText(context, I18n.t(context, "history_copied"), Toast.LENGTH_SHORT).show()
                            }
                            2 -> { repository.removeHistory(item.id); render(search.text.toString()) }
                        }
                    }.show()
                    true
                }
                list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(context, 6) })
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })
        render("")
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                AlertDialog.Builder(context)
                    .setMessage(I18n.t(context, "history_clear_confirm"))
                    .setPositiveButton(I18n.t(context, "history_clear")) { _, _ ->
                        repository.clearHistory()
                        Toast.makeText(context, I18n.t(context, "history_cleared"), Toast.LENGTH_SHORT).show()
                        render(search.text.toString())
                    }
                    .setNegativeButton(I18n.t(context, "close"), null)
                    .show()
            }
        }
        dialog.show()
    }
}

/** Predlogi pod naslovno vrstico: zadetki iz zgodovine in zaznamkov, medtem ko uporabnik tipka. */
class HistorySuggestions(
    private val context: Context,
    private val anchor: View,
    private val repository: BrowserRepository,
    private val onPick: (String) -> Unit,
) {
    private data class Suggestion(val title: String, val url: String, val bookmark: Boolean)

    private val popup = ListPopupWindow(context)
    private var suggestions: List<Suggestion> = emptyList()
    private val adapter = object : BaseAdapter() {
        override fun getCount(): Int = suggestions.size
        override fun getItem(position: Int): Any = suggestions[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = suggestions[position]
            val density = context.resources.displayMetrics.density
            val row = (convertView as? LinearLayout) ?: LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
                addView(TextView(context).apply { setTextColor(Color.parseColor("#F1F5F9")); textSize = 15f; maxLines = 1 })
                addView(TextView(context).apply { setTextColor(Color.parseColor("#94A3B8")); textSize = 12f; maxLines = 1 })
            }
            (row.getChildAt(0) as TextView).text = (if (item.bookmark) "⭐ " else "🕒 ") + item.title.ifEmpty { item.url }
            (row.getChildAt(1) as TextView).text = item.url
            return row
        }
    }

    init {
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.parseColor("#1E293B")))
        popup.isModal = false
        popup.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        popup.setOnItemClickListener { _, _, position, _ ->
            val picked = suggestions.getOrNull(position) ?: return@setOnItemClickListener
            dismiss()
            onPick(picked.url)
        }
    }

    /** Called on every keystroke; shows up to 6 matches, newest history first, bookmarks first of all. */
    fun update(typed: String) {
        val query = typed.trim()
        if (query.length < 2 || query.contains("://") && query.length > 40) { dismiss(); return }
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val seen = HashSet<String>()
        val result = ArrayList<Suggestion>()
        for (b in repository.getBookmarks()) {
            val hay = (b.title + " " + b.url).lowercase()
            if (words.all { hay.contains(it) } && seen.add(b.url)) result.add(Suggestion(b.title, b.url, true))
            if (result.size >= 3) break
        }
        for (h in repository.searchHistory(query, 12)) {
            if (seen.add(h.url)) result.add(Suggestion(h.title, h.url, false))
            if (result.size >= 6) break
        }
        suggestions = result
        if (result.isEmpty()) { dismiss(); return }
        adapter.notifyDataSetChanged()
        popup.width = anchor.width.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT
        if (!popup.isShowing) popup.show()
    }

    fun dismiss() { if (popup.isShowing) popup.dismiss() }
}
