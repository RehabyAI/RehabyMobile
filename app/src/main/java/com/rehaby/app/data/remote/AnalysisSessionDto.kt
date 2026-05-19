package com.rehaby.app.data.remote

/** POST /analysis/frame */
data class FrameAnalysisRequestDto(
    val image_base64: String
)

data class FrameAnalysisResponseDto(
    val score: Int,
    val status: String,
    val feedback: String
)

/** POST /sessions/ */
data class CreateSessionRequestDto(
    val patient_id: String,
    val exercise_name: String,
    val rep_count: Int,
    val average_score: Int,
    val errors_detected: List<String>,
    val feedback_summary: String
)

data class CreateSessionResponseDto(
    val patient_id: String,
    val exercise_name: String,
    val rep_count: Int,
    val average_score: Int,
    val errors_detected: List<String>,
    val feedback_summary: String,
    val session_id: String,
    val start_time: String,
    val end_time: String
)
