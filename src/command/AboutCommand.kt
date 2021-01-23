package co.simonkenny.web.command

import co.simonkenny.web.airtable.data.AboutRecord
import co.simonkenny.web.airtable.data.AirtableRequester
import co.simonkenny.web.airtable.data.airtableDate
import kotlinx.html.*
import java.util.*

const val CMD_ABOUT = "about"

private val FLAG_TYPE = FlagInfo("t", "type", default = true, optional = false)
private val FLAG_LIMIT = FlagInfo("l", "limit")
private val FLAG_ORDER = FlagInfo("o", "order")
private val FLAG_SHOW_DATES = FlagInfo("sd", "showdates")

private const val DEFAULT_TYPE_VALUE = "project"
private const val DEFAULT_ORDER_VALUE = "newest"

private const val TYPE_ALL = "all"


class AboutCommand private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_ABOUT

    companion object {
        fun default() = AboutCommand(listOf(FlagData(FLAG_TYPE, listOf(DEFAULT_TYPE_VALUE))))

        private val registeredFlags = listOf(FLAG_TYPE, FLAG_LIMIT, FLAG_ORDER, FLAG_SHOW_DATES)

        fun parse(params: List<String>): AboutCommand =
            AboutCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
                .apply { if (!isValid(registeredFlags)) error("Missing required options") }
    }

    override fun distinctKey(): String = "$name ${findFlag(FLAG_TYPE)!!.flagInfo}"

    override suspend fun render(block: HtmlBlockTag) {
        val aboutRecords = AirtableRequester.getInstance().aboutRecordsFilter(
            type = getFlagOption(FLAG_TYPE, 0)?.takeIf { it != TYPE_ALL },
            limit = getFlagOption(FLAG_LIMIT,0)?.toIntOrNull(),
            order = getFlagOption(FLAG_ORDER,0)?.let { orderKey ->
                AboutRecord.ORDERS.find { it.key == orderKey }
            } ?: AboutRecord.Order.START_DATE_DESC
        )
        with(block) {
            div("container") {
                aboutRecords.map { it.fields }.forEach {
                    hr { }
                    h1 { +it.title }
                    if (findFlag(FLAG_SHOW_DATES) != null && !it.startDate.isNullOrBlank()) {
                        p {
                            +it.startDate.airtableDate().readable()
                            if (!it.endDate.isNullOrBlank()) +" --> ${it.endDate.airtableDate().readable()}"
                        }
                    }
                    unsafe { +(it.description?.fromMarkdown() ?: "<p>No description</p>") }
                    "this ${it.type.toUpperCase(Locale.US)} is ${(it.status ?: "None").toUpperCase(Locale.US)}"
                        .run { it.url?.takeIf { url -> url.isNotBlank() }
                            ?.let { url -> p { a(href = url, target = "_blank") { +this@run } } }
                                ?: p { em { +this@run } }
                        }
                    it.image?.let { image -> img(src = image, alt = it.title) }
                }
            }
        }
    }
}