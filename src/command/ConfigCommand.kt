package co.simonkenny.web.command

import co.simonkenny.web.DIV_CLASS
import co.simonkenny.web.FriendCodeLock
import kotlinx.html.*

const val CMD_CONFIG = "config"

private val FLAG_FRIEND = FlagInfo("f", "friend")
private val FLAG_DARK = FlagInfo("d", "dark")
private val FLAG_LIGHT = FlagInfo("l", "light")
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

    val friendUnlocked = FriendCodeLock.getInstance().unlock(getFlagOption(FLAG_FRIEND) ?: "")

    companion object {
        private val registeredFlags = listOf(FLAG_HELP, FLAG_FRIEND, FLAG_DARK, FLAG_LIGHT, FLAG_THEME, FLAG_CLEAR)

        private fun flagReplacer(flagData: FlagData) =
            when (flagData.flagInfo) {
                FLAG_DARK -> FlagData(FLAG_THEME, listOf(FLAG_THEME_OPT_DARK))
                FLAG_LIGHT -> FlagData(FLAG_THEME, listOf(FLAG_THEME_OPT_LIGHT))
                else -> flagData
            }

        fun parse(params: List<String>): ConfigCommand =
            ConfigCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags, ::flagReplacer))

        fun extract(commands: List<Command>): ConfigCommand? =
            commands.find { it.name == CMD_CONFIG }?.let { it as ConfigCommand }
    }

    override fun distinctKey(): String = CMD_CONFIG

    override fun helpRender(block: HtmlBlockTag, config: ConfigCommand?) {
        block.div(DIV_CLASS) {
            if (findFlag(FLAG_FRIEND) != null && !friendUnlocked) {
                p { +"Friend code is incorrect" }
            }
            pre(classes = "scroll") {
                +"""usage: config [options]

    Note that config is stored as cookie if browser allows. Only stores last config.

Options:
-h,--help             shows this help, ignores other commands and flags
-t=<?>,--theme=<?>    set display theme, one of: light (default), dark
-d,--dark             alias for --theme=dark
-l,--light            alias for --theme=light
-c,--clear            clear config (removes cookie if present). ignores other options
-f=<?>,--friend=<?>   submit friend code, grants access to more of website,
                          get in touch with site owner to get friend code.
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag, config: ConfigCommand?) {
        if (checkHelp(block, config)) return
        block.div(DIV_CLASS) {
            if (clear) p { +"Config is cleared." }
            if (findFlag(FLAG_THEME) != null) {
                p {
                    when (getFlagOption(FLAG_THEME)) {
                        FLAG_THEME_OPT_DARK -> +"Theme set to dark. 🌙"
                        FLAG_THEME_OPT_LIGHT -> +"Theme set to light. ☀️"
                        else -> +"Unknown theme, default is light."
                    }
                }
            }
            if (findFlag(FLAG_FRIEND) != null) {
                p {
                    when {
                        friendUnlocked -> +"Friend code is set."
                        else -> +"Friend code is incorrect."
                    }
                }
            }
        }
    }
}