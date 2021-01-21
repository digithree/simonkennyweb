package co.simonkenny.web

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.routing.*
import kotlinx.html.*

data class SuggestedCommand(val uri: String, val text: String)

data class CommandMetadata(val name: String, val params: List<String>)

private val SUGGESTED_COMMANDS = listOf(
    SuggestedCommand("/?cmd=help", "help"),
    SuggestedCommand("/?cmd=about+work", "about work"),
    SuggestedCommand("/?cmd=config+-d", "config --dark")
)


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    /*
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
     */

    routing {
        get("/") {
            //call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
            val commands = parseCommands(call.request.queryParameters)
            call.respondHtml {
                commonHead(this, ConfigCmd.extract(commands)?.dark ?: false)
                body("terminal") {
                    br { }
                    div("container") {
                        h2 { +"Simon Kenny - personal website" }
                        if (commands.isEmpty()) {
                            with(AboutCmd.default()) {
                                p {
                                    +"No command entered, showing default content i.e. "
                                    code { +toUriCmdParam() }
                                }
                                render(this@div)
                            }
                        }
                    }
                    commands.forEach { div("container") { it.render(this) } }
                    promptFooter(this, SUGGESTED_COMMANDS)
                }
            }
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/") {
            resources("static")
        }

        /*
        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }
         */
    }
}

data class MySession(val count: Int = 0)

