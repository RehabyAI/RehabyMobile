package com.rehaby.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rehaby.app.data.ExerciseRepository
import com.rehaby.app.R
import com.rehaby.app.databinding.FragmentHomeBinding
import com.rehaby.app.util.SessionManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.exerciseList.layoutManager = LinearLayoutManager(requireContext())
        binding.exerciseList.adapter = ExerciseAdapter(ExerciseRepository.getAll())

        val savedName = SessionManager.getFullName(requireContext())
        binding.greetingText.text = if (!savedName.isNullOrBlank()) {
            getString(R.string.greeting_named, savedName)
        } else {
            getString(R.string.greeting_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
