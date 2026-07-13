package com.piyushkuca.androidvibe

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- NETWORK MODELS ---

data class ChatRequest(
    val model: String = "meta/llama-3.1-405b-instruct",
    val messages: List<Message>,
    val max_tokens: Int = 1024,
    val temperature: Double = 0.2
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message
)

// --- API INTERFACE ---

interface NvidiaApiService {
    @POST("v1/chat/completions")
    suspend fun generateCode(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

// --- RETROFIT SINGLETON ---

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("https://integrate.api.nvidia.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val nvidiaApi: NvidiaApiService = retrofit.create(NvidiaApiService::class.java)
