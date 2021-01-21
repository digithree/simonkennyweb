package co.simonkenny.web

import io.ktor.http.*
import kotlinx.html.HtmlBlockTag
import kotlinx.html.code
import kotlinx.html.p


abstract class Command(
    // Flags added to this command to modify its behaviour.
    private val flags: List<FlagData> = emptyList()
) {
    // Name of this command.
    abstract val name: String

    /**
     * Create URI command param which can be used to recreate this command.
     */
    fun toUriCmdParam() = name + flags.flagsDisplay()

    /**
     * Distinct key specific to this command which is used to remove duplicates per domain.
     */
    abstract fun distinctKey(): String

    /**
     * Render this command to a HTML block directly.
     */
    open fun render(block: HtmlBlockTag) =
        with(block) {
            p {
                code { +name }
                +" run with params: ${flags.flagsDisplay()}"
            }
        }

    protected fun isValid(registeredFlags: List<FlagInfo>) =
        registeredFlags.none { flagInfo ->
            if (!flagInfo.optional) flags.find { it.flagInfo == flagInfo } == null
            else false
        }

    protected fun findFlag(flagInfo: FlagInfo): FlagData? = flags.find { it.flagInfo == flagInfo }
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
        .map {
            it.split(" ")
                .let { parts ->
                    CommandMetadata(
                        parts[0],
                        parts
                            .takeIf { list -> list.size > 1 }
                            ?.subList(1, parts.size)
                            ?: emptyList()
                    )
                }
        }.mapNotNull {
            when (it.name) {
                CMD_CONFIG -> ConfigCmd.parse(it.params)
                CMD_ABOUT -> AboutCmd.parse(it.params)
                else -> null
            }
        }
        .distinctBy { it.distinctKey() }
        .toList()


fun List<String>.extractFlagsRaw() =
    map { it.trim() }
        //.filter { it.startsWith("-") }
            // remove whitespace that is not in quotes
        .map { it.replace("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex(), "")}

fun String.extractOptions() =
    substringAfterLast("=", "")
        .takeIf { it.isNotBlank() }
            // split by comma when not in quotes
        ?.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
            ?: listOf(this)

fun List<String>.createFlagDataList(
    flagInfoList: List<FlagInfo>,
    replacer: ((flagData: FlagData) -> FlagData)? = null
): List<FlagData> =
    mapNotNull {
        flagInfoList.find { flagInfo -> flagInfo.matches(it) }
            ?.let { flagInfo -> FlagData(flagInfo, it.extractOptions()) }
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