package io.constructor.data.remote

/**
 * @suppress
 */
object ApiPaths {
    const val URL_AUTOCOMPLETE = "autocomplete/%s"
    const val URL_AUTOCOMPLETE_SELECT_EVENT = "autocomplete/{term}/select"
    const val URL_SEARCH_SUBMIT_EVENT = "autocomplete/{term}/search"
    const val URL_SESSION_START_EVENT = "behavior"
    const val URL_CONVERSION_EVENT = "v2/behavioral_action/conversion"
    const val URL_SEARCH_RESULT_CLICK_EVENT = "autocomplete/{term}/click_through"
    const val URL_BEHAVIOR = "behavior"
    const val URL_PURCHASE = "v2/behavioral_action/purchase"
    const val URL_SEARCH = "search/%s"
    const val URL_BROWSE = "browse/%s/%s"
    const val URL_BROWSE_RESULT_CLICK_EVENT = "v2/behavioral_action/browse_result_click"
    const val URL_BROWSE_RESULT_LOAD_EVENT = "v2/behavioral_action/browse_result_load"
    const val URL_RECOMMENDATIONS = "recommendations/v1/pods/%s"
    const val URL_RECOMMENDATION_RESULT_CLICK_EVENT = "v2/behavioral_action/recommendation_result_click"
    const val URL_RECOMMENDATION_RESULT_VIEW_EVENT = "v2/behavioral_action/recommendation_result_view"
}
