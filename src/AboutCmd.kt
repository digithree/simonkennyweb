package co.simonkenny.web

const val CMD_ABOUT = "about"

private val FLAG_TYPE = FlagInfo("t", "type", default = true, optional = false)

private const val DEFAULT_TYPE_VALUE = "projects"


class AboutCmd private constructor(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_ABOUT

    companion object {
        fun default() = AboutCmd(listOf(FlagData(FLAG_TYPE, listOf(DEFAULT_TYPE_VALUE))))

        private val registeredFlags = listOf(FLAG_TYPE)

        fun parse(params: List<String>): AboutCmd =
            AboutCmd(params.extractFlagsRaw().createFlagDataList(registeredFlags))
                .apply { if (!isValid(registeredFlags)) error("Missing required options") }
    }

    override fun distinctKey(): String = "$name ${findFlag(FLAG_TYPE)!!.flagInfo}"

    // TODO : create own render
    //override fun render(block: HtmlBlockTag)
}