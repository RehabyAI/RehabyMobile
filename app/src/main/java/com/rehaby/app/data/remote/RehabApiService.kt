package com.rehaby.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RehabApiService {

    @POST("analysis/frame")
    suspend fun analyzeFrame(@Body body: FrameAnalysisRequestDto): Response<FrameAnalysisResponseDto>

    @POST("sessions/")
    suspend fun createSession(@Body body: CreateSessionRequestDto): Response<CreateSessionResponseDto>

    @GET("patients/{patientId}/trends")
    suspend fun getPatientTrends(@Path("patientId") patientId: String): Response<List<TrendPointDto>>

    @GET("patients/{patientId}/history")
    suspend fun getPatientHistory(@Path("patientId") patientId: String): Response<List<PatientHistoryEntryDto>>

    @GET("patients/{patientId}/errors")
    suspend fun getPatientErrors(@Path("patientId") patientId: String): Response<List<PatientErrorStatDto>>
}
