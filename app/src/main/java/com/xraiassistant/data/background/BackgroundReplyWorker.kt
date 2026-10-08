package com.xraiassistant.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.xraiassistant.R
import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.repositories.AIProviderRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Finishes an AI reply after the user leaves the app. Android may stop the
 * app's own request once it is in the background or the device dozes;
 * WorkManager runs this job under the system's control, waits for a network,
 * and survives the app being closed.
 */
class BackgroundReplyWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun aiProviderRepository(): AIProviderRepository
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val store = BackgroundReplyStore.from(applicationContext)
        val job = store.job(jobId) ?: return Result.failure()
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .aiProviderRepository()

        return try {
            val text = repository.generateResponse(
                prompt = job.prompt,
                model = job.model,
                temperature = job.temperature,
                topP = job.topP,
                systemPrompt = job.systemPrompt,
                effort = AIEffort.fromApiValue(job.effort)
            )
            store.saveOutcome(BackgroundReplyStore.Outcome(jobId, text.ifBlank { null }, if (text.isBlank()) "Empty reply" else null, job.model))
            BackgroundReplyStore.announce(jobId)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 2 && com.xraiassistant.domain.errors.NetworkInterruption.isInterruption(e)) {
                Result.retry()
            } else {
                store.saveOutcome(BackgroundReplyStore.Outcome(jobId, null, e.message ?: "Request failed"))
                BackgroundReplyStore.announce(jobId)
                Result.failure()
            }
        }
    }

    /** Needed for expedited work on Android 11 and older, where it runs as a foreground service. */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Finishing replies", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Finishing your reply")
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val KEY_JOB_ID = "jobId"
        private const val CHANNEL = "background_replies"
        private const val NOTIFICATION_ID = 4107

        fun workName(jobId: String) = "background-reply-$jobId"

        fun enqueue(context: Context, jobId: String) {
            val request = OneTimeWorkRequestBuilder<BackgroundReplyWorker>()
                .setInputData(workDataOf(KEY_JOB_ID to jobId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(jobId), ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context, jobId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(jobId))
        }
    }
}
