package com.edleshed.giftandgo.files

import jakarta.validation.Validator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.collections.filterIsInstance

interface FileProcessorService {
    fun process(text: String): List<Person>
}

@Service
@ConditionalOnProperty(prefix = "features", name = ["person-validation"], havingValue = "true", matchIfMissing = true)
class ValidatingFileProcessorService(
    private val personParser: PersonParser,
    private val validator: Validator
) : FileProcessorService {
    override fun process(text: String): List<Person> {
        val results = parsePeople(text, personParser)
        handleParsingErrors(results)

        val people = results.validateOrThrow()

        return people
    }

    private fun List<ParseResult>.validateOrThrow(): List<Person> {
        val okPairs: List<Pair<Int, Person>> =
            filterIsInstance<ParseResult.Ok>().map { it.line to it.person }
        val people: List<Person> = okPairs.map { it.second }

        val validationErrors =
            okPairs.asSequence().flatMap { (line, person) ->
                validator.validate(person).map { v ->
                    ValidationError(
                        line = line,
                        field = v.propertyPath.toString(),
                        message = v.message
                    )
                }
            }.sortedWith(compareBy({ it.line }, { it.field }))
                .toList()
        if (validationErrors.isNotEmpty()) throw ValidationAggregateException(validationErrors)

        return people
    }
}

@Service
@ConditionalOnProperty(prefix = "features", name = ["person-validation"], havingValue = "false", matchIfMissing = false)
class RelaxedFileProcessorService(
    private val personParser: PersonParser
) : FileProcessorService {
    override fun process(text: String): List<Person> {
        val results = parsePeople(text, personParser)
        handleParsingErrors(results)

        return results.filterIsInstance<ParseResult.Ok>().map { it.person }
    }
}

private fun parsePeople(text: String, personParser: PersonParser): List<ParseResult> =
    text.lineSequence()
        .withIndex()
        .mapNotNull { (index, raw) ->
            val line = raw.trim()
            if (line.isEmpty()) null else personParser.tryParse(line, index + 1)
        }
        .toList()

private fun handleParsingErrors(results: List<ParseResult>) {
    val parseErrors = results.filterIsInstance<ParseResult.Error>()
        .sortedWith(compareBy({ it.line }, { it.message }))
    if (parseErrors.isNotEmpty()) {
        throw ParseAggregateException(parseErrors.map { LineError(it.line, it.message) })
    }
}

data class LineError(val line: Int, val message: String)

class ParseAggregateException(val errors: List<LineError>) : IllegalArgumentException(
    errors.joinToString("\n") { "Line ${it.line}: ${it.message}" }
)

data class ValidationError(val line: Int, val field: String, val message: String)

class ValidationAggregateException(val errors: List<ValidationError>) : IllegalArgumentException(
    errors.joinToString("\n") { "Line ${it.line}: Field ${it.field}, ${it.message}" })
