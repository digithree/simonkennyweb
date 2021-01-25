package co.simonkenny.web.airtable

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

data class FieldMatcher(val field: String, val value: String)

class RequestWrapper<T>(
    private val token: String,
    private val table: String,
    private val request: suspend (httpClient: HttpClient, block: HttpRequestBuilder.() -> Unit) -> List<T>
) {

    fun fetch(
        fieldMatchers: List<FieldMatcher>? = null,
        limit: Int? = null,
        order: IOrder<T>? = null,
    ): List<T> {
        val list: List<T>
        runBlocking {
            val client = HttpClient(CIO) {
                install(JsonFeature)
            }
            list = request(client) {
                url("https://api.airtable.com/v0/appeP7oDW7r7B82Zn/$table")
                fieldMatchers?.takeIf { it.isNotEmpty() }?.run {
                    if (size == 1) parameter("filterByFormula", "{${this[0].field}} = '${this[0].value}'")
                    else {
                        parameter("filterByFormula",
                            joinToString(",", transform = { fieldMatcher ->
                                "{${fieldMatcher.field}} = '${fieldMatcher.value}'"
                            }).let { "AND($it)" })
                    }
                }
                limit?.takeIf { (1..100).contains(it) }?.run {parameter("maxRecords", "$this") }
                order?.run {
                    parameter("sort[0][field]", field)
                    parameter("sort[0][direction]", direction)
                }
                header("Authorization", "Bearer $token")
            }
            client.close()
        }
        return list
    }
}