package edu.dixietech.example.websockets.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Todo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val isCompleted: Boolean = false
)