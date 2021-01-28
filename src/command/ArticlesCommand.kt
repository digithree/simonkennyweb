package co.simonkenny.web.command

import airtable.AirtableRequester
import airtable.LIMIT_MAX
import airtable.airtableDate
import co.simonkenny.web.DIV_CLASS
import co.simonkenny.web.airtable.*
import kotlinx.html.*
import java.lang.IllegalStateException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws


const val CMD_ARTICLES = "articles"

private val FLAG_LIMIT = FlagInfo("l", "limit")
private val FLAG_ORDER = FlagInfo("o", "order")
private val FLAG_DETAILS = FlagInfo("d", "details")
private val FLAG_COMMENTS = FlagInfo("c", "comments")
private val FLAG_GROUP = FlagInfo("g", "group")
private val FLAG_NSFW = FlagInfo("n", "nsfw")
private val FLAG_ROW = FlagInfo("r", "row")

private const val COMMENTS_ON = "on"
private const val COMMENTS_OFF = "off"

private const val ROW_ON = "on"
private const val ROW_OFF = "off"

private const val GROUP_NONE = "none"
private const val GROUP_YEAR_ADDED = "yearadd"
private const val GROUP_MONTH_ADDED = "monthadd"
private const val GROUP_YEAR_PUBLISHED = "yearpub"
private const val GROUP_MONTH_PUBLISHED = "monthpub"

private val GROUP_DATE_NONE = Calendar.getInstance().apply { set(1970, 1, 1) }.time
private val DATE_FORMAT_GROUP_YEAR = SimpleDateFormat("yyyy")
private val DATE_FORMAT_GROUP_MONTH = SimpleDateFormat("MMMMMMM yyyy")

class ArticlesCommand(
    flags: List<FlagData>
): Command(flags) {

    private enum class RenderSize {
        LARGE, SMALL
    }

    override val name = CMD_ARTICLES

    override val canBeOptionless = true

    override val _registeredFlags = registeredFlags

    companion object {
        private val registeredFlags = listOf(
            FLAG_HELP, FLAG_LIMIT, FLAG_ORDER, FLAG_DETAILS, FLAG_COMMENTS, FLAG_GROUP, FLAG_NSFW, FLAG_ROW)

        @Throws(IllegalStateException::class)
        fun parse(params: List<String>): ArticlesCommand =
            ArticlesCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
    }

    override fun distinctKey(): String = name

    override fun helpRender(block: HtmlBlockTag, friendCodeActive: Boolean) {
        block.div(DIV_CLASS) {
            pre(classes = "scroll") {
                +"""usage: articles [options]
                    
Options:
-h,--help             shows this help, ignores other commands and flags
-l=<?>,--limit=<?>    positive integer between 1 to $LIMIT_MAX, show number of
                          items up to limit
-o=<?>,--order=<?>    order to show items in, default is newest-add,
                          one of: newestadd, newestpub
-c=<?>,--comments=<?> show comments for each media item, off by default
                          unless grouping, one of: on, off
-d,--details          show more details for each media item
-g=<?>,--g=<?>        grouping to apply, default is none,
                          one of: none, yearadd, yearpub,
                                        monthadd, monthpub
-r=<?>,--row=<?>      include ROW link, on by default,
                          one of: on, off
-n,--nsfw             include NSFW content
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag, friendCodeActive: Boolean) {
        block.div(DIV_CLASS) { p { em {
            +"Note: articles here are not endorsed, they are simply of some interest."
        } } }
        if (checkHelp(block, friendCodeActive)) return
        val articlesRecord = AirtableRequester.getInstance().articles.fetch(
            limit = getFlagOption(FLAG_LIMIT)?.toIntOrNull()?.takeIf { it > 0 } ?: LIMIT_MAX,
            order = try {
                getFlagOption(FLAG_ORDER)?.let { orderKey ->
                    ArticlesRecord.ORDERS.find { it.key == orderKey }
                } ?: ArticlesRecord.Order.NEWEST_ADDED
            } catch (e: Exception) {
                e.printStackTrace()
                ArticlesRecord.Order.NEWEST_ADDED
            }
        )
        val nsfw: Boolean = findFlag(FLAG_NSFW) != null
        block.div(DIV_CLASS) {
            articlesRecord.map { it.fields }.takeIf { it.isNotEmpty() }
                    ?.filter { !(it.nsfw == "true") || nsfw }?.run {
                when (val group = getFlagOption(FLAG_GROUP) ?: GROUP_NONE) {
                    GROUP_NONE -> forEach {
                        section(classes = "group") {
                            hr { }
                            renderItem(this, it, RenderSize.LARGE)
                        }
                    }
                    else -> groupBy {
                        (if (group == GROUP_MONTH_ADDED || group == GROUP_YEAR_ADDED) it.added else it.published)
                            ?.takeIf { date -> date.isNotBlank() }?.airtableDate()?.let {
                                Calendar.getInstance().apply {
                                    time = it
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    if (group == GROUP_YEAR_ADDED || group == GROUP_YEAR_PUBLISHED) set(Calendar.MONTH, 1)
                                }.time
                            } ?: GROUP_DATE_NONE
                        }
                        .toSortedMap { o1, o2 -> o2.compareTo(o1) }
                        .forEach { (date, list) ->
                            div(classes = "terminal-card") {
                                header {
                                    date.takeIf { it != GROUP_DATE_NONE }?.run {
                                        when(group) {
                                            GROUP_YEAR_ADDED, GROUP_YEAR_PUBLISHED -> DATE_FORMAT_GROUP_YEAR
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
                }
            }
        }
    }

    private fun renderItem(block: HtmlBlockTag, articlesFields: ArticlesFields, renderSize: RenderSize) {
        with(block) {
            articlesFields.let {
                p {
                    if (getFlagOption(FLAG_ROW) == null || getFlagOption(FLAG_ROW) == ROW_ON) {
                        a(href = "https://row.onl/url?q=${URLEncoder.encode(it.url, "UTF-8")}", target = "_blank") { +"[ROW]" }
                        +" | "
                    }
                    a(href = it.url, target = "_blank") { +it.title }
                    br { }
                    it.published?.run { +airtableDate().readable() }
                }
                var longText = false
                if (findFlag(FLAG_DETAILS) != null || renderSize == RenderSize.LARGE) {
                    it.description?.run {
                        p { +"${this@run}..." }
                    }
                    p { +"Added: ${it.added.airtableDate().readable()}" }
                    longText = true
                }
                if (getFlagOption(FLAG_COMMENTS) == COMMENTS_ON) {
                    it.comment?.takeIf { str -> str.isNotBlank() }?.run {
                        h2 { +"Comments" }
                        p { +this@run }
                        longText = true
                    }
                }
                if (longText && renderSize == RenderSize.SMALL) hr { }
            }
        }
    }
}