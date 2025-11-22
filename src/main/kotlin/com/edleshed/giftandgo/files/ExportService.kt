package com.edleshed.giftandgo.files

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

interface ExportService {
    fun toPeopleJsonFile(people: List<Person>): ByteArray
}

@Service
class DefaultExportService(private val mapper: ObjectMapper) : ExportService {
    override fun toPeopleJsonFile(people: List<Person>): ByteArray {
        val export = people.map { PersonResponse(it.name, it.transport.description, it.transport.topSpeed) }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export)
    }
}
