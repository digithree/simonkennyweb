package co.simonkenny.web.airtable

import co.simonkenny.web.airtable.about.AboutRecord
import co.simonkenny.web.airtable.about.AirtableAboutAccessObject
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

interface IOrder<T> {
    val key: String
    val comparator: Comparator<T>
    val field: String
    val direction: String
}

class RequestWrapper<T>(
    private val token: String,
    private val table: String,
    private val request: suspend (httpClient: HttpClient, block: HttpRequestBuilder.() -> Unit) -> List<T>
) {

    fun fetch(
        type: String? = null,
        limit: Int? = null,
        order: IOrder<T>? = null
    ): List<T> {
        val list: List<T>
        runBlocking {
            val client = HttpClient(CIO) {
                install(JsonFeature)
            }
            list = request(client) {
                url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/$table")
                type?.run { parameter("filterByFormula", "{type} = '$this'") }
                limit?.takeIf { (1..100).contains(it) }?.run {parameter("maxRecords", "$this") }
                order?.run {
                    parameter("sort[0][field]", field)
                    parameter("sort[0][direction]", direction)
                }
                header("Authorization", "Bearer $token")
            }
            /*
            val response: U = client.request {
                url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/$table")
                if (type != null) parameter("filterByFormula", "{type} = '$type'")
                header("Authorization", "Bearer $token")
            }
             */
            client.close()
        }
        return list
        /*
        return about
            //.filter { type.let { type -> it.fields.type == type } }
            .sortedWith(order.comparator)
            .let { limit?.let { limit -> it.take(limit) } ?: it }
         */
    }
}