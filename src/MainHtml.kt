package co.simonkenny.web

import co.simonkenny.web.command.SuggestedCommand
import kotlinx.html.*

const val BODY_CLASS = "terminal"
const val DIV_CLASS = "container"

fun commonHead(html: HTML, dark: Boolean = false) =
    html.head {
        meta { charset = "UTF-8" }
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1.0"
        }
        meta {
            httpEquiv = "X-UA-Compatible"
            content = "ie=edge"
        }
        link {
            rel = "apple-touch-icon"
            sizes = "180x180"
            href = "/apple-touch-icon.png"
        }
        link {
            rel = "icon"
            type = "image/png"
            sizes = "32x32"
            href = "/favicon-32x32.png"
        }
        link {
            rel = "icon"
            type = "image/png"
            sizes = "16x16"
            href = "/favicon-16x16.png"
        }
        link {
            rel = "manifest"
            href = "/site.webmanifest"
        }
        title { +"SimonKenny.Co" }
        meta {
            name = "description"
            content = "I'm SimonKenny dot Co"
        }
        link {
            rel = "stylesheet"
            href = "/css/normalize.css"
        }
        link {
            rel = "stylesheet"
            href = "/css/terminal.min.css"
        }
        link {
            rel = "stylesheet"
            href = "/css/skenco.css"
        }
        if (dark) {
            link {
                rel = "stylesheet"
                href = "/css/dark.css"
            }
        }
    }

fun renderPrompt(
    block: HtmlBlockTag,
    hintText: String = "type command",
    filledInText: String? = null,
    suggestedCommands: List<SuggestedCommand>? = null
) =
    with(block) {
        div(DIV_CLASS) {
            form(action = "#") {
                div("form-group") {
                    input(name = "cmd", type = InputType.text) {
                        id = "cmd"
                        required = true
                        placeholder = hintText
                        filledInText?.run { value = filledInText }
                    }
                    +" "
                    button(classes = "btn btn-default", type = ButtonType.submit) {
                        role = "button"
                        name = "submit"
                        id = "submit"
                        +"Return"
                    }
                }
            }
        }
        div("container ahoveroff") {
            suggestedCommands?.forEach {
                a(href = it.uri) {
                    button(classes = "btn btn-primary btn-ghost") { +it.text }
                }
                +" "
            }
        }
    }