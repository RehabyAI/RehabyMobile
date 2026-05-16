package com.rehaby.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rehaby.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text?.toString().orEmpty().trim()
            val password = binding.passwordInput.text?.toString().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_email_password, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            navigateToDashboard()
        }

        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.forgotPassword.setOnClickListener {
            Toast.makeText(this, R.string.forgot_password_hint, Toast.LENGTH_SHORT).show()
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
