package com.rehaby.app.pose

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs

data class PoseCheckResult(
    val isCorrect: Boolean,
    val feedbackMessage: String
)

object ExerciseRules {

    fun check(exerciseId: String, landmarks: List<NormalizedLandmark>): PoseCheckResult {
        if (landmarks.size < 33) return PoseCheckResult(true, "")
        return when (exerciseId) {
            "knee_extension" -> checkKneeExtension(landmarks)
            "hip_abduction" -> checkHipAbduction(landmarks)
            "wall_squat" -> checkWallSquat(landmarks)
            else -> PoseCheckResult(true, "")
        }
    }

    private fun checkKneeExtension(lm: List<NormalizedLandmark>): PoseCheckResult {
        val kneeAngle = AngleCalculator.angleBetween(lm[24], lm[26], lm[28])
        return when {
            kneeAngle < 150f -> PoseCheckResult(false, "Straighten your knee more")
            else -> PoseCheckResult(true, "")
        }
    }

    private fun checkHipAbduction(lm: List<NormalizedLandmark>): PoseCheckResult {
        val shoulderDiff = abs(lm[11].y() - lm[12].y())
        val hipDiff = abs(lm[23].y() - lm[24].y())
        return when {
            shoulderDiff > 0.08f ->
                PoseCheckResult(false, "Keep your upper body straight, do not lean")
            hipDiff < 0.03f ->
                PoseCheckResult(false, "Lift your leg higher out to the side")
            else -> PoseCheckResult(true, "")
        }
    }

    private fun checkWallSquat(lm: List<NormalizedLandmark>): PoseCheckResult {
        val kneeAngle = AngleCalculator.angleBetween(lm[24], lm[26], lm[28])
        val backLean = abs(lm[12].x() - lm[24].x())
        return when {
            kneeAngle > 120f ->
                PoseCheckResult(false, "Bend your knees more, lower your hips")
            kneeAngle < 70f ->
                PoseCheckResult(false, "You are too low, rise up slightly")
            backLean > 0.12f ->
                PoseCheckResult(false, "Keep your back flat against the wall")
            else -> PoseCheckResult(true, "")
        }
    }
}
