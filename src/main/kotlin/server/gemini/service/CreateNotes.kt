package server.gemini.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import server.gemini.models.askGemini.AskGeminiRequest
import server.gemini.models.askGemini.AskGeminiResponse
import server.gemini.models.askGemini.Prompt
import server.gemini.models.createNote.CreateNoteRequest
import server.gemini.models.createVectors.Content
import server.gemini.models.createVectors.CreateVectorsRequest
import server.gemini.models.createVectors.CreateVectorsResponse
import server.gemini.models.createVectors.Part
import server.gemini.models.queryVectors.QueryVectorsRequest
import server.gemini.models.queryVectors.QueryVectorsResponse
import server.gemini.models.searchNotes.SearchNotesResponse
import server.gemini.models.searchNotes.SearchRequest
import server.gemini.models.signUpFirebase.SignUpFirebaseError
import server.gemini.models.upsert.Metadata
import server.gemini.models.upsert.UpsertVectorsRequest
import server.gemini.models.upsert.UpsertVectorsResponse
import server.gemini.models.upsert.Vector
import server.gemini.utils.Paragraph
import server.gemini.utils.splitIntoParagraphs
import java.util.*

class CreateNotes(private val client: HttpClient) {
    fun createParagraphs(content: String): List<Paragraph> {
        return content.splitIntoParagraphs()
    }

    suspend fun createVectors(paragraphs: List<Paragraph>): List<Vector?> {
        val vectorsList: MutableList<Vector?> = mutableListOf()
        paragraphs.forEach { paragraph ->
            println(paragraph.text)
            try {
                val createVectorUrl = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:" +
                        "embedContent?key=AIzaSyDUReaeZGCtZaLQlBdIL-x76Lwck7pTNXg"
                val c = client.post {
                    url(createVectorUrl)
                    setBody(
                        CreateVectorsRequest(
                            content = Content(
                                parts = listOf(
                                    Part(paragraph.text)
                                )
                            ),
                            model = "models/text-embedding-004"
                        )
                    )
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    headers {
                        append("Accept", "*/*")
                        append("Content-Type", "application/json")
                    }
                }
                println("Response is ${c}")
                if (c.status.isSuccess()) {
                    println("Went Insidessss")
                    val body = c.body<CreateVectorsResponse>()
                    val vectors = body.embedding?.values
                    vectorsList.add(
                        Vector(
                            id = UUID.randomUUID().toString(),
                            metadata = Metadata(
                                paragraph.text
                            ),
                            values = vectors,
                            namespace = paragraph.namespace
                        )
                    )
                } else {
                    println("Went Insides ${c.body<CreateVectorsResponse>()}")
                }
            } catch (e: Exception) {
                println("Error is ${e.message}")
            }
        }
        return vectorsList
    }

    suspend fun upsertVectors(upsertVectorsRequests: List<UpsertVectorsRequest>): UpsertVectorsResponse {
        var count = 0
        try {
            upsertVectorsRequests.forEach { upsertVectorsRequest ->
                println("Request is $upsertVectorsRequest")
                val addFireStoreUrl = "https://temporary-m7240u6.svc.aped-4627-b74a.pinecone.io/vectors/upsert"
                val c = client.post {
                    url(addFireStoreUrl)
                    setBody(
                        upsertVectorsRequest
                    )
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    headers {
                        append("Api-Key", "e88fe59a-4d45-4e71-a287-90a5088db545")
                        append("X-Pinecone-API-Version", "2024-07")
                        append("Accept", "*/*")
                        append("content-Type", "application/json")
                    }
                }
                println("UpsertResponse is ${c}")
                if (c.status.isSuccess()) {
                    println("UpsertResponse is inside")
                    val body = c.body<UpsertVectorsResponse>()
                    count += body.upsertedCount ?: 0
                } else {
                    println("UpsertResponse is ouside")
                    UpsertVectorsResponse(
                        upsertedCount = null
                    )
                }
            }
            return UpsertVectorsResponse(
                upsertedCount = count
            )
        } catch (e: Exception) {
            println("UpsertResponse is ${e.message}")
            return UpsertVectorsResponse(
                upsertedCount = null
            )
        }
    }

    suspend fun queryVectorsRequest(queryVectorsRequest: QueryVectorsRequest): QueryVectorsResponse {
        try {
            val queryVectorsUrl = "https://temporary-m7240u6.svc.aped-4627-b74a.pinecone.io/query"
            val c = client.post {
                url(queryVectorsUrl)
                println("QueryResponse vectors ${queryVectorsRequest.vector}")
                setBody(QueryVectorsRequest(
                    vector = queryVectorsRequest.vector,
                    topK = queryVectorsRequest.topK ?: 10,
                    includeMetadata = queryVectorsRequest.includeMetadata ?: true,
                    includeValues = queryVectorsRequest.includeValues ?: true,
                    namespace = queryVectorsRequest.namespace
                ))
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                headers {
                    append("Api-Key", "e88fe59a-4d45-4e71-a287-90a5088db545")
                    append("X-Pinecone-API-Version", "2024-07")
                    append("Accept", "*/*")
                    append("content-Type", "application/json")
                }
            }
            println("QueryResponse is ${c}")
            if (c.status.isSuccess()) {
                println("QueryResponse is inside")
                return c.body<QueryVectorsResponse>()
            } else {
                println("QueryResponse is outside")
                return QueryVectorsResponse(
                    results = null
                )
            }
        } catch (e: Exception) {
            println("QueryResponse is ${e.message}")
            return QueryVectorsResponse(
                results = null
            )
        }
    }

    suspend fun askGemini(question: String, content: String): SearchNotesResponse {
        try {
            val askGeminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyDUReaeZGCtZaLQlBdIL-x76Lwck7pTNXg"

            val createPrompt = "This is the question on a users note that they want to ask" +
                    "The question is: $question. The content of the note is: $content. Now answer the question." +
                    "nicely and in a way that is helpful to the user."

            val c = client.post {
                url(askGeminiUrl)
                setBody(
                    AskGeminiRequest(
                        contents = listOf(
                            server.gemini.models.askGemini.Content(
                                parts = listOf(
                                    server.gemini.models.askGemini.Part(createPrompt)
                                )
                            )
                        )
                    )
                )
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                headers {
                    append("Accept", "*/*")
                    append("Content-Type", "application/json")
                }
            }
            println("AskGeminiResponse111 is ${content} ")
            if (c.status.isSuccess()) {
                println("AskGeminiResponse is inside")
                val response = c.body<AskGeminiResponse>().candidates?.get(0)?.content?.parts?.get(0)?.text
                return SearchNotesResponse(
                    response = response,
                    content = content,
                    name = question
                )
            } else {
                println("AskGeminiResponse is ouside")
                return SearchNotesResponse(
                    response = c.bodyAsText()
                )
            }
        } catch (e: Exception) {
            println("Error is ${e.message}")
            return SearchNotesResponse(
                response = e.message
            )
        }
    }

}