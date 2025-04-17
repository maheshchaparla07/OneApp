import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.example.multiscreenapp.databinding.DialogSignupBinding
import java.util.Calendar

class SignUpDialog(
    private val context: Context,
    private val onSignUpComplete: (email: String, password: String, firstName: String, lastName: String, dob: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()

    }

    private fun setupViews() {
        binding.btnSignUp.setOnClickListener {
            val firstName = binding.setFirstName.text.toString().trim()
            val lastName = binding.setLastName.text.toString().trim()
            val dob = binding.setDateOfBirth.text.toString().trim()
            val email = binding.setEmail.text.toString().trim()
            val password = binding.setPassword.text.toString().trim()

            if (validateInput(firstName, lastName, dob, email, password)) {
                onSignUpComplete(email, password, firstName, lastName, dob)
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupDatePicker() {
        binding.setDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, day ->  val selectedDate = "${year}-${month + 1}-${day}"
                    binding.setDateOfBirth.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        dob: String,
        email: String,
        password: String
    ): Boolean {
        var isValid = true

        if (firstName.isEmpty()) {
            binding.setFirstName.error = "First name required"
            isValid = false
        } else {
            binding.setFirstName.error = null
        }

        if (lastName.isEmpty()) {
            binding.setLastName.error = "Last name required"
            isValid = false
        } else {
            binding.setLastName.error = null
        }

        if (dob.isEmpty()) {
            binding.setDateOfBirth.error = "Date of birth required"
            isValid = false
        } else {
            binding.setDateOfBirth.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.setEmail.error = "Valid email required"
            isValid = false
        } else {
            binding.setEmail.error = null
        }

        if (password.isEmpty() || password.length < 6) {
            binding.setPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.setPassword.error = null
        }

        return isValid
    }
}