package com.rehaby.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rehaby.app.data.ExerciseRepository
import com.rehaby.app.databinding.ActivityMainBinding
import com.rehaby.app.ui.ExerciseAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.exerciseList.layoutManager = LinearLayoutManager(this)
        binding.exerciseList.adapter = ExerciseAdapter(ExerciseRepository.getAll())
    }
}
