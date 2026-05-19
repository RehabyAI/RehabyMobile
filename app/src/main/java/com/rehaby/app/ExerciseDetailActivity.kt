package com.rehaby.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.rehaby.app.data.ExerciseRepository
import com.rehaby.app.databinding.ActivityExerciseDetailBinding
import com.rehaby.app.util.YoutubeThumbnails
import com.rehaby.app.util.openExternalUrl

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_EXERCISE_ID) ?: run {
            finish()
            return
        }
        val exercise = ExerciseRepository.getById(id) ?: run {
            finish()
            return
        }

        binding.toolbarTitle.text = exercise.name
        binding.toolbarSubtitle.text = getString(R.string.detail_subtitle_format, exercise.difficulty)

        YoutubeThumbnails.hqDefaultFromWatchUrl(exercise.demoVideoUrl)?.let { thumbUrl ->
            binding.heroImage.load(thumbUrl) {
                crossfade(true)
                placeholder(exercise.iconResId)
                error(exercise.iconResId)
            }
        } ?: binding.heroImage.setImageResource(exercise.iconResId)

        binding.heroImage.setOnClickListener {
            openExternalUrl(exercise.demoVideoUrl)
        }

        val setsReps = if (exercise.holdSeconds > 0) {
            "${exercise.sets} sets · ${exercise.holdSeconds} sec hold"
        } else {
            "${exercise.sets} sets · ${exercise.reps} reps"
        }
        binding.tagSetsReps.text = setsReps
        binding.tagDifficulty.text = exercise.difficulty
        binding.tagPose.text = getString(R.string.tag_pose_on)

        binding.instructionsContainer.removeAllViews()
        exercise.instructions.forEachIndexed { index, step ->
            val row = LayoutInflater.from(this).inflate(
                R.layout.item_instruction_step,
                binding.instructionsContainer,
                false
            ) as LinearLayout
            row.findViewById<TextView>(R.id.stepNumber).text = (index + 1).toString()
            row.findViewById<TextView>(R.id.stepText).text = step
            binding.instructionsContainer.addView(row)
        }

        binding.watchDemoButton.setOnClickListener {
            openExternalUrl(exercise.demoVideoUrl)
        }
        binding.viewReferenceButton.setOnClickListener {
            openExternalUrl(exercise.referenceImageUrl)
        }
        binding.startExerciseButton.setOnClickListener {
            startActivity(
                Intent(this, PoseDetectionActivity::class.java).apply {
                    putExtra(PoseDetectionActivity.EXTRA_EXERCISE_ID, exercise.id)
                }
            )
        }

        binding.backButton.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_EXERCISE_ID = "exerciseId"
    }
}
