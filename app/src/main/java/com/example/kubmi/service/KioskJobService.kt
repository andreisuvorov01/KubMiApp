package com.example.kubmi.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.kubmi.MainActivity

/**
 * JobService для Android 12+ (S) и выше
 * Используется как дополнительный механизм защиты для обеспечения
 * постоянного запуска основного приложения
 * 
 * Преимущества:
 * - Работает даже если Foreground Service убит
 * - Более надёжен чем AlarmManager
 * - Требуемые разрешения минимальны
 */
@RequiresApi(Build.VERSION_CODES.S)
class KioskJobService : JobService() {
    
    companion object {
        private const val TAG = "KioskJobService"
        private const val JOB_ID = 42
        
        /**
         * Планирование периодической задачи для проверки и возврата приложения в foreground
         */
        fun schedule(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Log.w(TAG, "JobScheduler requires Android 12+")
                return
            }
            
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            
            val jobInfo = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, KioskJobService::class.java)
            ).apply {
                // Запускать задачу каждые 15 минут
                setPeriodic(15 * 60 * 1000)
                
                // Не требуем сетевого соединения
                setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                
                // Сохранять задачу после перезагрузки
                setPersisted(true)
                
                // Запускать даже на низком уровне батареи
                // (можно изменить на setRequiresDeviceIdle(true) если нужно)
            }.build()
            
            val result = jobScheduler.schedule(jobInfo)
            
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "✓ Job scheduled successfully (ID: $JOB_ID)")
            } else {
                Log.e(TAG, "✗ Failed to schedule job")
            }
        }
        
        /**
         * Отмена запланированной задачи
         */
        fun cancel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.cancel(JOB_ID)
            Log.d(TAG, "Job cancelled (ID: $JOB_ID)")
        }
    }
    
    override fun onStartJob(params: JobParameters): Boolean {
        Log.v(TAG, "onStartJob called")
        
        // Проверить и вернуть приложение в foreground
        checkAndRestartKiosk(params)
        
        // false = задача завершена
        // true = задача продолжается в фоне (нужно вызвать jobFinished)
        return false
    }
    
    override fun onStopJob(params: JobParameters): Boolean {
        Log.v(TAG, "onStopJob called")
        
        // true = переплнировать задачу при следующей возможности
        // false = не переплнировать
        return true
    }
    
    /**
     * Проверка и восстановление приложения в foreground
     */
    private fun checkAndRestartKiosk(params: JobParameters) {
        try {
            // Получить ActivityManager для проверки что в foreground
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val packageName = packageName
            
            // Проверить если наше приложение в foreground
            val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager.appTasks.any { 
                    it.taskInfo?.topActivity?.packageName == packageName 
                }
            } else {
                @Suppress("DEPRECATION")
                activityManager.getRunningTasks(1)?.firstOrNull()?.topActivity?.packageName == packageName
            }
            
            if (!isForeground) {
                Log.w(TAG, "Kiosk app not in foreground, restoring...")
                restoreKioskApp()
            } else {
                Log.v(TAG, "Kiosk app is already in foreground")
            }
            
            jobFinished(params, false) // Задача успешно завершена
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndRestartKiosk", e)
            jobFinished(params, true) // Переплнировать при ошибке
        }
    }
    
    /**
     * Восстановление основного приложения в foreground
     */
    private fun restoreKioskApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            startActivity(intent)
            Log.d(TAG, "✓ Kiosk app restored to foreground")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to restore kiosk app", e)
        }
    }
}
