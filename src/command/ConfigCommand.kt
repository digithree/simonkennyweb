package co.simonkenny.web.command

import kotlinx.html.HtmlBlockTag
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.p

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

    val dark = findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_DARK) ?: false

    val clear = findFlag(FLAG_CLEAR) != null

    companion object {
        private val registeredFlags = listOf(FLAG_DARK, FLAG_THEME, FLAG_CLEAR)

        private fun flagReplacer(flagData: FlagData) =
            when (flagData.flagInfo) {
                FLAG_DARK -> FlagData(FLAG_THEME, listOf(FLAG_THEME_OPT_DARK))
                else -> flagData
            }

        fun parse(params: List<String>): ConfigCommand =
            ConfigCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags, ::flagReplacer))
                .apply { if (!isValid(registeredFlags)) error("Missing required options") }

        fun extract(commands: List<Command>): ConfigCommand? =
            commands.find { it.name == CMD_CONFIG }?.let { it as ConfigCommand }
    }

    override fun distinctKey(): String = CMD_CONFIG

    override suspend fun render(block: HtmlBlockTag) {
        with(block) {
            div("container") {
                h3 { +"config" }
                p {
                    when {
                        clear -> +"Config is cleared."
                        findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_DARK) == true ->
                            +"Theme set to dark."
                        findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_LIGHT) == true ->
                            +"Theme set to light."
                        else -> +"Unknown config params, you have made an error."
                    }
                }
            }
        }
    }
}