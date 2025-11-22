package com.edleshed.giftandgo.ipcheck

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RequestLogService(
    private val repo: RequestLogRepository
) {

    fun log(
        requestUri: String,
        responseCode: Int,
        ipAddress: String,
        countryCode: String?,
        ipProvider: String?,
        timeLapsedMs: Long
    ) {
        repo.save(
            RequestLog(
                requestUri = requestUri,
                requestTimestamp = Instant.now(),
                responseCode = responseCode,
                ipAddress = ipAddress,
                countryCode = countryCode,
                ipProvider = ipProvider,
                timeLapsedMs = timeLapsedMs
            )
        )
    }
}
