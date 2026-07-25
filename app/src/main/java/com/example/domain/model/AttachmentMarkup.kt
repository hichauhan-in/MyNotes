package com.example.domain.model

/**
 * Inline image markup for note content. Images live in the note body as a token on
 * their own line — `![img](attachment://FILE_NAME)` — so text and images can be
 * interleaved in any order. Only the file name is stored; the bytes live in the
 * app-private attachments directory.
 */
object AttachmentMarkup {

    private val TOKEN = Regex("""!\[[^]]*]\(attachment://([^)\s]+)\)""")

    /** The inline token that represents [fileName] in a note body. */
    fun token(fileName: String): String = "![img](attachment://$fileName)"

    /** File names of every inline image, in the order they appear. */
    fun fileNames(content: String): List<String> =
        TOKEN.findAll(content).map { it.groupValues[1] }.toList()

    /** If [line] is exactly an image token, returns its file name; otherwise null. */
    fun imageFileName(line: String): String? =
        TOKEN.matchEntire(line.trim())?.groupValues?.get(1)

    /** Content with image tokens removed — used for previews, word counts, etc. */
    fun stripTokens(content: String): String = content.replace(TOKEN, " ")
}
