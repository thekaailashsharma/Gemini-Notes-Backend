package server.gemini.models.addFirestore


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddFireStoreRequest(
    @SerialName("fields")
    val fields: FieldsX?
)