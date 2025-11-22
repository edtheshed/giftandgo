package com.edleshed.giftandgo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FileProcessorApplication

fun main(args: Array<String>) {
    runApplication<FileProcessorApplication>(*args)
}
