package co.simonkenny.web

import co.simonkenny.web.airtable.data.AirtableRequester
import co.simonkenny.web.command.AboutCmd
import co.simonkenny.web.command.ConfigCmd
import co.simonkenny.web.command.SuggestedCommand
import co.simonkenny.web.command.parseCommands
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*

private val SUGGESTED_COMMANDS = listOf(
    SuggestedCommand("/?cmd=help", "help"),
    SuggestedCommand("/?cmd=about+work", "about work"),
    SuggestedCommand("/?cmd=config+-d", "config --dark")
)


fun configGlobals(application: Application) {
    application.environment.config
        .propertyOrNull("ktor.security.airtableApiKey")!!
        .run { AirtableRequester.setToken(getString()) }
}


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * For AirTable access run the compiled jar with the following option:
 *      -P:ktor.security.airtableApiKey="APIKEY"
 */
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
            configGlobals(this@module) // TODO : find better way to set env vars
            val commands = parseCommands(call.request.queryParameters)
            call.respondHtml {
                commonHead(this, ConfigCmd.extract(commands)?.dark ?: false)
                body("terminal") {
                    br { }
                    div("container") {
                        h1 { +"Simon Kenny - personal website" }
                        if (commands.isEmpty()) {
                            with(AboutCmd.default()) {
                                p {
                                    +"No command entered, showing default content i.e. "
                                    code { +toUriCmdParam() }
                                }
                            }
                        }
                    }
                    commands.takeIf { it.isNotEmpty() }
                        ?.forEach { runBlocking { it.render(this@body) } }
                        ?: with(AboutCmd.default()) { runBlocking { render(this@body) } }
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

