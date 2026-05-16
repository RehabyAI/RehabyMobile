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
    val instructions: List<String>
)
