package server.gemini.models.createNote.addNoteToFirebase


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StartDate(
    @SerialName("stringValue")
    val stringValue: String? = null
)