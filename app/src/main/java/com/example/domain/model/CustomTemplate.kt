package com.example.domain.model

import java.util.UUID

/**
 * A user-defined draft that appears on the templates button in the bottom-left corner.
 * Selecting it opens a new note pre-filled with [content]. [iconKey] maps to an icon in the UI layer.
 */
data class CustomTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String,
    val content: String,
)
