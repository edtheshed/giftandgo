package com.edleshed.giftandgo.ipcheck

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ErrorResponse(
    val title: String,
    val status: Int,
    val detail: String,
    val ip: String,
)

@Entity
@Table(name = "request_log")
open class RequestLog(
    @Id
    val id: UUID = UUID.randomUUID(),
    val requestUri: String,
    val requestTimestamp: Instant,
    val responseCode: Int,
    val ipAddress: String,
    val countryCode: String?,
    val ipProvider: String?,
    val timeLapsedMs: Long,
) {
    protected constructor() : this(
        id = UUID.randomUUID(),
        requestUri = "",
        requestTimestamp = Instant.EPOCH,
        responseCode = 0,
        ipAddress = "",
        countryCode = null,
        ipProvider = null,
        timeLapsedMs = 0L,
    )
}

interface RequestLogRepository : JpaRepository<RequestLog, UUID>

@Component
@ConditionalOnProperty(prefix = "ipcheck", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class IpGateAndAuditFilter(
    private val ipCheckService: IpCheckService,
    private val logService: RequestLogService,
    private val mapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val started = System.nanoTime()
        val ip = resolveClientIp(request)
        val uri = request.requestURI
        var cc: String?
        var isp: String?

        val decision = ipCheckService.check(ip)
        cc = decision.countryCode
        isp = decision.isp

        if (decision.blocked) {
            logRequest(uri, FORBIDDEN.value(), ip, cc, isp, started)
            respondForbidden(response, decision.reason ?: FORBIDDEN.name, ip)
            return
        }

        try {
            filterChain.doFilter(request, response)
            logRequest(uri, response.status, ip, cc, isp, started)
        } catch (ex: Exception) {
            logRequest(uri, INTERNAL_SERVER_ERROR.value(), ip, cc, isp, started)
            throw ex
        }
    }

    private fun resolveClientIp(req: HttpServletRequest): String {
        val xff =
            req
                .getHeader("X-Forwarded-For")
                ?.split(',')
                ?.firstOrNull()
                ?.trim()
        if (!xff.isNullOrBlank()) return xff

        val realIp = req.getHeader("X-Real-IP")?.trim()
        if (!realIp.isNullOrBlank()) return realIp

        return req.remoteAddr
    }

    private fun respondForbidden(
        resp: HttpServletResponse,
        reason: String,
        ip: String,
    ) {
        resp.status = FORBIDDEN.value()
        resp.contentType = APPLICATION_JSON_VALUE

        val error =
            ErrorResponse(
                title = FORBIDDEN.name,
                status = FORBIDDEN.value(),
                detail = reason,
                ip = ip,
            )

        resp.writer.write(mapper.writeValueAsString(error))
    }

    private fun logRequest(
        uri: String,
        status: Int,
        ip: String,
        cc: String?,
        isp: String?,
        started: Long,
    ) {
        val elapsedMs =
            TimeUnit
                .NANOSECONDS
                .toMillis(System.nanoTime() - started)

        logService.log(
            requestUri = uri,
            responseCode = status,
            ipAddress = ip,
            countryCode = cc,
            ipProvider = isp,
            timeLapsedMs = elapsedMs,
        )
    }
}
