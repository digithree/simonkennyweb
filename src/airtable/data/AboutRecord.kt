package co.simonkenny.web.airtable.data

import java.util.*

data class AirtableAboutAccessObject(
    val records: List<AboutRecord>
)

data class AboutRecord(
    val id: String,
    val fields: AboutFields,
    val createdTime: String
)

data class AboutFields(
    val name: String,
    val type: String,
    val title: String,
    val description: String?,
    val startDate: Date?,
    val endDate: Date?,
    val url: String?,
    val image: String?,
    val status: String?
)