package com.rehaby.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rehaby.app.databinding.ActivitySessionSummaryBinding

class SessionSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionSummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty()
        val accuracy = intent.getIntExtra(EXTRA_FORM_ACCURACY, 0)
        val reps = intent.getIntExtra(EXTRA_REPS_COMPLETED, 0)
        val duration = intent.getLongExtra(EXTRA_DURATION_SEC, 0L)

        binding.titleExercise.text = name
        binding.accuracyPercent.text = getString(R.string.percent_value, accuracy)
        binding.ringProgress.setProgress(accuracy)
        binding.metricReps.text = reps.toString()
        binding.metricDuration.text = formatDuration(duration)

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    companion object {
        const val EXTRA_EXERCISE_NAME = "exerciseName"
        const val EXTRA_FORM_ACCURACY = "formAccuracy"
        const val EXTRA_REPS_COMPLETED = "repsCompleted"
        const val EXTRA_DURATION_SEC = "durationSeconds"
    }
}
