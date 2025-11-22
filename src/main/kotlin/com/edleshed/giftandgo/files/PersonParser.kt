package com.edleshed.giftandgo.files

import org.springframework.stereotype.Component
import java.util.UUID

interface PersonParser {
    fun tryParse(line: String, lineNumber: Int): ParseResult
}

@Component
class DefaultPersonParser : PersonParser {
    override fun tryParse(line: String, lineNumber: Int): ParseResult = runCatching {
        parseOrThrow(line)
    }.fold(
        onSuccess = { ParseResult.Ok(lineNumber, it) },
        onFailure = { ParseResult.Error(lineNumber, it.message ?: "Unknown parse error") }
    )

    internal fun parseOrThrow(line: String): Person {
        val parts = line.split('|')
        requireParse(parts.size == 7) { "expected 7 fields, got ${parts.size}" }

        val uuid = try {
            UUID.fromString(parts[0])
        }
        catch (_: IllegalArgumentException) { throw ParseException("invalid UUID") }

        val id = parts[1]
        val name = parts[2]
        val likes = parts[3]
        val description = parts[4]
        val averageSpeed = parts[5].toDouble("averageSpeed")
        val topSpeed = parts[6].toDouble("topSpeed")

        return Person(
                uniqueId = uuid,
                id = id,
                name = name,
                likes = likes,
                transport = Transport(
                    description = description,
                    averageSpeed = averageSpeed,
                    topSpeed = topSpeed
                )
            )
    }
}

sealed interface ParseResult {
    data class Ok(val line: Int, val person: Person) : ParseResult
    data class Error(val line: Int, val message: String) : ParseResult
}

class ParseException(message: String, cause: Throwable? = null)
    : IllegalArgumentException(message, cause)

inline fun requireParse(condition: Boolean, message: () -> String) {
    if (!condition) throw ParseException(message())
}

fun String.toDouble(field: String): Double =
    toDoubleOrNull() ?: throw ParseException("$field must be a number")
