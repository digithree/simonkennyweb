package co.simonkenny.web.command

import co.simonkenny.web.DIV_CLASS
import io.ktor.http.*
import kotlinx.html.HtmlBlockTag
import kotlinx.html.div
import kotlinx.html.p
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.text.SimpleDateFormat
import java.util.*


private val DATE_FORMAT_READABLE = SimpleDateFormat("dd MMMMMMM yyyy")

data class SuggestedCommand(val uri: String, val text: String)

data class CommandMetadata(val name: String, val params: List<String>)

abstract class Command(
    // Flags added to this command to modify its behaviour.
    private val flags: List<FlagData>
) {
    // Name of this command.
    abstract val name: String

    protected abstract val _registeredFlags: List<FlagInfo>

    protected open val canBeOptionless = false

    /**
     * Create URI command param which can be used to recreate this command.
     */
    fun toUriCmdParam() = name + flags.flagsDisplay()

    /**
     * Distinct key specific to this command which is used to remove duplicates per domain.
     */
    abstract fun distinctKey(): String

    abstract fun helpRender(block: HtmlBlockTag, config: ConfigCommand?)

    /**
     * Render this command to a HTML block directly.
     */
    open suspend fun render(block: HtmlBlockTag, config: ConfigCommand?) = helpRender(block, config)

    protected fun checkHelp(block: HtmlBlockTag, config: ConfigCommand?): Boolean {
        if (findFlag(FLAG_HELP) != null || !isValid()) {
            if (!isValid()) {
                block.div(DIV_CLASS) {
                    p { +"Command options are invalid, showing help" }
                }
            }
            helpRender(block, config)
            return true
        }
        return false
    }

    protected fun isValid() =
        (flags.isNotEmpty() || canBeOptionless) &&
                _registeredFlags.none { flagInfo ->
                    if (!flagInfo.optional) flags.find { it.flagInfo == flagInfo } == null
                    else false
                }

    protected fun findFlag(flagInfo: FlagInfo): FlagData? =
        flags.find { it.flagInfo == flagInfo }

    protected fun getFlagOption(flagInfo: FlagInfo, optionNum: Int = 0): String? =
        findFlag(flagInfo)?.options?.get(optionNum)
}

data class FlagInfo(
    val short: String,
    val long: String,
    val default: Boolean = false,
    val optional: Boolean = true
) {
    fun matches(toMatch: String) =
        toMatch
            .substringAfterLast("-")
            .substringBefore("=")
            .let { it == short || it == long }
            .let { match ->
                if (!match && default) toMatch.let { !it.startsWith("-") }
                else match
            }
}

val FLAG_HELP = FlagInfo("h", "help")

data class FlagData(val flagInfo: FlagInfo, val options: List<String>) {
    fun toReadableString() = "--${flagInfo.long}" +
            if (options.isNotEmpty()) {
                "=" + options.joinToString(",")
            } else ""
}


fun parseCommands(parameters: Parameters): List<Command> =
    parameters.entries()
        .asSequence()
        .filter { it.key == "cmd" }
        .map { it.value }
        .flatten()
        .toList()
        .map {
            if (it.contains("&&")) {
                it.split("&&").map { part -> part.trim() }
            } else listOf(it)
        }
        .flatten()
        .let { parseCommands(it) }
        .take(3)

fun parseCommands(commands: List<String>): List<Command> =
    commands.mapNotNull { parseCommand(it) }
        .distinctBy { it.distinctKey() }

fun parseCommand(command: String): Command? =
    command.split(" ")
        .let { parts ->
            CommandMetadata(
                parts[0],
                parts
                    .takeIf { list -> list.size > 1 }
                    ?.subList(1, parts.size)
                    ?: emptyList()
            )
        }.let {
            when (it.name) {
                CMD_HELP -> HelpCommand.parse(it.params)
                CMD_CONFIG -> ConfigCommand.parse(it.params)
                CMD_ABOUT -> AboutCommand.parse(it.params)
                CMD_MEDIA -> MediaCommand.parse(it.params)
                CMD_ARTICLES -> ArticlesCommand.parse(it.params)
                else -> ErrorCommand.create("${it.name} ${it.params.joinToString(" ")}")
            }
        }

fun List<String>.extractFlagsRaw() =
    map { it.trim() }
        //.filter { it.startsWith("-") }
            // remove whitespace that is not in quotes
        .map { it.replace("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex(), "")}

fun String.extractOptions(isDefault: Boolean = false) =
    substringAfterLast("=", "")
        .takeIf { it.isNotBlank() }
            // split by comma when not in quotes
        ?.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
            ?: if (isDefault) listOf(this) else emptyList()

fun List<String>.createFlagDataList(
    flagInfoList: List<FlagInfo>,
    replacer: ((flagData: FlagData) -> FlagData)? = null
): List<FlagData> =
    mapNotNull {
        flagInfoList.find { flagInfo -> flagInfo.matches(it) }
            ?.let { flagInfo -> FlagData(flagInfo, it.extractOptions(flagInfo.default)) }
    }.let { list ->
        if (replacer != null) {
            list.map { replacer.invoke(it) }
        } else list
    }.distinct()

fun List<FlagData>.flagsDisplay(leadingSpaceIfNotEmpty: Boolean = true) =
    if (isNotEmpty()) {
        (if (leadingSpaceIfNotEmpty) " " else "") +
            joinToString(" ", transform = { flagData -> flagData.toReadableString() })
    }
    else ""

fun String.fromMarkdown(): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    return HtmlGenerator(this, parsedTree, flavour).generateHtml()
}

fun List<Command>.readable() = joinToString(separator = " && ", transform = { it.toUriCmdParam()})

fun Date.readable(): String = DATE_FORMAT_READABLE.format(this)