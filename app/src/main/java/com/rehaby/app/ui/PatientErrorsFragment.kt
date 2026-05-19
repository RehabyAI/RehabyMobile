package com.rehaby.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rehaby.app.R
import com.rehaby.app.data.remote.ApiModule
import com.rehaby.app.databinding.FragmentPatientErrorsBinding
import com.rehaby.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientErrorsFragment : Fragment() {

    private var _binding: FragmentPatientErrorsBinding? = null
    private val binding get() = _binding!!

    private val adapter = PatientErrorsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPatientErrorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadErrors()
    }

    private fun loadErrors() {
        val patientId = SessionManager.getPatientId(requireContext())
        if (patientId.isNullOrBlank()) {
            binding.progress.visibility = View.GONE
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = getString(R.string.insights_need_patient)
            adapter.submit(emptyList())
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                try {
                    val resp = ApiModule.rehabApi.getPatientErrors(patientId)
                    if (resp.isSuccessful) resp.body().orEmpty() else null
                } catch (_: Exception) {
                    null
                }
            }

            val b = _binding ?: return@launch
            b.progress.visibility = View.GONE

            if (list == null) {
                b.emptyText.visibility = View.VISIBLE
                b.emptyText.text = getString(R.string.insights_load_failed)
                adapter.submit(emptyList())
                return@launch
            }

            if (list.isEmpty()) {
                b.emptyText.visibility = View.VISIBLE
                b.emptyText.text = getString(R.string.errors_empty)
                adapter.submit(emptyList())
            } else {
                b.emptyText.visibility = View.GONE
                adapter.submit(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
