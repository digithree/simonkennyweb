package co.simonkenny.web.airtable.data

import kotlin.Comparator

data class AirtableAboutAccessObject(
    val records: List<AboutRecord>
)

data class AboutRecord(
    val id: String,
    val fields: AboutFields,
    val createdTime: String
) {
    enum class Order(val key: String, val comparator: Comparator<AboutRecord>) {
        ALPHABETICAL("alphabetical", compare { o1, o2 -> o1.fields.name.compareTo(o2.fields.name) }),
        START_DATE_DESC("newest", compare { o1, o2 ->
            o1.fields.startDate?.let { o2.fields.startDate?.airtableDate()?.compareTo(it.airtableDate()) } ?: 0}),
        START_DATE_ASC("oldest", compare { o1, o2 ->
            o2.fields.startDate?.let { o1.fields.startDate?.airtableDate()?.compareTo(it.airtableDate()) } ?: 0}),
    }

    companion object {
        private fun compare(c: (o1: AboutRecord, o2: AboutRecord) -> Int): Comparator<AboutRecord> =
            Comparator { o1, o2 -> if (o1 == null || o2 == null) 0 else c(o1, o2) }

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