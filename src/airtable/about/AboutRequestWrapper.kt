package co.simonkenny.web.airtable.about

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

class AboutRequestWrapper(var token: String) {

    fun aboutRecordsFilter(
        type: String? = null,
        limit: Int? = null,
        order: AboutRecord.Order = AboutRecord.Order.START_DATE_DESC
    ): List<AboutRecord> {
        val about: List<AboutRecord>
        runBlocking {
            about = requestAirtableAbout(type)

        }
        return about
            //.filter { type.let { type -> it.fields.type == type } }
            .sortedWith(order.comparator)
            .let { limit?.let { limit -> it.take(limit) } ?: it }
    }

    private suspend fun requestAirtableAbout(type: String?): List<AboutRecord> {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val response: AirtableAboutAccessObject = client.request {
            url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/about")
            if (type != null) parameter("filterByFormula", "{type} = '$type'")
            header("Authorization", "Bearer $token")
        }
        client.close()
        return response.records
    }
}