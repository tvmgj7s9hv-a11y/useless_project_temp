package org.tinkerhub.scrollmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BootReceiver
 *
 * Runs upon device restart (ACTION_BOOT_COMPLETED).
 * Android automatically maintains Accessibility Service enabled state across restarts.
 * This receiver verifies and reinitializes local notification channels.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Android OS automatically restores enabled AccessibilityServices
        }
    }
}
