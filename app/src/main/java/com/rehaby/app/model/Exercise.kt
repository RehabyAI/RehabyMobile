package com.rehaby.app.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val sets: Int,
    val reps: Int,
    val holdSeconds: Int = 0,
    val difficulty: String,
    val iconResId: Int,
    val correctPoseImageResId: Int,
    /** YouTube watch URL for the exercise demo. */
    val demoVideoUrl: String,
    /** Reference still (e.g. Pinterest pin) — opened in browser; used as secondary image when possible. */
    val referenceImageUrl: String,
    val instructions: List<String>
)
