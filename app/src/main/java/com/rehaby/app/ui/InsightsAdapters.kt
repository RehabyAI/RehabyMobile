package com.rehaby.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rehaby.app.R
import com.rehaby.app.data.remote.PatientErrorStatDto
import com.rehaby.app.data.remote.PatientHistoryEntryDto
import com.rehaby.app.databinding.ItemHistorySessionBinding
import com.rehaby.app.databinding.ItemPatientErrorBinding
import kotlin.math.roundToInt

class HistorySessionsAdapter(
    private var items: List<PatientHistoryEntryDto> = emptyList()
) : RecyclerView.Adapter<HistorySessionsAdapter.VH>() {

    fun submit(list: List<PatientHistoryEntryDto>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistorySessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(private val binding: ItemHistorySessionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(e: PatientHistoryEntryDto) {
            val ctx = binding.root.context
            binding.exerciseName.text = e.exercise_name?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.history_unknown_exercise)

            val parts = mutableListOf<String>()
            (e.start_time ?: e.end_time)?.let { parts.add(shortDateOrRaw(it)) }
            e.rep_count?.let { parts.add(ctx.getString(R.string.history_reps_fmt, it)) }
            e.average_score?.let { parts.add(ctx.getString(R.string.history_score_fmt, it.roundToInt())) }
            binding.metaLine.text = parts.joinToString(" · ").ifBlank { ctx.getString(R.string.history_no_meta) }

            val summary = e.feedback_summary?.trim().orEmpty()
            if (summary.isNotEmpty()) {
                binding.feedbackSummary.visibility = View.VISIBLE
                binding.feedbackSummary.text = summary
            } else {
                binding.feedbackSummary.visibility = View.GONE
            }
        }

        private fun shortDateOrRaw(iso: String): String {
            if (iso.length >= 10) return iso.substring(0, 10)
            return iso
        }
    }
}

class PatientErrorsAdapter(
    private var items: List<PatientErrorStatDto> = emptyList()
) : RecyclerView.Adapter<PatientErrorsAdapter.VH>() {

    fun submit(list: List<PatientErrorStatDto>) {
        items = list.sortedByDescending { it.occurrence_count }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPatientErrorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(private val binding: ItemPatientErrorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(e: PatientErrorStatDto) {
            binding.errorName.text = e.error_name
            binding.countBadge.text = e.occurrence_count.toString()
        }
    }
}
