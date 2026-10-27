package com.jrb

import com.jrb.support.runTestApp
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `health endpoint returns 200`() = runTestApp {
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }

}
