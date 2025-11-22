package com.edleshed.giftandgo.ipcheck

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@ConfigurationProperties("ipcheck")
data class IpCheckProperties(
    val baseUrl: String,
    val blockedCountries: Set<String>,
    val blockedIsps: Set<String>,
    val timeoutMs: Long,
    val enabled: Boolean,
)

data class IpApiResponse(
    val status: String,
    val message: String? = null,
    val query: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val isp: String? = null,
)

data class Decision(
    val blocked: Boolean,
    val reason: String? = null,
    val countryCode: String? = null,
    val isp: String? = null,
)

interface IpCheckService {
    fun check(ip: String): Decision
}

@Service
class DefaultIpCheckService(
    private val props: IpCheckProperties,
    private val client: RestClient,
) : IpCheckService {
    override fun check(ip: String): Decision {
        val response: IpApiResponse? =
            try {
                client
                    .get()
                    .uri("/{ip}", ip)
                    .retrieve()
                    .body(IpApiResponse::class.java)
            } catch (_: Exception) {
                return Decision(true, "IP check failed")
            }

        when {
            response == null -> {
                return Decision(true, "IP check failed")
            }

            response.status != "success" -> {
                return Decision(true, "IP check: ${response.message ?: "fail"}")
            }

            else -> {
                val cc = response.countryCode?.uppercase()
                if (cc != null && props.blockedCountries.contains(cc)) {
                    return Decision(true, "Blocked country: $cc", cc, response.isp)
                }

                val ispText = listOfNotNull(response.isp).joinToString(" ").lowercase()
                if (props.blockedIsps.any { it in ispText }) {
                    val ispName = response.isp ?: "unknown"
                    return Decision(true, "Blocked ISP: $ispName", cc, ispName)
                }

                return Decision(false, countryCode = cc, isp = response.isp)
            }
        }
    }
}
