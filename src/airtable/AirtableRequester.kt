package airtable

import co.simonkenny.web.airtable.*
import io.ktor.client.*
import io.ktor.client.request.*
import java.text.SimpleDateFormat
import java.util.*

const val LIMIT_MAX = 100

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

    val articles: RequestWrapper<ArticlesRecord> by lazy {
        RequestWrapper(
            token,
            "articles"
        ) { client: HttpClient, block ->
            client.request<AirtableArticlesAccessObject> { block() }.records
        }
    }
}

fun String.airtableDate(): Date = DATE_FORMAT.parse(this)

fun compareAirtableDates(date1: String?, date2: String?, newestFirst: Boolean = true): Int =
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

fun <T>compare(c: (o1: T, o2: T) -> Int): Comparator<T> =
    Comparator { o1, o2 -> if (o1 == null || o2 == null) 0 else c(o1, o2) }