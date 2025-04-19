import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import uk.ac.tees.mad.s3445191.databinding.DialogSignupBinding
import java.util.Calendar
import java.util.regex.Pattern

class SignUpDialog(
    private val context: Context,
    private val onSignUpComplete: (email: String, password: String, firstName: String, lastName: String, dob: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogSignupBinding
    private val namePattern = Pattern.compile("^[a-zA-Z]+$")
    private val datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupViews()
        setupDatePicker()
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
                { _, year, month, day ->
                    val monthFormatted = (month + 1).toString().padStart(2, '0')
                    val dayFormatted = day.toString().padStart(2, '0')
                    val selectedDate = "${year}-${monthFormatted}-${dayFormatted}"
                    binding.setDateOfBirth.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                // Set maximum date to current date
                datePicker.maxDate = calendar.timeInMillis

                // Set minimum date to 10 years before current date
                calendar.add(Calendar.YEAR, -10)
                datePicker.minDate = calendar.timeInMillis
            }.show()
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

        // First name validation
        if (firstName.isEmpty()) {
            binding.setFirstName.error = "First name required"
            isValid = false
        } else if (!namePattern.matcher(firstName).matches()) {
            binding.setFirstName.error = "Only alphabets allowed"
            isValid = false
        } else {
            binding.setFirstName.error = null
        }

        // Last name validation
        if (lastName.isEmpty()) {
            binding.setLastName.error = "Last name required"
            isValid = false
        } else if (!namePattern.matcher(lastName).matches()) {
            binding.setLastName.error = "Only alphabets allowed"
            isValid = false
        } else {
            binding.setLastName.error = null
        }

        // Date of birth validation
        if (dob.isEmpty()) {
            binding.setDateOfBirth.error = "Date of birth required"
            isValid = false
        } else if (!datePattern.matcher(dob).matches()) {
            binding.setDateOfBirth.error = "Format should be YYYY-MM-DD"
            isValid = false
        } else {
            try {
                val parts = dob.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1  // Calendar months are 0-based
                val day = parts[2].toInt()

                val dobCalendar = Calendar.getInstance().apply {
                    set(year, month, day)
                }

                val minDobCalendar = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -10)
                }

                if (dobCalendar.after(minDobCalendar)) {
                    binding.setDateOfBirth.error = "Must be at least 10 years old"
                    isValid = false
                } else {
                    binding.setDateOfBirth.error = null
                }
            } catch (e: Exception) {
                binding.setDateOfBirth.error = "Invalid date"
                isValid = false
            }
        }

        // Email validation
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.setEmail.error = "Valid email required"
            isValid = false
        } else {
            binding.setEmail.error = null
        }

        // Password validation
        if (password.isEmpty() || password.length < 6) {
            binding.setPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.setPassword.error = null
        }

        return isValid
    }
}