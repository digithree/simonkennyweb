package co.simonkenny.web

const val CMD_CONFIG = "config"

private val FLAG_DARK = FlagInfo("d", "dark")
private val FLAG_THEME = FlagInfo("t", "theme")

//private const val FLAG_THEME_OPT_LIGHT = "light"
private const val FLAG_THEME_OPT_DARK = "dark"

class ConfigCmd private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_CONFIG

    val dark = findFlag(FLAG_THEME)?.options?.contains(FLAG_THEME_OPT_DARK) ?: false

    companion object {
        private val registeredFlags = listOf(FLAG_DARK, FLAG_THEME)

        private fun flagReplacer(flagData: FlagData) =
            when (flagData.flagInfo) {
                FLAG_DARK -> FlagData(FLAG_THEME, listOf(FLAG_THEME_OPT_DARK))
                else -> flagData
            }

        fun parse(params: List<String>): ConfigCmd =
            ConfigCmd(params.extractFlagsRaw().createFlagDataList(registeredFlags, ::flagReplacer))
                .apply { if (!isValid(registeredFlags)) error("Missing required options") }

        fun extract(commands: List<Command>): ConfigCmd? =
            commands.find { it.name == CMD_CONFIG }?.let { it as ConfigCmd }
    }

    override fun distinctKey(): String = CMD_CONFIG
}