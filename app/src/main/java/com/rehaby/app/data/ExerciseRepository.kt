package com.rehaby.app.data

import com.rehaby.app.R
import com.rehaby.app.model.Exercise

object ExerciseRepository {
    fun getAll(): List<Exercise> = listOf(
        Exercise(
            id = "knee_extension",
            name = "Knee Extension",
            description = "Strengthens quadriceps. Sit on a chair and extend one leg fully.",
            sets = 3,
            reps = 10,
            difficulty = "Easy",
            iconResId = R.drawable.ic_knee_extension,
            correctPoseImageResId = R.drawable.correct_knee_extension,
            instructions = listOf(
                "Sit on a chair with your back straight and feet flat on the floor",
                "Slowly extend your right leg until it is completely straight",
                "Hold for 2 seconds at full extension",
                "Lower your leg slowly back to the starting position",
                "Repeat for the other leg"
            )
        ),
        Exercise(
            id = "hip_abduction",
            name = "Hip Abduction",
            description = "Strengthens hip muscles. Stand and lift your leg out to the side.",
            sets = 2,
            reps = 12,
            difficulty = "Moderate",
            iconResId = R.drawable.ic_hip_abduction,
            correctPoseImageResId = R.drawable.correct_hip_abduction,
            instructions = listOf(
                "Stand upright holding a wall or chair for balance",
                "Keep your core engaged and back straight",
                "Slowly lift your right leg out to the side, keeping toes forward",
                "Raise to about 45 degrees — do not tilt your body",
                "Lower slowly and repeat"
            )
        ),
        Exercise(
            id = "wall_squat",
            name = "Wall Squat Hold",
            description = "Builds knee and thigh strength. Hold a squat position against a wall.",
            sets = 3,
            reps = 1,
            holdSeconds = 30,
            difficulty = "Moderate",
            iconResId = R.drawable.ic_wall_squat,
            correctPoseImageResId = R.drawable.correct_wall_squat,
            instructions = listOf(
                "Stand with your back flat against a wall",
                "Walk your feet forward about 2 feet from the wall",
                "Slowly slide down until your knees are at 90 degrees",
                "Your thighs should be parallel to the floor",
                "Hold this position for 30 seconds, breathing steadily"
            )
        )
    )

    fun getById(id: String): Exercise? = getAll().find { it.id == id }
}
