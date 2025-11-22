package com.edleshed.giftandgo.ipcheck

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.web.client.RestClient

class IpCheckServiceTest {
    private val client: RestClient = mock()
    private val getSpec: RestClient.RequestHeadersUriSpec<*> = mock()
    private val responseSpec: RestClient.ResponseSpec = mock()

    private val ipCheckProperties =
        IpCheckProperties(
            baseUrl = "https://ip-api.com",
            blockedCountries = setOf("UK", "USA"),
            blockedIsps = setOf("gcp", "aws"),
            timeoutMs = 1000,
            enabled = true,
        )

    private val ipCheckService = DefaultIpCheckService(ipCheckProperties, client)

    @Test
    fun `check valid ip and not block`() {
        val ip = "1.1.1.1"
        stubIpResponse(
            ip,
            IpApiResponse(
                status = "success",
                message = null,
                countryCode = "FR",
                isp = "Orange",
            ),
        )

        val result = ipCheckService.check(ip)

        assertEquals(
            Decision(
                blocked = false,
                reason = null,
                countryCode = "FR",
                isp = "Orange",
            ),
            result,
        )
    }

    @Test
    fun `check ip from blocked country and block`() {
        val ip = "2.2.2.2"
        stubIpResponse(
            ip,
            IpApiResponse(
                status = "success",
                message = null,
                countryCode = "UK",
                isp = "BT",
            ),
        )

        val result = ipCheckService.check(ip)

        assertEquals(
            Decision(
                blocked = true,
                reason = "Blocked country: UK",
                countryCode = "UK",
                isp = "BT",
            ),
            result,
        )
    }

    @Test
    fun `check ip from blocked isp and block`() {
        val ip = "2.2.2.2"
        stubIpResponse(
            ip,
            IpApiResponse(
                status = "success",
                message = null,
                countryCode = "FR",
                isp = "AWS",
            ),
        )

        val result = ipCheckService.check(ip)

        assertEquals(
            Decision(
                blocked = true,
                reason = "Blocked ISP: AWS",
                countryCode = "FR",
                isp = "AWS",
            ),
            result,
        )
    }

    @Test
    fun `check ip but api gives null and block`() {
        val ip = "2.2.2.2"
        stubIpResponse(
            ip,
            null,
        )

        val result = ipCheckService.check(ip)

        assertEquals(
            Decision(
                blocked = true,
                reason = "IP check failed",
            ),
            result,
        )
    }

    @Test
    fun `check ip but request throws and block`() {
        val ip = "2.2.2.2"
        given(client.get()).willThrow(RuntimeException())

        val result = ipCheckService.check(ip)

        assertEquals(
            Decision(
                blocked = true,
                reason = "IP check failed",
            ),
            result,
        )
    }

    private fun stubIpResponse(
        ip: String,
        response: IpApiResponse?,
    ) {
        given(client.get()).willReturn(getSpec)
        given(getSpec.uri("/{ip}", ip)).willReturn(getSpec)
        given(getSpec.retrieve()).willReturn(responseSpec)
        given(responseSpec.body(IpApiResponse::class.java)).willReturn(response)
    }
}
