package co.simonkenny.web.airtable

import airtable.compare
import airtable.compareAirtableDates
import kotlin.Comparator

data class AirtableAboutAccessObject(
    val records: List<AboutRecord>
)

data class AboutRecord(
    val id: String,
    val fields: AboutFields,
    val createdTime: String
) {
    enum class Order(
        override val key: String,
        override val comparator: Comparator<AboutRecord>,
        override val field: String,
        override val direction: String
    ): IOrder<AboutRecord> {
        ALPHABETICAL(
            "alphabetical",
            compare { o1, o2 -> o1.fields.name.compareTo(o2.fields.name) },
            "name",
            "asc"
        ),
        START_DATE_DESC(
            "newest",
            compare { o1, o2 -> compareAirtableDates(o1.fields.startDate, o2.fields.startDate) },
            "startDate",
            "desc"
        ),
        START_DATE_ASC(
            "oldest",
            compare { o1, o2 -> compareAirtableDates(o1.fields.startDate, o2.fields.startDate, newestFirst = false) },
            "startDate",
            "asc"
        )
    }

    companion object {
        val ORDERS = listOf(Order.ALPHABETICAL, Order.START_DATE_DESC, Order.START_DATE_ASC)
    }
}

data class AboutFields(
    val name: String,
    val type: String,
    val title: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val url: String?,
    val image: String?,
    val status: String?
)