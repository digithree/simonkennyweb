package airtable

import co.simonkenny.web.airtable.about.AboutRequestWrapper
import co.simonkenny.web.airtable.media.MediaRequestWrapper
import java.text.SimpleDateFormat


const val STALE_DATA_INTERVAL = 86_400_400L // one day in ms

private var DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

class AirtableRequester {

    lateinit var token: String

    companion object {
        private val INSTANCE = AirtableRequester();

        fun getInstance() = INSTANCE
    }

    val about: AboutRequestWrapper by lazy { AboutRequestWrapper(token) }

    val media: MediaRequestWrapper by lazy { MediaRequestWrapper(token) }
}

fun String.airtableDate() = DATE_FORMAT.parse(this)

fun compareAirtableDates(date1: String?, date2: String?, newestFirst: Boolean = true) =
    if (date1 == null && date2 == null) {
        0
    } else if (date1 != null && date2 == null) {
        -1
    } else if (date2 != null && date1 == null) {
        1
    } else {
        if (newestFirst) date2?.airtableDate()?.compareTo(date1?.airtableDate()) ?: 0
        else date1?.airtableDate()?.compareTo(date2?.airtableDate()) ?: 0
    }