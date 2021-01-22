package co.simonkenny.web.airtable.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.currentTimeMillis
import java.util.*

private const val STALE_DATA_INTERVAL = 86_400_400L // one day in ms

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