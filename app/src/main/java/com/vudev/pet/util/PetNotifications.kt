package com.vudev.pet.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vudev.pet.MainActivity
import com.vudev.pet.R
import com.vudev.pet.service.PetOverlayService

object PetNotifications {

    const val CHANNEL_SERVICE = "pet_service"
    const val CHANNEL_REMINDER = "pet_reminder"
    const val ID_SERVICE = 1001
    const val ID_HUNGRY = 1002
    const val ID_SAD = 1004
    const val ID_NEGLECTED = 1005
    const val ID_BOOT = 1006

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val service = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.channel_service),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { setShowBadge(true) }
        manager.createNotificationChannel(service)
        manager.createNotificationChannel(reminder)
    }

    fun foreground(context: Context): Notification {
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hide = PendingIntent.getService(
            context,
            2,
            Intent(context, PetOverlayService::class.java).setAction(PetOverlayService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(context.getString(R.string.notif_service_title))
            .setContentText(context.getString(R.string.notif_service_text))
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_pet),
                    context.getString(R.string.action_hide),
                    hide,
                ).build(),
            )
            .setOngoing(true)
            .build()
    }

    fun hungry(context: Context, petName: String) {
        reminder(
            context,
            ID_HUNGRY,
            context.getString(R.string.notif_hungry_title),
            context.getString(R.string.notif_hungry_text, petName),
        )
    }

    fun sad(context: Context, petName: String) {
        reminder(
            context,
            ID_SAD,
            context.getString(R.string.notif_sad_title),
            context.getString(R.string.notif_sad_text, petName),
        )
    }

    fun neglected(context: Context, petName: String) {
        reminder(
            context,
            ID_NEGLECTED,
            context.getString(R.string.notif_neglect_title),
            context.getString(R.string.notif_neglect_text, petName),
        )
    }

    /** Sau khi máy khởi động lại: nhắc mở app thay vì tự bật foreground service (Android 15+ hạn chế). */
    fun bootReminder(context: Context) {
        reminder(
            context,
            ID_BOOT,
            context.getString(R.string.notif_boot_title),
            context.getString(R.string.notif_boot_text),
        )
    }

    fun reminder(context: Context, id: Int, title: String, text: String) {
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}