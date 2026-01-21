package com.jehan.aidkitmobile.interfaces

import com.jehan.aidkitmobile.models.Medication
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MedicationApi {

    @GET("api/medications")
    suspend fun getAll(): Response<List<Medication>>

    @POST("api/medications")
    suspend fun create(@Body medication: Medication): Response<Medication>

    @DELETE("api/medications/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
