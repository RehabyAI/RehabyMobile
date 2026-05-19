package com.rehaby.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rehaby.app.R
import com.rehaby.app.data.remote.ApiModule
import com.rehaby.app.databinding.FragmentTrendsBinding
import com.rehaby.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrendsFragment : Fragment() {

    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadTrends()
    }

    private fun loadTrends() {
        val patientId = SessionManager.getPatientId(requireContext())
        if (patientId.isNullOrBlank()) {
            binding.progress.visibility = View.GONE
            binding.chart.visibility = View.GONE
            binding.statusText.visibility = View.VISIBLE
            binding.statusText.text = getString(R.string.insights_need_patient)
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.statusText.visibility = View.GONE
        binding.chart.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val resp = ApiModule.rehabApi.getPatientTrends(patientId)
                    if (resp.isSuccessful) resp.body().orEmpty() else null
                } catch (_: Exception) {
                    null
                }
            }

            val b = _binding ?: return@launch
            b.progress.visibility = View.GONE

            if (result == null) {
                b.chart.visibility = View.GONE
                b.statusText.visibility = View.VISIBLE
                b.statusText.text = getString(R.string.insights_load_failed)
                return@launch
            }

            val pairs = result.map { it.date to it.average_score }
            if (pairs.isEmpty()) {
                b.chart.visibility = View.GONE
                b.statusText.visibility = View.VISIBLE
                b.statusText.text = getString(R.string.trends_empty)
            } else {
                b.statusText.visibility = View.GONE
                b.chart.visibility = View.VISIBLE
                b.chart.setTrendPoints(pairs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
