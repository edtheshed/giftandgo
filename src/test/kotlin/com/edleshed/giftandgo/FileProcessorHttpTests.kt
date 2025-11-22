package com.edleshed.giftandgo

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.http.ResponseEntity
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "ipcheck.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:ipcheck;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class FileProcessorHttpTests(
    @param:Autowired private val rest: TestRestTemplate,
    @param:Autowired private val objectMapper: ObjectMapper,
    @param:LocalServerPort private val port: Int,
) {
    @Test
    fun `can process file`() {
        val response = postProcessFile("/EntryFile.txt")

        assertEquals(OK, response.statusCode)
        assertEquals(APPLICATION_JSON, response.headers.contentType)
        assertEquals(
            """attachment; filename="OutcomeFile.json"""",
            response.headers.getFirst(CONTENT_DISPOSITION),
        )

        val json = objectMapper.readTree(response.body)
        assertTrue(json.isArray)
        assertEquals(3, json.size())
    }

    @Test
    fun `can process broken file`() {
        val response = postProcessFile("/BrokenFile.txt")

        assertEquals(BAD_REQUEST, response.statusCode)
        assertEquals(APPLICATION_PROBLEM_JSON, response.headers.contentType)

        val json = objectMapper.readTree(response.body)
        assertEquals(
            "One or more lines failed to parse",
            json.get("detail").asText(),
        )
        val errors = json.get("errors")
        assertEquals(3, errors.size())
        val first = errors.first()
        assertEquals(1, first.get("line").asInt())
        assertEquals("expected 7 fields, got 6", first.get("message").asText())
    }

    @Test
    fun `can process invalid file`() {
        val response = postProcessFile("/InvalidFile.txt")

        assertEquals(BAD_REQUEST, response.statusCode)
        assertEquals(APPLICATION_PROBLEM_JSON, response.headers.contentType)

        val json = objectMapper.readTree(response.body)
        assertEquals(
            "One or more fields are invalid",
            json.get("detail").asText(),
        )
        val violations = json.get("violations")
        assertEquals(4, violations.size())
        val first = violations.first()
        assertEquals(1, first.get("line").asInt())
        assertEquals("likes", first.get("field").asText())
        assertEquals("likes must start with 'Likes '", first.get("message").asText())
    }

    private fun postProcessFile(fileName: String): ResponseEntity<ByteArray> {
        val text = this::class.java.getResource(fileName)!!.readText(Charsets.UTF_8)

        val headers = HttpHeaders().apply { contentType = TEXT_PLAIN }
        val entity = HttpEntity(text, headers)

        return rest.postForEntity("http://localhost:$port/files/process", entity, ByteArray::class.java)
    }
}
