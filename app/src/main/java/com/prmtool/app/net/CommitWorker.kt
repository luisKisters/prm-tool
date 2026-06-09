package com.prmtool.app.net

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prmtool.app.PrmApplication
import com.prmtool.app.data.db.SendStatus
import kotlinx.coroutines.flow.first

/** Commits one reviewed contact to Twenty + Google Contacts via /api/commit. */
class CommitWorker(
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
            repo.updateStatus(clientId, SendStatus.COMMIT_FAILED)
            return Result.failure()
        }

        repo.updateStatus(clientId, SendStatus.COMMITTING)
        return try {
            val result = ApiClient.commit(baseUrl, token, repo.buildCommitRequest(contact))
            repo.applyCommitResult(clientId, result)
            requestContactsResync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                repo.updateStatus(clientId, SendStatus.COMMIT_FAILED)
                Result.failure()
            }
        }
    }

    /**
     * Ask the system to sync Google Contacts down to the device so the just-created contact shows
     * up in the phone's Contacts app promptly. A null account targets all accounts for the contacts
     * authority, so no account-access permission is needed. Best-effort; failures are ignored.
     */
    private fun requestContactsResync() {
        runCatching {
            val extras = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }
            ContentResolver.requestSync(null, ContactsContract.AUTHORITY, extras)
        }
    }

    companion object {
        const val KEY_CLIENT_ID = "clientId"
        private const val MAX_ATTEMPTS = 5
    }
}
