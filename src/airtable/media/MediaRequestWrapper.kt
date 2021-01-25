package co.simonkenny.web.airtable.media

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

class MediaRequestWrapper(var token: String) {

    fun mediaRecordsFilter(
        type: String? = null,
        limit: Int? = null,
        order: MediaRecord.Order = MediaRecord.Order.UPDATED
    ): List<MediaRecord> {
        val mediaRecords: List<MediaRecord>
        runBlocking {
            mediaRecords = requestAirtableMedia(type)
        }
        return mediaRecords
            //.filter { type?.let { type -> it.fields.type == type } ?: true }
            .sortedWith(order.comparator)
            .let { limit?.let { limit -> it.take(limit) } ?: it }
    }

    private suspend fun requestAirtableMedia(type: String? = null): List<MediaRecord> {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val response: AirtableMediaAccessObject = client.request {
            url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/media-record")
            if (type != null) parameter("filterByFormula", "{type} = '$type'")
            header("Authorization", "Bearer $token")
        }
        client.close()
        return response.records
    }
}