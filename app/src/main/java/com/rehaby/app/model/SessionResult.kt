package com.rehaby.app.model

data class SessionResult(
    val exerciseId: String,
    val exerciseName: String,
    val totalReps: Int,
    val correctReps: Int,
    val formAccuracyPercent: Int,
    val durationSeconds: Long,
    val feedbackGiven: List<String>
)
