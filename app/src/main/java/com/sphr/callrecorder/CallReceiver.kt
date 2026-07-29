package com.sphr.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var wasRinging = false
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                lastState = stateStr
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                lastState = stateStr
                val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_START
                    putExtra(CallRecordingService.EXTRA_NUMBER, number ?: "unknown")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    val stopIntent = Intent(context, CallRecordingService::class.java).apply {
                        action = CallRecordingService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                }
                lastState = stateStr
                wasRinging = false
            }
        }
    }
}
