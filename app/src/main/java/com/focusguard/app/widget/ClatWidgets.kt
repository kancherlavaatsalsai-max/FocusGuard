package com.focusguard.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focusguard.app.MainActivity
import com.focusguard.app.R
import com.focusguard.app.util.WidgetStore
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Call this whenever the exam date, reels or focus time change. */
object ClatWidgets {
    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        listOf(ClatWidgetSmall::class.java, ClatWidgetWide::class.java).forEach { cls ->
            val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                val provider = cls.getDeclaredConstructor().newInstance()
                ids.forEach { id -> mgr.updateAppWidget(id, provider.build(context)) }
            }
        }
    }
}

abstract class BaseClatWidget : AppWidgetProvider() {

    protected abstract val layoutId: Int
    protected abstract fun bind(context: Context, views: RemoteViews)

    internal fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, layoutId)
        val days = WidgetStore.daysLeft(context)
        views.setTextViewText(R.id.w_days, days.toString())
        views.setTextViewText(
            R.id.w_label,
            when (days) {
                0 -> "EXAM DAY 🍀"
                1 -> "day left"
                else -> "days left"
            }
        )
        bind(context, views)

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        return views
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                ClatWidgets.updateAll(context)
        }
    }
}

class ClatWidgetSmall : BaseClatWidget() {
    override val layoutId = R.layout.widget_clat_small
    override fun bind(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.w_title, "CLAT")
    }
}

class ClatWidgetWide : BaseClatWidget() {
    override val layoutId = R.layout.widget_clat_wide
    override fun bind(context: Context, views: RemoteViews) {
        val date = WidgetStore.examDate(context)
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
        views.setTextViewText(R.id.w_title, "CLAT · $date")
        views.setProgressBar(R.id.w_progress, 100, WidgetStore.prepProgress(context), false)

        val mins = WidgetStore.focusMinutesToday(context)
        val focus = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
        views.setTextViewText(R.id.w_line1, "🎯 Focus today: $focus")
        views.setTextViewText(R.id.w_line2, "📱 Reels today: ${WidgetStore.reelsToday(context)}")
    }
}
