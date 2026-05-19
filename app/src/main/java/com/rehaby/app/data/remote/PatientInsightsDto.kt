package com.rehaby.app.data.remote

/** GET /patients/{id}/trends */
data class TrendPointDto(
    val date: String,
    val average_score: Double
)

/** GET /patients/{id}/history — fields mirror session objects where possible */
data class PatientHistoryEntryDto(
    val session_id: String? = null,
    val patient_id: String? = null,
    val exercise_name: String? = null,
    val rep_count: Int? = null,
    val average_score: Double? = null,
    val errors_detected: List<String>? = null,
    val feedback_summary: String? = null,
    val start_time: String? = null,
    val end_time: String? = null
)

/** GET /patients/{id}/errors */
data class PatientErrorStatDto(
    val error_name: String,
    val occurrence_count: Int
)
