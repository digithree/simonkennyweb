package co.simonkenny.web.command

import co.simonkenny.web.DIV_CLASS
import kotlinx.html.*

class ErrorCommand(private val errorInput: String): HelpCommand(emptyList()) {

    companion object {
        fun create(errorInput: String) = ErrorCommand(errorInput)
    }

    override suspend fun render(block: HtmlBlockTag, config: ConfigCommand?) {
        block.div(DIV_CLASS) {
            hr { }
            p {
                +"Error in command: "
                code { +errorInput }
                h2 { +"Showing help due to command error" }
            }
            hr { }
        }
        super.render(block, config)
    }
}