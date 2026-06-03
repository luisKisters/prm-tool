package com.prmtool.app.net

import com.prmtool.app.data.db.ContactEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Posts a contact to the n8n webhook as multipart/form-data.
 * Text fields land in the Webhook node's body; the voice file appears under its binary property.
 */
object WebhookSender {

    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    fun send(url: String, contact: ContactEntity) {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("firstName", contact.firstName)
            .addFormDataPart("lastName", contact.lastName)
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

        val request = Request.Builder().url(url).post(builder.build()).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Webhook returned HTTP ${response.code}")
            }
        }
    }
}
