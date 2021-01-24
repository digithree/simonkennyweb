package co.simonkenny.web.command

import co.simonkenny.web.DIV_CLASS
import kotlinx.html.*
import java.lang.IllegalStateException
import kotlin.jvm.Throws

const val CMD_HELP = "help"

private val FLAG_INFO = FlagInfo("i", "info")

class HelpCommand(
    flags: List<FlagData>
): Command(flags) {

    override val name = CMD_HELP

    override val _registeredFlags = registeredFlags

    override val canBeOptionless = true

    companion object {
        private val registeredFlags = listOf(FLAG_INFO)

        @Throws(IllegalStateException::class)
        fun parse(params: List<String>): HelpCommand =
            HelpCommand(params.extractFlagsRaw().createFlagDataList(registeredFlags))
    }

    override fun distinctKey() = name

    override fun helpRender(block: HtmlBlockTag) {
        block.div(DIV_CLASS) {
            pre {
                +"""usage: help [options]
                    
Options:
-i,--info                     shows more information about this website and why it's
                                  done in this arguably over the top style
                """.trimIndent()
            }
        }
    }

    override suspend fun render(block: HtmlBlockTag) {
        checkHelp(block) // continue after
        block.div(DIV_CLASS) {
            if (findFlag(FLAG_INFO) != null) {
                unsafe {
                    +"""
Welcome to the personal website of Simon Kenny.

For a bit of fun and to be different, it is written in Kotlin, using the [Ktor](https://ktor.io) server framework, and
styled using [Terminal CSS](https://terminalcss.xyz) with a faux command line interface as a means of navigating the
content.

Have fun and explore. Commands are submitted as query parameters so you can share you favourite view of the linked
content, if that's what you want to do.
                    """.trimIndent().fromMarkdown()
                }
            }
            p { +"Displaying help for all commands:" }
        }
        parseCommands(listOf("help", "about --help", "config --help"))
            .forEach {
                block.div(DIV_CLASS) { h2 { +it.name } }
                it.helpRender(block)
            }
    }
}