package co.simonkenny.web.command

import co.simonkenny.web.airtable.data.AirtableRequester
import kotlinx.html.*
import java.util.*

const val CMD_ABOUT = "about"

private val FLAG_TYPE = FlagInfo("t", "type", default = true, optional = false)

private const val DEFAULT_TYPE_VALUE = "projects"


class AboutCommand private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_ABOUT

    companion object {
        fun default() = AboutCommand(listOf(FlagData(FLAG_TYPE, listOf(DEFAULT_TYPE_VALUE))))

        private val registeredFlags = listOf(FLAG_TYPE)

        fun parse(params: List<String>): AboutCommand =
            AboutCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
                .apply { if (!isValid(registeredFlags)) error("Missing required options") }
    }

    override fun distinctKey(): String = "$name ${findFlag(FLAG_TYPE)!!.flagInfo}"

    override suspend fun render(block: HtmlBlockTag) {
        val aboutRecords = AirtableRequester.getInstance().aboutRecords
        with(block) {
            div("container") {
                aboutRecords.map { it.fields }.forEach {
                    hr { }
                    h1 { +it.title }
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