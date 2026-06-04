package com.prmtool.app.net

import kotlinx.serialization.Serializable

/** Response from POST /api/enrich. */
@Serializable
data class EnrichResponse(
    val prmId: String = "",
    val transcript: String = "",
    val linkedinUrl: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val companyDomain: String = "",
    val summary: String = "",
    val enriched: List<String> = emptyList(),
)

/** Request body for POST /api/commit. */
@Serializable
data class CommitRequest(
    val prmId: String,
    val firstName: String = "",
    val lastName: String = "",
    val company: String = "",
    val companyDomain: String = "",
    val number: String = "",
    val headline: String = "",
    val linkedinUrl: String = "",
    val avatarUrl: String = "",
    val summary: String = "",
    val note: String = "",
    val events: String = "",
    val sources: String = "",
    val enriched: List<String> = emptyList(),
)

/** Response from POST /api/commit. */
@Serializable
data class CommitResponse(
    val prmId: String = "",
    val twentyId: String? = null,
    val twentyUrl: String? = null,
    val googleResourceName: String? = null,
)
