package com.edleshed.giftandgo.files

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class FileProcessorServiceTest {

    private val validator = org.springframework.validation.beanvalidation.LocalValidatorFactoryBean()
        .apply { afterPropertiesSet() }
    private val personParser = DefaultPersonParser()
    private val fileProcessorService: FileProcessorService = ValidatingFileProcessorService(
        personParser,
        validator
    )

    @Test
    fun shouldProcessCorrectFile() {
        val people = fileProcessorService.process(getText("/EntryFile.txt"))

        assertEquals(3, people.size)
        assertEquals(
            Person(
                UUID.fromString("3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7"),
                "2X2D24",
                "Mike Smith",
                "Likes Grape",
                Transport(
                    "Drives an SUV",
                    35.0,
                    95.5
                )
            ),
            people[1]
        )
    }

    @Test
    fun shouldShowErrorForBrokenFile() {
        val exception = assertFailsWith<ParseAggregateException> {
            fileProcessorService.process(getText("/BrokenFile.txt"))
        }
        val expected = setOf(
            LineError(1, "expected 7 fields, got 6"),
            LineError(3, "averageSpeed must be a number"),
            LineError(4, "invalid UUID")
        )

        assertEquals(expected, exception.errors.toSet())
    }

    @Test
    fun shouldShowErrorsForInvalidFile() {
        val exception = assertFailsWith<ValidationAggregateException> {
            fileProcessorService.process(getText("/InvalidFile.txt"))
        }
        val expected = setOf(
            ValidationError(1, "likes", "likes must start with 'Likes '"),
            ValidationError(2, "transport.averageSpeed", "must be greater than or equal to 0"),
            ValidationError(3, "id", "must not be blank"),
            ValidationError(3, "name", "must not be blank")
        )

        assertEquals(expected, exception.errors.toSet())
    }

    @Test
    fun relaxedFileProcessorShouldNotValidate() {
        val people = RelaxedFileProcessorService(personParser).process(getText("/InvalidFile.txt"))

        assertEquals(3, people.size)
        assertEquals(
            Person(
                UUID.fromString("3ce2d17b-e66a-4c1e-bca3-40eb1c9222c7"),
                "2X2D24",
                "Mike Smith",
                "Likes Grape",
                Transport(
                    "Drives an SUV",
                    -35.0,
                    95.5
                )
            ),
            people[1]
        )
    }

    private fun getText(fileName: String): String = this::class.java.getResource(fileName)!!
        .readText(Charsets.UTF_8)
}