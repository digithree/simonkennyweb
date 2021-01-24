package co.simonkenny.web.airtable.media

import airtable.STALE_DATA_INTERVAL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.currentTimeMillis
import java.util.*

class MediaRequestWrapper(var token: String) {

    private var lastAccessTime = Date()

    private val _mediaRecords = mutableListOf<MediaRecord>()
    val mediaRecords: List<MediaRecord>
        get () {
            if (_mediaRecords.isEmpty() || lastAccessTime.before(Date(currentTimeMillis() - STALE_DATA_INTERVAL))) {
                _mediaRecords.clear()
                runBlocking {
                    _mediaRecords.addAll(requestAirtableMedia())
                }
            }
            return _mediaRecords
        }

    fun mediaRecordsFilter(
        type: String? = null,
        limit: Int? = null,
        order: MediaRecord.Order = MediaRecord.Order.UPDATED
    ) =
        mediaRecords.filter { type?.let { type -> it.fields.type == type } ?: true }
            .sortedWith(order.comparator)
            .let { limit?.let { limit -> it.take(limit) } ?: it }

    private suspend fun requestAirtableMedia(): List<MediaRecord> {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val response: AirtableMediaAccessObject = client.request {
            url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/media-record")
            header("Authorization", "Bearer $token")
        }
        client.close()
        return response.records
    }
}