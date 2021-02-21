package co.simonkenny.web

import airtable.AirtableRequester
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
    "help",
    "about work",
    "config --dark",
    "articles --group=monthpub",
    "media all --rating=5"
)


fun configGlobals(application: Application) {
    with(application.environment.config) {
        // mandatory
        propertyOrNull("ktor.security.airtableApiKey")!!
            .run { AirtableRequester.getInstance().token = getString() }
        // optional
        propertyOrNull("ktor.security.friendCode")
            ?.run { FriendCodeLock.getInstance().friendCode = getString() }
    }
}

data class SessionConfig(val configCommand: String)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * For AirTable access run the compiled jar with the following option:
 *      -P:ktor.security.airtableApiKey="APIKEY"
 *
 * Optionally enable friend code:
 *      -P:ktor.security.friendCode="FRIEND_CODE"
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
                    body(BODY_CLASS) {
                        br { }
                        div(DIV_CLASS) {
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
                body(BODY_CLASS) {
                    br { }
                    // page header, always the same
                    div(DIV_CLASS) {
                        p {
                            img(src = "/favicon-32x32.png", classes = "tinytiny")
                            +" Simon Kenny - personal website "
                            if (commands.isNotEmpty()) {
                                a(href = "/") { +"[home]" }
                            }
                        }
                        if (commands.isEmpty()) {
                            with(AboutCommand.default()) {
                                p {
                                    +"Default command: "
                                    code { +toUriCmdParam() }
                                }
                                p { em { +"Enter command in text box to explore website content 😁" } }
                            }
                        }
                    }
                    // header prompt
                    renderPrompt(this, commands.readable(), commands.readable())
                    // render each command
                    commands.takeIf { it.isNotEmpty() }
                        ?.forEach { runBlocking { it.render(this@body, configCommand) } }
                        ?: with(AboutCommand.default()) { runBlocking { render(this@body, configCommand) } }
                    div(DIV_CLASS) { hr { } }
                    // footer prompt
                    renderPrompt(this, commands.readable(), commands.readable(), SUGGESTED_COMMANDS)
                    // additional footer data
                    div(DIV_CLASS) { br { } }
                    configCommand?.run {
                        div(DIV_CLASS) {
                            if (friendUnlocked) h3 { +"Friend code is active." }
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

