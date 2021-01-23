package co.simonkenny.web

import co.simonkenny.web.command.ConfigCommand
import co.simonkenny.web.command.parseCommand
import co.simonkenny.web.command.parseCommands
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun testConfigCommandParseAndUnparse() {
        parseCommands(listOf("config --dark"))
            //ConfigCommand.parse(listOf("--dark"))
            .also { assertEquals(it.size, 1) }
            .let { it[0] }
            .also { assert(it is ConfigCommand) }
            .let { it as ConfigCommand }
            .also { assertEquals(it.dark, true) }
            .toUriCmdParam()
            .also { assertEquals(it, "config --theme=dark") }
            .let { parseCommand(it) }
            .also { assertNotNull(it) }
            .also { assert(it is ConfigCommand) }
            .also { assertEquals((it!! as ConfigCommand).dark, true) }
    }
}
