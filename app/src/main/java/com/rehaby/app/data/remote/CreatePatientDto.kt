package com.rehaby.app.data.remote

/**
 * Request body for POST /patients/
 * ([API](https://ai-posture-coach-real-time-exercise-form.onrender.com/patients/))
 */
data class CreatePatientRequestDto(
    val full_name: String,
    val age: Int,
    val gender: String,
    val condition: String,
    val rehab_plan: String
)

data class CreatePatientResponseDto(
    val full_name: String,
    val age: Int,
    val gender: String,
    val condition: String,
    val rehab_plan: String,
    val patient_id: String,
    val created_at: String
)
