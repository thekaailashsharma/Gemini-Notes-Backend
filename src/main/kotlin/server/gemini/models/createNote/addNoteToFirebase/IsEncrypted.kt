package server.gemini.models.createNote.addNoteToFirebase


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IsEncrypted(
    @SerialName("booleanValue")
    val booleanValue: Boolean? = null
)