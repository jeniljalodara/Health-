package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getCoachResponse(history: List<Content>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Hi there! It looks like the Gemini API Key is not configured yet. Plase configure it in the Secrets panel in AI Studio so I can assist you with custom advice!"
        }

        val request = GenerateContentRequest(
            contents = history,
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are Dr. Heighten, a friendly, professional AI Pediatric Growth and Wellness Coach. " +
                                "Your purpose is to help the user grow taller, healthier, and develop strong habits. " +
                                "Provide highly scientific yet practical guidance on vertical bone growth, growth plates, " +
                                "spinal decompression, postural stretching, active sleep, HGH (human growth hormone) release optimization, " +
                                "and proper micronutrients (Calcium, Protein, Vitamin D3, Zinc). " +
                                "Be encouraging, positive, and clear. Remind them that health, posture, and consistent habits " +
                                "have a massive impact on physical development. Keep your answers reasonably concise and friendly."
                    )
                )
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, I didn't receive a valid tip. Please try asking again!"
        } catch (e: Exception) {
            "Coach response error: ${e.localizedMessage}. Please check your connection and try again."
        }
    }
}
