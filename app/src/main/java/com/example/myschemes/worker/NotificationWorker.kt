package com.example.myschemes.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme  // ← добавить импорт
import com.example.myschemes.data.model.SchemeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val database = SchemeDatabase.getInstance(applicationContext)
                val schemes = database.schemeDao().getAllSchemes()

                val expiring = schemes.filter { it.getStatus() == SchemeStatus.EXPIRING }
                val expired = schemes.filter { it.getStatus() == SchemeStatus.EXPIRED }

                if (expiring.isNotEmpty() || expired.isNotEmpty()) {
                    showNotification(expiring, expired)
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun showNotification(expiring: List<Scheme>, expired: List<Scheme>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "schemes_channel",
                "MySchemes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о сроках пересмотра схем"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val message = buildString {
            if (expired.isNotEmpty()) {
                appendLine("🔴 Просрочено: ${expired.size}")
                expired.take(3).forEach { scheme: Scheme ->  // ← явно указали тип
                    appendLine("  • ${scheme.equipmentName}")
                }
                if (expired.size > 3) appendLine("  ... и ещё ${expired.size - 3}")
            }
            if (expiring.isNotEmpty()) {
                if (expired.isNotEmpty()) appendLine()
                appendLine("🟡 Скоро истекают: ${expiring.size}")
                expiring.take(3).forEach { scheme: Scheme ->  // ← явно указали тип
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    appendLine("  • ${scheme.equipmentName} (до ${dateFormat.format(Date(scheme.nextRevisionDate))})")
                }
                if (expiring.size > 3) appendLine("  ... и ещё ${expiring.size - 3}")
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, "schemes_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MySchemes: проверка сроков")
            .setContentText(message.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}