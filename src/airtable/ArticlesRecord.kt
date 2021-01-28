package co.simonkenny.web.airtable

import airtable.compare

data class AirtableArticlesAccessObject(
    val records: List<ArticlesRecord>
)

data class ArticlesRecord(
    val id: String,
    val fields: ArticlesFields,
    val createdTime: String
) {
    enum class Order(
        override val key: String,
        override val comparator: Comparator<ArticlesRecord>,
        override val field: String,
        override val direction: String
    ): IOrder<ArticlesRecord> {
        NEWEST_ADDED(
            "newestadd",
            compare { o1, o2 -> airtable.compareAirtableDates(o1.fields.added, o2.fields.added) },
            "added",
            "desc"
        ),
        NEWEST_PUBLISHED(
            "newestpub",
            compare { o1, o2 -> airtable.compareAirtableDates(o1.fields.published, o2.fields.published) },
            "published",
            "desc"
        )
    }

    companion object {
        val ORDERS = listOf(Order.NEWEST_ADDED, Order.NEWEST_PUBLISHED)
    }
}

data class ArticlesFields(
    val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val published: String?,
    val added: String,
    val nsfw: String?,
    val comment: String?
)