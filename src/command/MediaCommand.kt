package co.simonkenny.web.command

import airtable.AirtableRequester
import airtable.airtableDate
import co.simonkenny.web.DIV_CLASS
import co.simonkenny.web.airtable.media.MediaRecord
import kotlinx.html.*
import java.lang.IllegalStateException
import java.util.*
import kotlin.jvm.Throws

const val CMD_MEDIA = "media"

private val FLAG_TOPIC = FlagInfo("t", "topic", default = true, optional = false)
private val FLAG_LIMIT = FlagInfo("l", "limit")
private val FLAG_ORDER = FlagInfo("o", "order")
private val FLAG_DETAILS = FlagInfo("d", "details")

private val SERVICE_BASE_URLS = mapOf(
    "play" to "https://www.igdb.com/games/%s"
)

private const val TOPIC_ALL = "all"


private fun createServiceUrl(type: String, serviceId: String): String? =
    SERVICE_BASE_URLS[type]?.let { String.format(Locale.US, it, serviceId) }



class MediaCommand(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_MEDIA

    override val _registeredFlags = registeredFlags

    override val friendsOnly = true

    companion object {
        private val registeredFlags = listOf(FLAG_HELP, FLAG_TOPIC, FLAG_LIMIT, FLAG_ORDER, FLAG_DETAILS)

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
-l=<limit>,--limit=<limit>    positive integer, show number of items up to limit
-o=<order>,--order=<order>    order to show items in, default is updated,
                                  one of: updated, title, status, rating
-d,--details                  show more details for each media item
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag, friendCodeActive: Boolean) {
        if (checkHelp(block, friendCodeActive)) return
        val mediaRecord = AirtableRequester.getInstance().media.mediaRecordsFilter(
            type = getFlagOption(FLAG_TOPIC, 0)?.takeIf { it != TOPIC_ALL },
            limit = getFlagOption(FLAG_LIMIT,0)?.toIntOrNull()?.takeIf { it > 0 },
            order = getFlagOption(FLAG_ORDER,0)?.let { orderKey ->
                MediaRecord.ORDERS.find { it.key == orderKey }
            } ?: MediaRecord.Order.UPDATED
        )
        block.div(DIV_CLASS) {
            mediaRecord.map { it.fields }.takeIf { it.isNotEmpty() }?.forEach {
                hr { }
                h1 { +it.title }
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
                                else -> "Terrible" //1
                            }.let { str -> b { +str } }
                        }
                    }
                if (findFlag(FLAG_DETAILS) != null) {
                    it.description?.takeIf { str -> str.isNotBlank() }?.run { p {+this@run } }
                    it.comments?.takeIf { str -> str.isNotBlank() }?.run {
                        h2 { +"Comments" }
                        p { +this@run }
                    }
                }
                createServiceUrl(it.type, it.serviceId)?.run {
                    p {
                        br { }
                        a(href = this@run, target = "_blank") {
                            it.image?.let { image -> img(src = image, alt = it.title, classes = "thumbnail") }
                                ?: +"Details on 3rd party service"
                        }
                    }
                }?: it.image?.let { image -> img(src = image, alt = it.title, classes = "thumbnail") }
            } ?: run {
                em { +"No items available matching options." }
            }
        }
    }
}