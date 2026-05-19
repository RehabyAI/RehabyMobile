package com.rehaby.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * POST [patients/](https://ai-posture-coach-real-time-exercise-form.onrender.com/patients/)
 */
interface PatientApiService {

    @POST("patients/")
    suspend fun createPatient(@Body body: CreatePatientRequestDto): Response<CreatePatientResponseDto>
}
