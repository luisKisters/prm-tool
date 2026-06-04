package com.prmtool.app.net

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prmtool.app.PrmApplication
import com.prmtool.app.data.db.SendStatus
import kotlinx.coroutines.flow.first

/** Enriches one stored contact via /api/enrich, then parks it as ENRICHED for review. */
class EnrichWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val clientId = inputData.getString(KEY_CLIENT_ID) ?: return Result.failure()
        val container = (applicationContext as PrmApplication).container
        val repo = container.repository

        val contact = repo.getContact(clientId) ?: return Result.failure()
        val baseUrl = container.settings.apiBaseUrl.first()
        val token = container.settings.apiToken.first()
        if (baseUrl.isBlank()) {
            repo.updateStatus(clientId, SendStatus.ENRICH_FAILED)
            return Result.failure()
        }

        repo.updateStatus(clientId, SendStatus.ENRICHING)
        return try {
            val result = ApiClient.enrich(baseUrl, token, contact)
            repo.applyEnrichment(clientId, result)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                repo.updateStatus(clientId, SendStatus.ENRICH_FAILED)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_CLIENT_ID = "clientId"
        private const val MAX_ATTEMPTS = 5
    }
}
