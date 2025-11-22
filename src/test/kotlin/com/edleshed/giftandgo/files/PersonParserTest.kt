package com.edleshed.giftandgo.files

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class PersonParserTest {
    val parser = DefaultPersonParser()

    @Test
    fun `should parse line`() {
        val line = "18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1"
        val person = parser.parseOrThrow(line)

        assertEquals(
            Person(
                UUID.fromString("18148426-89e1-11ee-b9d1-0242ac120002"),
                "1X1D14",
                "John Smith",
                "Likes Apricots",
                Transport(
                    "Rides A Bike",
                    6.2,
                    12.1,
                ),
            ),
            person,
        )
    }

    @Nested
    inner class Exceptions {
        @Test
        fun `throw with invalid line`() {
            val line = "18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|Likes Apricots|Rides A Bike|6.2|12.1"
            val exception =
                assertFailsWith<ParseException> {
                    parser.parseOrThrow(line)
                }
            assertEquals("expected 7 fields, got 6", exception.message)
        }

        @Test
        fun `throw with invalid top speed`() {
            val line = "18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|NAN"
            val exception =
                assertFailsWith<ParseException> {
                    parser.parseOrThrow(line)
                }
            assertEquals("topSpeed must be a number", exception.message)
        }

        @Test
        fun `throw with invalid average speed`() {
            val line = "18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|NAN|12.1"
            val exception =
                assertFailsWith<ParseException> {
                    parser.parseOrThrow(line)
                }
            assertEquals("averageSpeed must be a number", exception.message)
        }
    }
}
