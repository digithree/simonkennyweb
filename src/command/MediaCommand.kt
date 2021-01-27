package co.simonkenny.web.command

import airtable.AirtableRequester
import airtable.airtableDate
import co.simonkenny.web.DIV_CLASS
import co.simonkenny.web.airtable.FieldMatcher
import co.simonkenny.web.airtable.MediaFields
import co.simonkenny.web.airtable.MediaRecord
import kotlinx.html.*
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

const val CMD_MEDIA = "media"

private val FLAG_TOPIC = FlagInfo("t", "topic", default = true, optional = false)
private val FLAG_LIMIT = FlagInfo("l", "limit")
private val FLAG_ORDER = FlagInfo("o", "order")
private val FLAG_STATUS = FlagInfo("s", "status")
private val FLAG_DETAILS = FlagInfo("d", "details")
private val FLAG_GROUP = FlagInfo("g", "group")

private val SERVICE_BASE_URLS = mapOf(
    "play" to "https://www.igdb.com/games/%s",
    "read" to "https://www.goodreads.com/book/show/%s",
    "watch" to "https://www.themoviedb.org/%s",
    "listen" to "https://www.listennotes.com/podcasts/%s"
)

private const val TOPIC_ALL = "all"

private const val GROUP_NONE = "none"
private const val GROUP_YEAR = "year"
private const val GROUP_MONTH = "month"

private val GROUP_YEAR_NONE = Calendar.getInstance().apply { set(1970, 1, 1) }.time
private val DATE_FORMAT_GROUP_YEAR = SimpleDateFormat("yyyy")
private val DATE_FORMAT_GROUP_MONTH = SimpleDateFormat("MMMMMMM yyyy")


private fun createServiceUrl(type: String, serviceId: String): String? =
    SERVICE_BASE_URLS[type]?.let { String.format(Locale.US, it, serviceId) }



class MediaCommand(
    flags: List<FlagData>
): Command(flags) {

    enum class RenderSize {
        LARGE, SMALL
    }

    override val name = CMD_MEDIA

    override val _registeredFlags = registeredFlags

    override val friendsOnly = true

    companion object {
        private val registeredFlags = listOf(
            FLAG_HELP, FLAG_TOPIC, FLAG_LIMIT, FLAG_ORDER, FLAG_STATUS, FLAG_DETAILS, FLAG_GROUP)

        @Throws(IllegalStateException::class)
        fun parse(params: List<String>): MediaCommand =
            MediaCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
    }

    override fun distinctKey(): String = "$name ${findFlag(FLAG_TOPIC)?.toReadableString()}"

    override fun helpRender(block: HtmlBlockTag, friendCodeActive: Boolean) {
        block.div(DIV_CLASS) {
            pre {
                +"""usage: media <topic> [options]

    This command only available to friends.
                    
Options:
-h,--help                     shows this help, ignores other commands and flags
-t=<type>,--type=<type>       topic to show item about,
                                  one of: play, watch, read, listen, all
-l=<limit>,--limit=<limit>    positive integer between 1 to 30, show number of items up to limit
-o=<order>,--order=<order>    order to show items in, default is updated,
                                  one of: updated, title, status, rating
-s=<status>,--status=<status> filter by status, matching <status>,
                                  one of: want, ready, queued, started, partial, nearly,
                                          complete, ongoing, abandoned, paused
-d,--details                  show more details for each media item
-g=<group>,--g=<group>        grouping to apply, default is none,
                                  one of: none, year, month
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag, friendCodeActive: Boolean) {
        if (checkHelp(block, friendCodeActive)) return
        val mediaRecord = AirtableRequester.getInstance().media.fetch(
            fieldMatchers = listOfNotNull(
                getFlagOption(FLAG_TOPIC)?.takeIf { it != TOPIC_ALL }?.let { FieldMatcher("type", it) },
                getFlagOption(FLAG_STATUS)?.let { FieldMatcher("lastStatus", it) }
            ),
            limit = getFlagOption(FLAG_LIMIT)?.toIntOrNull()?.takeIf { it > 0 } ?: 30,
            order = try {
                getFlagOption(FLAG_ORDER)?.let { orderKey ->
                    MediaRecord.ORDERS.find { it.key == orderKey }
                } ?: MediaRecord.Order.UPDATED
            } catch (e: Exception) {
                e.printStackTrace()
                MediaRecord.Order.UPDATED
            }
        )
        block.div(DIV_CLASS) {
            mediaRecord.map { it.fields }.takeIf { it.isNotEmpty() }?.run {
                val group = getFlagOption(FLAG_GROUP) ?: GROUP_NONE
                when (group) {
                    GROUP_YEAR, GROUP_MONTH -> groupBy {
                            it.lastUpdate?.takeIf { date -> date.isNotBlank() }?.airtableDate()?.let {
                                Calendar.getInstance().apply {
                                    time = it
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    if (group == GROUP_YEAR) set(Calendar.MONTH, 1)
                                }.time
                            } ?: GROUP_YEAR_NONE
                        }
                        .toSortedMap { o1, o2 -> o2.compareTo(o1) }
                        .forEach { (date, list) ->
                            div(classes = "terminal-card") {
                                header {
                                    date.takeIf { it != GROUP_YEAR_NONE }?.run {
                                        when(group) {
                                            GROUP_YEAR -> DATE_FORMAT_GROUP_YEAR
                                            else -> DATE_FORMAT_GROUP_MONTH
                                        }.format(date).let { +it }
                                    } ?: +"None"
                                }
                                div {
                                    list.forEach {
                                        section(classes = "group") {
                                            renderItem(this, it, RenderSize.SMALL)
                                        }
                                    }
                                }
                            }
                            br { }
                            br { }
                        }
                    else -> forEach {
                        section(classes = "group") {
                            hr { }
                            renderItem(this, it, RenderSize.LARGE)
                        }
                    }
                }
            } ?: run {
                em { +"No items available matching options." }
            }
        }
    }

    private fun renderItem(block: HtmlBlockTag, mediaFields: MediaFields, renderSize: RenderSize) {
        with(block) {
            mediaFields.let {
                h1 { +it.title }
                createServiceUrl(it.type, it.serviceId)?.run {
                    a(href = this@run, target = "_blank") {
                        it.image?.let { image -> img(
                            src = image,
                            alt = it.title,
                            classes = "leftfloat ${if (renderSize == RenderSize.SMALL) "thumbnailsmall" else "thumbnaillarge" }"
                        ) } ?: +"Details on 3rd party service"
                    }
                }?: it.image?.let { image -> img(
                    src = image,
                    alt = it.title,
                    classes = "leftfloat ${if (renderSize == RenderSize.LARGE) "thumbnaillarge" else "thumbnailsmall" }"
                ) }
                section(classes = if (renderSize == RenderSize.LARGE) "textblock" else null) {
                    p { em {
                        +"${it.type.toUpperCase(Locale.US)} - ${it.lastStatus.let { str -> if (str.isNullOrBlank()) "UNKNOWN" else str.toUpperCase(Locale.US) }}"
                        it.lastUpdate?.takeIf { str -> str.isNotBlank() }?.run { +" (${airtableDate().readable()})" }
                    } }
                    it.rating.takeIf { rating -> (1..5).contains(rating) }
                        ?.let { rating ->
                            p {
                                +"I think it's "
                                when(rating) {
                                    5 -> "Awesome"
                                    4 -> "Pretty Good"
                                    3 -> "Ok"
                                    2 -> "Poor"
                                    else -> "Terrible"
                                }.let { str -> b { +str } }
                            }
                        } ?: p { +"I don't know what I think yet" }
                    if (renderSize == RenderSize.LARGE) {
                        if (findFlag(FLAG_DETAILS) != null) {
                            it.description?.takeIf { str -> str.isNotBlank() }?.run { p {+this@run } }
                        }
                        it.comments?.takeIf { str -> str.isNotBlank() }?.run {
                            h2 { +"Comments" }
                            p { +this@run }
                        }
                    }
                }
            }
        }
    }
}