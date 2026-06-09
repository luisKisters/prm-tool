package com.prmtool.app.net

import kotlinx.serialization.Serializable

/** Response from POST /api/enrich. */
@Serializable
data class EnrichResponse(
    val prmId: String = "",
    val transcript: String = "",
    val email: String = "",
    val linkedinUrl: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val companyDomain: String = "",
    val companyLinkedinUrl: String = "",
    val companyEmployees: Int? = null,
    val companyAddress: String = "",
    val companyEnriched: List<String> = emptyList(),
    val summary: String = "",
    val enriched: List<String> = emptyList(),
)

/** Request body for POST /api/company (resolve a domain to its website title). */
@Serializable
data class CompanyLookupRequest(
    val domain: String,
)

/** Response from POST /api/company. */
@Serializable
data class CompanyLookupResponse(
    val domain: String = "",
    val title: String = "",
)

/** Request body for POST /api/linkedin (re-derive headline + photo from an edited LinkedIn URL). */
@Serializable
data class LinkedinLookupRequest(
    val url: String,
    val name: String = "",
)

/** Response from POST /api/linkedin. Fields are empty when the profile can't be resolved. */
@Serializable
data class LinkedinLookupResponse(
    val linkedinUrl: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
)

/** Request body for POST /api/commit. */
@Serializable
data class CommitRequest(
    val prmId: String,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val company: String = "",
    val companyDomain: String = "",
    val companyLinkedinUrl: String = "",
    val companyEmployees: Int? = null,
    val companyAddress: String = "",
    val companyEnriched: List<String> = emptyList(),
    val number: String = "",
    val headline: String = "",
    val linkedinUrl: String = "",
    val avatarUrl: String = "",
    val summary: String = "",
    val note: String = "",
    val events: String = "",
    val sources: String = "",
    val sourceDetails: String = "",
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
