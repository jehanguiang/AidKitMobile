package com.jehan.aidkitmobile.network

import com.jehan.aidkitmobile.interfaces.AiApi
import com.jehan.aidkitmobile.interfaces.MedicationApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val medicationApi: MedicationApi by lazy {
        retrofit.create(MedicationApi::class.java)
    }

    val aiApi: AiApi by lazy {
        retrofit.create(AiApi::class.java)
    }
}
