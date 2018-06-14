package io.constructor.core

class Constants {

    companion object {
        val EVENT_QUERY_SENT = this::class.qualifiedName + "query_sent"
        val EVENT_SUGGESTIONS_RETRIEVED = this::class.qualifiedName + "suggestions_retrieved"
        val EXTRA_QUERY = this::class.qualifiedName + "query"
        val EXTRA_TERM = this::class.qualifiedName + "term"
        val EXTRA_SUGGESTIONS = this::class.qualifiedName + "suggestions"
        val EXTRA_SUGGESTION = this::class.qualifiedName + "suggestion"
    }

    object QueryConstants {
        const val SESSION = "s"
        const val TIMESTAMP = "_dt"
        const val IDENTITY = "i"
        const val ITEM_ID = "item_id"
        const val ACTION = "action"
        const val AUTOCOMPLETE_SECTION = "autocomplete_section"
        const val ORIGINAL_QUERY = "original_query"
        const val CLIENT = "c"
        const val EVENT = "tr"
        const val AUTOCOMPLETE_KEY = "autocomplete_key"
        const val GROUP_ID = "group[group_id]"
        const val GROUP_DISPLAY_NAME = "group[display_name]"
    }

    object QueryValues {
        const val EVENT_CLICK = "click"
        const val EVENT_SEARCH = "search"
        const val EVENT_SESSION_START = "session_start"
        const val SEARCH_SUGGESTIONS = "Search Suggestions"
        const val EVENT_SEARCH_RESULTS = "search-results"
        const val EVENT_INPUT_FOCUS = "focus"
    }
}