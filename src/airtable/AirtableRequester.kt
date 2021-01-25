package airtable

import co.simonkenny.web.airtable.*
import io.ktor.client.*
import io.ktor.client.request.*
import java.text.SimpleDateFormat


private var DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

class AirtableRequester {

    lateinit var token: String

    companion object {
        private val INSTANCE = AirtableRequester();

        fun getInstance() = INSTANCE
    }

    val about: RequestWrapper<AboutRecord> by lazy {
        RequestWrapper(
            token,
            "about"
        ) { client: HttpClient, block ->
            client.request<AirtableAboutAccessObject> { block() }.records
        }
    }

    val media: RequestWrapper<MediaRecord> by lazy {
        RequestWrapper(
            token,
            "media-record"
        ) { client: HttpClient, block ->
            client.request<AirtableMediaAccessObject> { block() }.records
        }
    }
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