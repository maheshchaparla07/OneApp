package com.example.multiscreenapp


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.multiscreenapp.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001
    private var isAuthComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up click listeners
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (validateInput(email, password)) {
                signUpUser(email, password)
            }
        }

        binding.forgotPasswordText.setOnClickListener {
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return false
        }
        binding.emailLayout.error = null

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            return false
        }
        binding.passwordLayout.error = null

        return true
    }

    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    showError("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun signUpUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                    LoginActivity()
                } else {
                    showError("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithGoogle() {
        isAuthComplete = false
        binding.progressBar.visibility = View.VISIBLE
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            showError("Sign-in failed: ${e.message}")
        }

        // Add timeout watchdog
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAuthComplete) showError("Sign-in timeout")
        }, 15000) // 15-second timeout
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                binding.progressBar.visibility = View.GONE
                showError("Google sign-in failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                isAuthComplete = true
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    showError("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToHome() {
        // Make sure the intent uses the correct activity class
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()  // Optional: close login activity
    }

    // set up validation
    private fun setupInputValidations() {
        // Email validation
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail(binding.emailEditText.text.toString())
        }

        // Password validation
        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Invalid email format"
                false
            }
            else -> {
                binding.emailLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                binding.passwordLayout.error = "Password is required"
                false
            }
            password.length < 8 -> {
                binding.passwordLayout.error = "Minimum 8 characters required"
                false
            }
            else -> {
                binding.passwordLayout.error = null
                binding.passwordLayout.helperText = "Strong password"
                true
            }
        }
    }


    private fun showError(message: String?) {
        Toast.makeText(
            this,
            message ?: "An unknown error occurred",
            Toast.LENGTH_LONG
        ).show()
    }
}