package uk.ac.tees.mad.s3445191

import SignUpDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uk.ac.tees.mad.s3445191.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var isAuthComplete = false
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = Firebase.auth
        if (auth.currentUser != null) navigateToHome()



        setupGoogleSignIn()
        setupUIListeners()
        setupInputValidations()

    }

    //Configures Google Sign-In options and initializes client
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleGoogleSignInResult(it.data)
        }
    }


    private val credentialPreferences by lazy {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            this,
            "secure_user_data",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun saveUserPreferences(
        firstName: String,
        lastName: String,
        dob: String,
        email: String
    ) {
        credentialPreferences.edit().apply {
            putString("firstName", firstName)
            putString("lastName", lastName)
            putString("dob", dob)
            putString("email", email)
            apply()
        }
    }

    //Sets up click listeners for UI elements
    private fun setupUIListeners() {
        binding.apply {
            btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
            loginButton.setOnClickListener { handleEmailLogin() }
            signUpButton.setOnClickListener { showSignUpDialog() }
            forgotPasswordText.setOnClickListener {
                Toast.makeText(this@LoginActivity, "Forgot password clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Google Sign-In flow with timeout handling
    private fun signInWithGoogle() {
        isAuthComplete = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)

                //timeout for authentication process
                withContext(Dispatchers.IO) {
                    delay(45000)
                    if (!isAuthComplete) {
                        withContext(Dispatchers.Main) {
                            showError("Sign-in timeout")
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Sign-in failed: ${e.message}")
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    //Handles email & password login validation
    private fun handleEmailLogin() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        if (validateInput(email, password)) loginUser(email, password)
    }

    //Authenticate user with Firebase using email/password
    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = Firebase.firestore.collection("users").document(userId).get().await()

                saveUserPreferences(
                    userDoc.getString("firstName") ?: "",
                    userDoc.getString("lastName") ?: "",
                    userDoc.getString("dob") ?: "",
                    userDoc.getString("email") ?: email
                )

                withContext(Dispatchers.Main) {
                    navigateToHome()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Authentication failed: ${e.message}")
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Signup user
    private fun showSignUpDialog() {
        val dialog = SignUpDialog(this) { email, password, firstName, lastName, dob ->
            signUpUserWithDetails(email, password, firstName, lastName, dob)
        }
        dialog.show()
    }



    //popup signup screen and store the data in firebase authentication thru auth
    private fun signUpUserWithDetails(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        dob: String
    ) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                saveUserProfile(firstName, lastName, dob, email)
                saveUserPreferences(firstName, lastName, dob, email) // Add this line
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Signup Successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Registration failed: ${e.message}")
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun saveUserProfile(
        firstName: String,
        lastName: String,
        dob: String,
        email: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                Firebase.firestore.collection("users").document(userId).set(
                    hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "dob" to dob,
                        "email" to email
                    )
                ).await()
                withContext(Dispatchers.Main) {
                    navigateToHome()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Profile save failed", e)
                withContext(Dispatchers.Main) {
                    showError("Profile save failed: ${e.message}")
                }
            }
        }
    }


    // Region Helper Methods
    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: Exception) {
            showError("Google sign-in failed: ${e.message}")
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                val user = auth.currentUser

                user?.let {
                    val userId = it.uid
                    val userDoc = Firebase.firestore.collection("users").document(userId).get().await()

                    if (userDoc.exists()) {
                        saveUserPreferences(
                            userDoc.getString("firstName") ?: "",
                            userDoc.getString("lastName") ?: "",
                            userDoc.getString("dob") ?: "",
                            userDoc.getString("email") ?: it.email ?: ""
                        )
                    } else {
                        val names = it.displayName?.split(" ") ?: listOf("", "")
                        val firstName = names.firstOrNull() ?: ""
                        val lastName = names.drop(1).joinToString(" ") ?: ""
                        val email = it.email ?: ""

                        Firebase.firestore.collection("users").document(userId).set(
                            hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "dob" to "",
                                "email" to email
                            )
                        ).await()

                        saveUserPreferences(firstName, lastName, "", email)
                    }
                }

                withContext(Dispatchers.Main) {
                    navigateToHome()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Authentication failed: ${e.message}")
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    //Navigate to Home Screen
    private fun navigateToHome() {
        Intent(this, HomeActivity::class.java).apply {
            startActivity(this)
            finish()
        }
    }


    //  Input Validation
    private fun setupInputValidations() {
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail(binding.emailEditText.text.toString())
        }

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    private fun validateInput(email: String, password: String): Boolean {
        return validateEmail(email) && validatePassword(password)
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


    companion object {
        private const val TAG = "LoginActivity"
    }

    private fun showError(message: String?) {
        Toast.makeText(this, message ?: "An unknown error occurred", Toast.LENGTH_LONG).show()
    }
}