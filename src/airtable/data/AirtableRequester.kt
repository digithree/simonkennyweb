package co.simonkenny.web.airtable.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*


private const val STALE_DATA_INTERVAL = 86_400_400L // one day in ms

private var DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

class AirtableRequester {

    companion object {
        private val INSTANCE = AirtableRequester();
        private lateinit var TOKEN: String

        fun setToken(token: String) {
            TOKEN = token
        }

        fun getInstance() = INSTANCE
    }

    private var aboutLastAccessTime = Date()

    private val _aboutRecords = mutableListOf<AboutRecord>()
    val aboutRecords: List<AboutRecord>
        get () {
            if (_aboutRecords.isEmpty() || aboutLastAccessTime.before(Date(currentTimeMillis() - STALE_DATA_INTERVAL))) {
                _aboutRecords.clear()
                runBlocking {
                    _aboutRecords.addAll(requestAirtableAbout())
                }
            }
            return _aboutRecords
        }

    fun aboutRecordsFilter(type: String? = null, limit: Int? = null, order: AboutRecord.Order = AboutRecord.Order.START_DATE_DESC) =
        aboutRecords.filter { type?.let { type -> it.fields.type == type } ?: true }
            .sortedWith(order.comparator)
            .let { limit?.let { limit -> it.take(limit) } ?: it }

    private suspend fun requestAirtableAbout(): List<AboutRecord> {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val response: AirtableAboutAccessObject = client.request {
            url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/about")
            header("Authorization", "Bearer $TOKEN")
        }
        client.close()
        return response.records
    }
}

fun String.airtableDate() = DATE_FORMAT.parse(this)