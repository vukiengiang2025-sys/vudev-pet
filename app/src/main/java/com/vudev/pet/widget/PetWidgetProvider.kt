package com.vudev.pet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vudev.pet.MainActivity
import com.vudev.pet.R
import com.vudev.pet.core.Mood
import com.vudev.pet.core.PetState
import com.vudev.pet.data.PetRepository

/** Widget màn hình chính: mặt thú, tên, mood và ba nhu cầu; bấm mở app. */
class PetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val state = PetRepository(context).load() ?: PetState()
        for (id in ids) manager.updateAppWidget(id, build(context, state))
    }

    companion object {
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, PetWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isEmpty()) return
                val state = PetRepository(context).load() ?: PetState()
                for (id in ids) manager.updateAppWidget(id, build(context, state))
            }
        }

        private fun build(context: Context, state: PetState): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_pet)
            views.setTextViewText(R.id.widget_name, state.name)
            views.setTextViewText(R.id.widget_mood, moodLabel(context, state.mood))
            views.setTextViewText(
                R.id.widget_needs,
                "Đói " + state.needs.hunger.toInt() + "% · Vui " + state.needs.happiness.toInt() +
                    "% · Năng lượng " + state.needs.energy.toInt() + "%",
            )
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        private fun moodLabel(context: Context, mood: Mood): String = when (mood) {
            Mood.CALM -> context.getString(R.string.mood_calm)
            Mood.HAPPY -> context.getString(R.string.mood_happy)
            Mood.EXCITED -> context.getString(R.string.mood_excited)
            Mood.CURIOUS -> context.getString(R.string.mood_curious)
            Mood.SAD -> context.getString(R.string.mood_sad)
            Mood.SLEEPY -> context.getString(R.string.mood_sleepy)
            Mood.ANGRY -> context.getString(R.string.mood_angry)
            Mood.SURPRISED -> context.getString(R.string.mood_surprised)
            Mood.SULK -> context.getString(R.string.mood_sulk)
        }
    }
}
