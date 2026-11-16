package com.vudev.pet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vudev.pet.data.PetSettings
import com.vudev.pet.util.PetNotifications

/**
 * Sau khi máy khởi động lại, không tự bật overlay service (Android 12+ hạn chế foreground
 * service từ background). Chỉ gửi thông báo mời người dùng mở app; MainActivity sẽ tự bật
 * lại overlay nếu người dùng đã bật trước đó.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!PetSettings(context).overlayEnabled) return
        PetNotifications.bootReminder(context)
    }
}
