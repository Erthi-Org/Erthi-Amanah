package com.erthi.amanah.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // WorkManager re-enqueues persisted periodic work automatically on boot.
            // This receiver exists to satisfy the manifest declaration.
        }
    }
}
