package com.edleshed.giftandgo.files

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import jakarta.validation.constraints.Pattern
import java.util.UUID

data class Transport(
    @field:NotBlank val description: String,
    @field:PositiveOrZero val averageSpeed: Double,
    @field:PositiveOrZero val topSpeed: Double,
)

// add separate classes for parsing and validation and response
data class Person(
    val uniqueId: UUID,
    @field:NotBlank val id: String,
    @field:NotBlank val name: String,
    @field:Pattern(regexp = "^Likes\\s.+$", message = "likes must start with 'Likes '")
    val likes: String,
    @field:Valid val transport: Transport,
)

data class PersonResponse(
    val name: String,
    val transport: String,
    val topSpeed: Double,
)
