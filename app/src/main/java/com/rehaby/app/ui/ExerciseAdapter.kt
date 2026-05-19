package com.rehaby.app.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rehaby.app.ExerciseDetailActivity
import coil.load
import com.rehaby.app.databinding.ItemExerciseBinding
import com.rehaby.app.model.Exercise
import com.rehaby.app.util.YoutubeThumbnails

class ExerciseAdapter(
    private val items: List<Exercise>
) : RecyclerView.Adapter<ExerciseAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

        inner class VH(private val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: Exercise) {
            YoutubeThumbnails.hqDefaultFromWatchUrl(exercise.demoVideoUrl)?.let { thumb ->
                binding.exerciseIcon.load(thumb) {
                    placeholder(exercise.iconResId)
                    error(exercise.iconResId)
                }
            } ?: binding.exerciseIcon.setImageResource(exercise.iconResId)
            binding.exerciseName.text = exercise.name
            val setsReps = if (exercise.holdSeconds > 0) {
                "${exercise.sets} sets · ${exercise.holdSeconds} sec hold"
            } else {
                "${exercise.sets} sets · ${exercise.reps} reps"
            }
            binding.exerciseSetsReps.text = setsReps
            binding.difficultyBadge.text = when (exercise.difficulty) {
                "Moderate" -> "Mod"
                else -> exercise.difficulty
            }
            binding.root.setOnClickListener {
                val ctx = binding.root.context
                ctx.startActivity(
                    Intent(ctx, ExerciseDetailActivity::class.java).apply {
                        putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_ID, exercise.id)
                    }
                )
            }
        }
    }
}
