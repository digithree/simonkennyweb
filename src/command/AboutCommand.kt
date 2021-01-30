package co.simonkenny.web.command

import airtable.AirtableRequester
import airtable.airtableDate
import co.simonkenny.web.DIV_CLASS
import co.simonkenny.web.airtable.AboutRecord
import co.simonkenny.web.airtable.FieldMatcher
import kotlinx.html.*
import java.lang.IllegalStateException
import java.util.*
import kotlin.jvm.Throws

const val CMD_ABOUT = "about"

private val FLAG_TOPIC = FlagInfo("t", "topic", default = true, optional = false)
private val FLAG_LIMIT = FlagInfo("l", "limit")
private val FLAG_ORDER = FlagInfo("o", "order")
private val FLAG_SHOW_DATES = FlagInfo("sd", "show-dates")

private const val DEFAULT_TOPIC_VALUE = "project"
private const val DEFAULT_ORDER_VALUE = "newest"

private const val TOPIC_ALL = "all"


class AboutCommand private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_ABOUT

    override val _registeredFlags = registeredFlags

    companion object {
        fun default() = AboutCommand(listOf(FlagData(FLAG_TOPIC, listOf(DEFAULT_TOPIC_VALUE))))

        private val registeredFlags = listOf(FLAG_HELP, FLAG_TOPIC, FLAG_LIMIT, FLAG_ORDER, FLAG_SHOW_DATES)

        @Throws(IllegalStateException::class)
        fun parse(params: List<String>): AboutCommand =
            AboutCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
    }

    override fun distinctKey(): String = "$name ${findFlag(FLAG_TOPIC)?.toReadableString()}"

    override fun helpRender(block: HtmlBlockTag, config: ConfigCommand?) {
        block.div(DIV_CLASS) {
            pre(classes = "scroll") {
                +"""usage: about <topic> [options]
                    
Options:
-h,--help               shows this help, ignores other commands and flags
-t=<?>,--type=<?>       topic to show item about,
                            one of: project, education, work, personal, all
-l=<?>,--limit=<?>      positive integer, show number of items up to limit
-o=<?>,--order=<?>      order to show items in, one of: newest, oldest, alphabetical
-sd,--show-dates        shows dates in item display (off by default)
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag, config: ConfigCommand?) {
        if (checkHelp(block, config)) return
        val aboutRecords = AirtableRequester.getInstance().about.fetch(
            fieldMatchers = listOfNotNull(
                getFlagOption(FLAG_TOPIC)?.takeIf { it != TOPIC_ALL }?.let { FieldMatcher("type", it) }
            ),
            limit = getFlagOption(FLAG_LIMIT)?.toIntOrNull()?.takeIf { it > 0 },
            order = getFlagOption(FLAG_ORDER)?.let { orderKey ->
                AboutRecord.ORDERS.find { it.key == orderKey }
            } ?: AboutRecord.Order.START_DATE_DESC
        )
        block.div(DIV_CLASS) {
            aboutRecords.map { it.fields }.takeIf { it.isNotEmpty() }?.forEach {
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
            } ?: run {
                em { +"No items available matching options." }
            }
        }
    }
}