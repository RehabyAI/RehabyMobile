package com.rehaby.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rehaby.app.data.remote.ApiModule
import com.rehaby.app.data.remote.CreatePatientRequestDto
import com.rehaby.app.databinding.ActivitySignupBinding
import com.rehaby.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val genders = listOf(
            getString(R.string.gender_male),
            getString(R.string.gender_female),
            getString(R.string.gender_other)
        )
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        binding.genderInput.setAdapter(genderAdapter)
        binding.genderInput.setText(genders[0], false)

        binding.signupBack.setOnClickListener { finish() }

        binding.submitSignupButton.setOnClickListener { submitSignup() }

        binding.goToLogin.setOnClickListener { finish() }
    }

    private fun submitSignup() {
        val fullName = binding.nameInput.text?.toString().orEmpty().trim()
        val ageStr = binding.ageInput.text?.toString().orEmpty().trim()
        val gender = binding.genderInput.text?.toString().orEmpty().trim()
        val condition = binding.conditionInput.text?.toString().orEmpty().trim()
        val rehabPlan = binding.rehabPlanInput.text?.toString().orEmpty().trim()
        val age = ageStr.toIntOrNull()

        if (fullName.isEmpty() || gender.isEmpty() || condition.isEmpty() || rehabPlan.isEmpty() ||
            age == null || age <= 0
        ) {
            Toast.makeText(this, R.string.fill_signup_fields, Toast.LENGTH_SHORT).show()
            return
        }

        binding.submitSignupButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiModule.patientApi.createPatient(
                        CreatePatientRequestDto(
                            full_name = fullName,
                            age = age,
                            gender = gender,
                            condition = condition,
                            rehab_plan = rehabPlan
                        )
                    )
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        SessionManager.savePatient(this@SignupActivity, body)
                        navigateToDashboard()
                    } else {
                        Toast.makeText(this@SignupActivity, R.string.signup_parse_error, Toast.LENGTH_LONG)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        getString(R.string.signup_failed, response.code()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: UnknownHostException) {
                Toast.makeText(this@SignupActivity, R.string.signup_network_error, Toast.LENGTH_LONG)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SignupActivity,
                    e.message ?: getString(R.string.signup_network_error),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.submitSignupButton.isEnabled = true
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
