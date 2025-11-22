package com.edleshed.giftandgo

import com.edleshed.giftandgo.ipcheck.RequestLog
import com.edleshed.giftandgo.ipcheck.RequestLogRepository
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.lang.Thread.sleep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "ipcheck.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:ipcheck;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class IpGateHttpTest(
    @param:Autowired private val rest: TestRestTemplate,
    @param:Autowired private val repo: RequestLogRepository,
    @param:LocalServerPort private val port: Int,
) {
    companion object {
        private lateinit var wiremock: WireMockServer

        @BeforeAll
        @JvmStatic
        fun startWireMock() {
            wiremock = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremock.start()
        }

        @AfterAll
        @JvmStatic
        fun stopWireMock() {
            wiremock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("ipcheck.base-url") { "http://localhost:${wiremock.port()}/json" }
        }
    }

    @BeforeEach
    fun reset() {
        wiremock.resetAll()
        repo.deleteAll()
    }

    private val xForwardedFor = "X-Forwarded-For"

    @Test
    fun `allows non-blocked and returns 200`() {
        val ip = "1.1.1.1"
        wiremock.stubFor(
            get(urlEqualTo("/json/$ip"))
                .willReturn(
                    okJson(
                        """{
                "status":"success",
                "query":"$ip",
                "country":"United Kingdom",
                "countryCode":"GB",
                "isp":"Cloudflare"
            }""",
                    ),
                ),
        )

        val headers =
            HttpHeaders().apply {
                contentType = TEXT_PLAIN
                add(xForwardedFor, ip)
            }
        val response =
            rest.postForEntity(
                "http://localhost:$port/files/process",
                HttpEntity(
                    "18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1",
                    headers,
                ),
                ByteArray::class.java,
            )

        assertEquals(200, response.statusCode.value())

        val row = waitForLog { it.requestUri == "/files/process" && it.ipAddress == ip }

        assertEquals(row.responseCode, 200)
        assertEquals("GB", row.countryCode)
        assertTrue((row.ipProvider ?: "").contains("Cloudflare"))
    }

    @Test
    fun `blocks by country and logs 403`() {
        val ip = "8.8.8.8"
        wiremock.stubFor(
            get(urlEqualTo("/json/$ip"))
                .willReturn(
                    okJson(
                        """{
                "status":"success",
                "query":"$ip",
                "country":"United States",
                "countryCode":"US",
                "isp":"Google LLC"
            }""",
                    ),
                ),
        )

        val headers =
            HttpHeaders().apply {
                contentType = TEXT_PLAIN
                add(xForwardedFor, ip)
            }
        val response =
            rest.postForEntity(
                "http://localhost:$port/files/process?hello=1",
                HttpEntity("whatever", headers),
                String::class.java,
            )

        assertEquals(403, response.statusCode.value())
        assertTrue(response.body!!.contains("Blocked country: US"))

        val row = waitForLog { it.requestUri == "/files/process" && it.responseCode == 403 }
        assertEquals("/files/process", row.requestUri)
        assertEquals(403, row.responseCode)
        assertEquals("US", row.countryCode)
        assertTrue((row.ipProvider ?: "").contains("Google"))
        assertTrue(row.timeLapsedMs >= 0)
    }

    @Test
    fun `blocks by isp even if country allowed`() {
        val ip = "3.3.3.3"
        wiremock.stubFor(
            get(urlEqualTo("/json/$ip"))
                .willReturn(
                    okJson(
                        """{
                "status":"success",
                "query":"$ip",
                "country":"Germany",
                "countryCode":"DE",
                "isp":"Amazon.com, Inc.",
                "org":"AWS EC2"
            }""",
                    ),
                ),
        )

        val headers =
            HttpHeaders().apply {
                contentType = TEXT_PLAIN
                add(xForwardedFor, ip)
            }
        val response =
            rest.postForEntity(
                "http://localhost:$port/files/process",
                HttpEntity("whatever", headers),
                String::class.java,
            )

        assertEquals(403, response.statusCode.value())
        assertTrue(response.body!!.contains("Blocked ISP"))
    }

    fun waitForLog(whichRequestLog: (RequestLog) -> Boolean): RequestLog {
        val deadline = System.currentTimeMillis() + 2000
        while (System.currentTimeMillis() < deadline) {
            val match = repo.findAll().firstOrNull(whichRequestLog)
            if (match != null) return match
            sleep(50)
        }
        throw AssertionError("Timed out waiting for matching RequestLog")
    }
}
