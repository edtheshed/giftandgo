package com.edleshed.giftandgo.files

import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/files")
class FileProcessorController(
    private val fileProcessorService: FileProcessorService,
    private val exportService: ExportService
) {

    @PostMapping(
        path = ["/process"],
        consumes = [TEXT_PLAIN_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun processFile(@RequestBody body: String): ResponseEntity<ByteArray> {
        val people = fileProcessorService.process(body)
        val bytes = exportService.toPeopleJsonFile(people)

        return ResponseEntity.ok()
            .contentType(APPLICATION_JSON)
            .header(CONTENT_DISPOSITION, """attachment; filename="OutcomeFile.json"""")
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }
}
