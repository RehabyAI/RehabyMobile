package com.rehaby.app.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PoseValidator(
    private val exerciseId: String,
    private val holdSeconds: Int = 0
) {
    private var repCount = 0
    private var isInStartPosition = true
    private var correctFrameCount = 0
    private var totalFrameCount = 0
    private var accumulatedCorrectHoldMs = 0L
    private var lastTimestampNs: Long = 0L

    fun validate(
        landmarks: List<NormalizedLandmark>,
        frameTimestampNs: Long
    ): ValidationOutput {
        totalFrameCount++
        val deltaMs = if (lastTimestampNs == 0L) 0L
        else (frameTimestampNs - lastTimestampNs) / 1_000_000
        lastTimestampNs = frameTimestampNs

        val result = ExerciseRules.check(exerciseId, landmarks)

        if (result.isCorrect) {
            correctFrameCount++
            if (holdSeconds > 0) {
                accumulatedCorrectHoldMs += deltaMs.coerceIn(0L, 250L)
            } else if (!isInStartPosition) {
                repCount++
                isInStartPosition = true
            }
        } else {
            isInStartPosition = false
            if (holdSeconds > 0) {
                accumulatedCorrectHoldMs = 0L
            }
        }

        val accuracy = if (totalFrameCount > 0) {
            (correctFrameCount * 100) / totalFrameCount
        } else {
            0
        }

        val holdTargetMs = holdSeconds * 1000L

        return ValidationOutput(
            isCorrect = result.isCorrect,
            feedbackMessage = result.feedbackMessage,
            repCount = repCount,
            formAccuracy = accuracy,
            accumulatedCorrectHoldMs = accumulatedCorrectHoldMs,
            holdTargetMs = holdTargetMs
        )
    }

    fun getRepCount(): Int = repCount
    fun getFormAccuracy(): Int = if (totalFrameCount > 0) {
        (correctFrameCount * 100) / totalFrameCount
    } else {
        0
    }
}

data class ValidationOutput(
    val isCorrect: Boolean,
    val feedbackMessage: String,
    val repCount: Int,
    val formAccuracy: Int,
    val accumulatedCorrectHoldMs: Long,
    val holdTargetMs: Long
)
