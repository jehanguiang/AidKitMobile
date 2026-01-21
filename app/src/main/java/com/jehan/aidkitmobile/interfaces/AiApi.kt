package com.jehan.aidkitmobile.interfaces

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {

    @POST("api/ai/ask/medication")
    suspend fun askAboutMedication(@Body request: AskRequest): Response<String>
}

data class AskRequest(val question: String)
