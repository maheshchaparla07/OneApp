package uk.ac.tees.mad.s3445191


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import uk.ac.tees.mad.s3445191.databinding.ActivitySplashBinding


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Theme_MultiScreenApp)
        super.onCreate(savedInstanceState)

        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //set 2 seconds time delay to stay in the splash screen and redirect to login screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java)) // Changed to LoginActivity
            finish()
        }, 2000)
    }
}