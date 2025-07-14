// 2. WorkScheduler.kt
package com.example.myapplication.worker

import android.content.Context
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_WORK = "hourly_reminder"

    /** 다음 실행 시점까지의 밀리초 딜레이 계산 */
    private fun calculateInitialDelay(startHour: Int): Long {
        val now = Calendar.getInstance()
        val next = now.clone() as Calendar

        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        // 현재 시간이 시작 전(9시 이전)이면, 오늘 startHour시로
        // 시작 후(9시 이후)면, 다음 정각(다음 시간)로
        if (currentHour < startHour) {
            next.set(Calendar.HOUR_OF_DAY, startHour)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)
        } else {
            // 다음 “정각” (분·초 → 0) +1시간
            next.add(Calendar.HOUR_OF_DAY, 1)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)
        }
        return next.timeInMillis - now.timeInMillis
    }

    /** 매시간 정각에 ReminderWorker 실행 예약 */
    fun scheduleHourlyReminder(context: Context, startHour: Int = 9, endHour: Int = 20) {
        val initialDelay = calculateInitialDelay(startHour)

        // Worker에 시간대 파라미터 전달
        val input = Data.Builder()
            .putInt("startHour", startHour)
            .putInt("endHour", endHour)
            .build()

        val req = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.HOURS  // 1시간 주기
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                req
            )
    }
}

