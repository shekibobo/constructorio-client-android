package io.constructor.data.model.search

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Models search redirect metadata
 */
data class RedirectData(
        @Json(name = "url") val url: String?,
        @Json(name = "rule_id") val rule_id: Int?,
        @Json(name = "match_id") val match_id: Int?
) : Serializable