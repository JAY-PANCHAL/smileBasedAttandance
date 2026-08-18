package com.smileattendance.app

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * This app runs unattended on a kiosk stand — there's nobody to tap "OK" on a crash dialog.
 * Any uncaught exception gets logged, then the process is killed and an alarm relaunches
 * MainActivity a moment later so the check-in screen recovers on its own.
 */
class SmileAttendanceApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e(TAG, "Uncaught exception on kiosk — scheduling restart", throwable)
            try {
                scheduleRestart()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule restart", e)
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 800, pendingIntent)
    }

    companion object {
        private const val TAG = "SmileAttendanceApp"
    }
}
