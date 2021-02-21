package co.simonkenny.web.airtable

import airtable.compare
import airtable.compareAirtableDates

data class AirtableMediaAccessObject(
    val records: List<MediaRecord>
)

data class MediaRecord(
    val id: String,
    val fields: MediaFields,
    val createdTime: String
) {
    enum class Order(
        override val key: String,
        override val comparator: Comparator<MediaRecord>,
        override val field: String,
        override val direction: String
    ): IOrder<MediaRecord> {
        TITLE(
            "title",
            compare { o1, o2 -> o1.fields.title.compareTo(o2.fields.title) },
            "title",
            "asc"
        ),
        STATUS(
            "status",
            compare { o1, o2 -> o1.fields.lastStatus?.compareTo(o2.fields.lastStatus ?: "") ?: 0 },
            "lastStatus",
            "asc"
        ),
        UPDATED(
            "updated",
            compare { o1, o2 -> compareAirtableDates(o1.fields.lastUpdate, o2.fields.lastUpdate) },
            "lastUpdate",
            "desc"
        ),
        UPDATED_REV(
            "updatedrev",
            compare { o1, o2 -> compareAirtableDates(o2.fields.lastUpdate, o1.fields.lastUpdate) },
            "lastUpdate",
            "asc"
        ),
        RATING(
            "rating",
            compare { o1, o2 -> (o2.fields.rating ?: 0).compareTo(o1.fields.rating ?: 0) },
            "rating",
            "desc"
        ),
    }

    companion object {
        val ORDERS = listOf(Order.TITLE, Order.STATUS, Order.UPDATED, Order.UPDATED_REV, Order.RATING)
    }
}

data class MediaFields(
    val internal: String,
    val type: String,
    val serviceId: String,
    val rating: Int?,
    val title: String,
    val description: String?,
    val comments: String?,
    val image: String?,
    val lastUpdate: String?,
    val lastStatus: String?,
    val public: String? // Boolean as String
)