package com.prmtool.app.net

import com.prmtool.app.data.db.ContactEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the Vercel backend. Two calls:
 *  - [enrich]: multipart upload of the captured contact + voice, returns enrichment for review.
 *  - [commit]: JSON of the user-confirmed fields, writes to Twenty + Google Contacts.
 */
object ApiClient {

    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun endpoint(baseUrl: String, path: String): String =
        baseUrl.trim().trimEnd('/') + path

    fun enrich(baseUrl: String, token: String, contact: ContactEntity): EnrichResponse {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("firstName", contact.firstName)
            .addFormDataPart("lastName", contact.lastName)
            .addFormDataPart("email", contact.email)
            .addFormDataPart("company", contact.company)
            .addFormDataPart("number", contact.number)
            .addFormDataPart("note", contact.note)
            .addFormDataPart("events", contact.events)
            .addFormDataPart("sources", contact.sources)
            .addFormDataPart("clientId", contact.clientId)
            .addFormDataPart("createdAt", contact.createdAt.toString())

        contact.voicePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                builder.addFormDataPart(
                    "voice",
                    file.name,
                    file.asRequestBody("audio/mp4".toMediaType())
                )
            }
        }

        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/enrich"))
            .header("Authorization", "Bearer $token")
            .post(builder.build())
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Enrich failed: HTTP ${response.code}")
            }
            return json.decodeFromString(EnrichResponse.serializer(), body)
        }
    }

    /** Re-derive headline + avatar for a specific LinkedIn URL. Best-effort; fields may be blank. */
    fun lookupLinkedin(
        baseUrl: String,
        token: String,
        url: String,
        name: String,
    ): LinkedinLookupResponse {
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/linkedin"))
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(LinkedinLookupRequest(url, name)).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("LinkedIn lookup failed: HTTP ${response.code}")
            }
            return json.decodeFromString(LinkedinLookupResponse.serializer(), body)
        }
    }

    /** Resolve a company domain to its website title. Best-effort; title may be blank. */
    fun lookupCompany(
        baseUrl: String,
        token: String,
        domain: String,
    ): CompanyLookupResponse {
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/company"))
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(CompanyLookupRequest(domain)).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Company lookup failed: HTTP ${response.code}")
            }
            return json.decodeFromString(CompanyLookupResponse.serializer(), body)
        }
    }

    fun commit(baseUrl: String, token: String, payload: CommitRequest): CommitResponse {
        val request = Request.Builder()
            .url(endpoint(baseUrl, "/api/commit"))
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Commit failed: HTTP ${response.code}")
            }
            return json.decodeFromString(CommitResponse.serializer(), body)
        }
    }
}
