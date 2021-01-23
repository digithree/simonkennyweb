package co.simonkenny.web

import co.simonkenny.web.airtable.data.AirtableRequester
import co.simonkenny.web.command.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.sessions.*
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

data class SessionConfig(val configCommand: String)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * For AirTable access run the compiled jar with the following option:
 *      -P:ktor.security.airtableApiKey="APIKEY"
 */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Sessions) {
        cookie<SessionConfig>(SessionConfig::javaClass.name) {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    routing {
        static("/") {
            resources("static")
        }

        get("/") {
            if (testing) {
                call.respondHtml {
                    commonHead(this)
                    body("terminal") {
                        br { }
                        div("container") {
                            h1 { +"Simon Kenny - personal website" }
                        }
                        p { +"Testing activated, just render simple HTML"}
                    }
                }
                return@get
            }

            configGlobals(this@module) // TODO : find better way to set env vars

            val commands = parseCommands(call.request.queryParameters)

            var configCommand = ConfigCommand.extract(commands)
                ?: call.sessions.get<SessionConfig>()?.let { parseCommand(it.configCommand) as ConfigCommand }

            if (configCommand?.clear == true) {
                configCommand = null
            }

            configCommand?.let { call.sessions.set(SessionConfig(it.toUriCmdParam())) }
                ?: call.sessions.clear(SessionConfig::javaClass.name)

            call.respondHtml {
                commonHead(this, configCommand?.dark ?: false)
                body("terminal") {
                    br { }
                    div("container") {
                        h1 { +"Simon Kenny - personal website" }
                        if (commands.isEmpty()) {
                            with(AboutCommand.default()) {
                                p {
                                    +"No command entered, showing default content i.e. "
                                    code { +toUriCmdParam() }
                                }
                            }
                        }
                    }
                    commands.takeIf { it.isNotEmpty() }
                        ?.forEach { runBlocking { it.render(this@body) } }
                        ?: with(AboutCommand.default()) { runBlocking { render(this@body) } }
                    promptFooter(this, SUGGESTED_COMMANDS)
                    div("container") { br { } }
                    configCommand?.run {
                        div("container") {
                            p {
                                +"Cookie to keep your custom config is stored, if browser allowed. Use "
                                code { +"config --clear" }
                                +" to remove it."
                                br { }
                                +"No personal data is stored with cookie."
                            }
                            br { }
                        }
                    }
                }
            }
        }
    }
}

