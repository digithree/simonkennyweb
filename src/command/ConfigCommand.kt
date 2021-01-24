package co.simonkenny.web.command

import co.simonkenny.web.DIV_CLASS
import kotlinx.html.*

const val CMD_CONFIG = "config"

private val FLAG_DARK = FlagInfo("d", "dark")
private val FLAG_THEME = FlagInfo("t", "theme")
private val FLAG_CLEAR = FlagInfo("c", "clear")

private const val FLAG_THEME_OPT_LIGHT = "light"
private const val FLAG_THEME_OPT_DARK = "dark"

class ConfigCommand private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_CONFIG

    override val _registeredFlags = registeredFlags

    val dark = findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_DARK) ?: false

    val clear = findFlag(FLAG_CLEAR) != null

    companion object {
        private val registeredFlags = listOf(FLAG_HELP, FLAG_DARK, FLAG_THEME, FLAG_CLEAR)

        private fun flagReplacer(flagData: FlagData) =
            when (flagData.flagInfo) {
                FLAG_DARK -> FlagData(FLAG_THEME, listOf(FLAG_THEME_OPT_DARK))
                else -> flagData
            }

        fun parse(params: List<String>): ConfigCommand =
            ConfigCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags, ::flagReplacer))

        fun extract(commands: List<Command>): ConfigCommand? =
            commands.find { it.name == CMD_CONFIG }?.let { it as ConfigCommand }
    }

    override fun distinctKey(): String = CMD_CONFIG

    override fun helpRender(block: HtmlBlockTag) {
        block.div(DIV_CLASS) {
            pre {
                +"""usage: config [options]

Options:
-h,--help                     shows this help, ignores other commands and flags
-t=<theme>,--theme=<theme>    set display theme, one of: light (default), dark
-d,--dark                     alias for --theme=dark
-c,--clear                    clear config (removes cookie if present). ignores other options
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag) {
        if (checkHelp(block)) return
        block.div(DIV_CLASS) {
            p {
                when {
                    clear -> +"Config is cleared."
                    findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_DARK) == true ->
                        +"Theme set to dark. 🌙"
                    findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_LIGHT) == true ->
                        +"Theme set to light. ☀️"
                    else -> +"Unknown config params, you have made an error."
                }
            }
        }
    }
}