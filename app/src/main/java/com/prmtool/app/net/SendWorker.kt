package com.prmtool.app.net

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prmtool.app.PrmApplication
import com.prmtool.app.data.db.SendStatus
import kotlinx.coroutines.flow.first

/** Sends one stored contact to the webhook, retrying with backoff on failure. */
class SendWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val clientId = inputData.getString(KEY_CLIENT_ID) ?: return Result.failure()
        val container = (applicationContext as PrmApplication).container
        val repo = container.repository

        val contact = repo.getContact(clientId) ?: return Result.failure()
        val url = container.settings.webhookUrl.first()
        if (url.isBlank()) {
            repo.updateStatus(clientId, SendStatus.FAILED)
            return Result.failure()
        }

        return try {
            WebhookSender.send(url, contact)
            repo.updateStatus(clientId, SendStatus.SENT)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                repo.updateStatus(clientId, SendStatus.FAILED)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_CLIENT_ID = "clientId"
        private const val MAX_ATTEMPTS = 5
    }
}
