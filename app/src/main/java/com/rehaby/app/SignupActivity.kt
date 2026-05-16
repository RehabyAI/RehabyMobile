package com.rehaby.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rehaby.app.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupBack.setOnClickListener { finish() }

        binding.submitSignupButton.setOnClickListener {
            val name = binding.nameInput.text?.toString().orEmpty().trim()
            val email = binding.emailInput.text?.toString().orEmpty().trim()
            val password = binding.passwordInput.text?.toString().orEmpty()
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_signup_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            navigateToDashboard()
        }

        binding.goToLogin.setOnClickListener { finish() }
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
