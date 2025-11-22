package com.edleshed.giftandgo

import com.edleshed.giftandgo.files.ParseAggregateException
import com.edleshed.giftandgo.files.ValidationAggregateException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ParseAggregateException::class)
    fun handleParseAggregateException(ex: ParseAggregateException): ResponseEntity<ProblemDetail> {
        val pd =
            ProblemDetail.forStatus(BAD_REQUEST).apply {
                title = "Invalid file"
                detail = "One or more lines failed to parse"
                setProperty("errors", ex.errors)
            }
        return ResponseEntity.status(BAD_REQUEST).body(pd)
    }

    @ExceptionHandler(ValidationAggregateException::class)
    fun handleValidationException(ex: ValidationAggregateException): ResponseEntity<ProblemDetail> {
        val pd =
            ProblemDetail.forStatus(BAD_REQUEST).apply {
                title = "Validation failed"
                detail = "One or more fields are invalid"
                setProperty(
                    "violations",
                    ex.errors.map { violation ->
                        mapOf(
                            "line" to violation.line,
                            "field" to violation.field,
                            "message" to violation.message,
                        )
                    },
                )
            }
        return ResponseEntity.status(BAD_REQUEST).body(pd)
    }
}
